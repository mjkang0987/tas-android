package kr.co.takeaseat.app.domain

import java.time.Instant
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.serialization.Serializable

// 웹 `ReservationDetail.tsx` 결제 모드(handlePaymentSave·syncPointAward·paymentTargetFor) + `reservationDetailUtils.ts`
// (getPaymentEntryDrafts·PAYMENT_METHOD_OPTIONS) + 예약완료(handleConfirmSave completing) 이식.
// 쿠폰·회원권은 로그인 전용(서버 혜택)이라 게스트엔 없다.

/** 웹 PAYMENT_METHOD_OPTIONS — '회원권'은 회원권 칸에서만 생겨 직접 고르는 목록엔 없다. */
val PAYMENT_METHOD_OPTIONS = listOf("현금", "현금+현금영수증", "카드", "네이버페이", "네이버 예약금", "지역화폐", "지역화폐+현금영수증", "상품권", "적립금", "할인")

/** 매출(받은 돈)에 넣지 않는 결제수단 — 적립 자동 계산도 이걸 뺀다(웹 NON_REVENUE_PAYMENT_METHODS). */
private val NON_REVENUE_PAYMENT_METHODS = setOf("할인", "적립금", "회원권")

fun isRevenuePaymentMethod(method: String) = method !in NON_REVENUE_PAYMENT_METHODS

/** 결제 칸 한 줄(입력 중) — method "" = 아직 안 고름, amount 는 친 그대로(숫자만). */
@Serializable
data class PaymentDraft(val method: String = "", val amount: String = "", val key: Long = Random.nextLong()) {
    // key 는 화면 줄 구분용(저장·비교엔 안 쓴다, 회전 뒤에도 살아남게 난수) — 같은 종류·금액 줄이 있어도 서로 다른 줄
    override fun equals(other: Any?) = other is PaymentDraft && other.method == method && other.amount == amount
    override fun hashCode() = 31 * method.hashCode() + amount.hashCode()
}


@Serializable
data class PointAward(val enabled: Boolean, val amount: String)

/** 결제 화면 상태 한 덩어리(회전에도 살린다). */
@Serializable
data class PaymentForm(val entries: List<PaymentDraft>, val pointAward: PointAward, val isPointAwardManual: Boolean = false)

private fun amountOf(value: String) = value.filter { it.isDigit() }.take(9).toIntOrNull() ?: 0

/** 웹 getPaymentEntryDrafts — 저장된 줄 그대로, 없으면 네이버 예약금 줄 + 나머지 빈 종류 줄, 그것도 아니면 전액 한 줄. */
fun paymentDrafts(reservation: Reservation, displayPrice: Int): List<PaymentDraft> {
    val entries = paymentEntriesOf(reservation)
    if (entries.isNotEmpty()) return entries.map { PaymentDraft(it.method, it.amount.toString()) }
    val deposit = reservation.naverDeposit ?: 0
    if (deposit > 0) {
        val remainder = displayPrice - deposit
        return listOfNotNull(PaymentDraft("네이버 예약금", deposit.toString()), PaymentDraft("", remainder.toString()).takeIf { remainder > 0 })
    }
    return listOf(PaymentDraft("", displayPrice.toString()))
}

/** 결제 화면을 열 때 — 적립은 이미 준 적립이 있거나 매장이 서비스 적립을 켰으면 켜 둔다(금액 = 준 적립, 없으면 가격 × 적립률). */
fun PaymentForm.Companion.open(reservation: Reservation, displayPrice: Int, settings: PointSettings) = PaymentForm(
    entries = paymentDrafts(reservation, displayPrice),
    pointAward = PointAward(
        enabled = (reservation.pointEarned ?: 0) > 0 || settings.enableServiceRate,
        amount = (reservation.pointEarned ?: (displayPrice * settings.serviceRate / 100).toInt()).toString(),
    ),
)

/** 웹 handlePaymentEntriesChange + syncPointAward — 줄이 바뀌면 받은 돈(적립금·할인 제외) × 적립률로 적립을 다시(직접 고쳤으면 그대로). */
fun PaymentForm.withEntries(next: List<PaymentDraft>, settings: PointSettings): PaymentForm {
    if (!settings.enableServiceRate || isPointAwardManual) return copy(entries = next)
    val received = next.filter { it.method.isNotEmpty() && isRevenuePaymentMethod(it.method) }.sumOf { amountOf(it.amount) }
    return copy(entries = next, pointAward = PointAward(enabled = true, amount = (received * settings.serviceRate / 100).toInt().toString()))
}

/** 줄 지우기 — 하나뿐이면 빈 줄로 남긴다(웹과 같다). */
fun PaymentForm.removeEntry(index: Int, settings: PointSettings) =
    withEntries(if (entries.size > 1) entries.filterIndexed { i, _ -> i != index } else listOf(PaymentDraft()), settings)

/** 맞춰야 할 금액 — '네이버 예약금' 줄(금액 > 0)이 있으면 서비스 금액 전체, 없으면 예약금을 뺀 실결제(웹 paymentTargetFor). */
private fun paymentTarget(entries: List<PaymentEntry>, reservation: Reservation, displayPrice: Int): Int =
    if (entries.any { it.method == "네이버 예약금" && it.amount > 0 }) displayPrice else displayPrice - (reservation.naverDeposit ?: 0)

/** 웹 handlePaymentSave 의 검사 — 같은 차례·문구. 통과하면 null. */
fun validatePayment(form: PaymentForm, reservation: Reservation, displayPrice: Int, customerPoints: Int, labels: StoreLabels): String? {
    val unselected = form.entries.filter { it.method.isEmpty() }.sumOf { amountOf(it.amount) }
    if (unselected > 0) {
        val advice = if (form.entries.size > 1) "결제 종류를 선택하거나 그 줄을 삭제해 주세요." else "결제 종류를 선택해 주세요."
        return "결제 종류를 선택하지 않은 금액(${formatPrice(unselected)})이 있습니다. $advice"
    }
    val entries = normalizedEntries(form)
    if (entries.isEmpty()) return "결제 종류와 금액을 입력해 주세요."
    val total = entries.sumOf { it.amount }
    val target = paymentTarget(entries, reservation, displayPrice)
    val deposit = reservation.naverDeposit ?: 0
    val depositRows = entries.filter { it.method == "네이버 예약금" }
    if (deposit > 0 && depositRows.isNotEmpty() && depositRows.sumOf { it.amount } != deposit) return "네이버 예약금은 ${formatPrice(deposit)}입니다."
    if (total != target) {
        val gap = target - total
        val targetLabel = if (target == displayPrice) "${labels.service} 금액" else "실결제 금액"
        return "결제 금액이 ${formatPrice(abs(gap))} ${if (gap > 0) "부족합니다" else "많습니다"}. (입력 ${formatPrice(total)} / $targetLabel ${formatPrice(target)})"
    }
    val usageDiff = pointsUsed(entries) - pointsUsed(paymentEntriesOf(reservation))
    if (customerPoints - usageDiff < 0) return "고객 적립금이 부족합니다."
    return null
}

private fun normalizedEntries(form: PaymentForm) =
    form.entries.map { PaymentEntry(it.method, amountOf(it.amount)) }.filter { it.method.isNotEmpty() && it.amount > 0 }

private fun pointsUsed(entries: List<PaymentEntry>) = entries.filter { it.method == "적립금" }.sumOf { it.amount }

/**
 * 결제 저장(검사 통과 뒤). 예약 = 결제완료 + 줄 + 적립. 고객 적립금 = 쓴 만큼 빼고(이전 결제와의 차이) 적립 차이를 더한다(0 아래로 안 내려감),
 * 이력은 '예약 결제 적립금 사용' · '예약 결제 적립'/'예약 결제 적립 조정'.
 */
fun applyPayment(snapshot: LocalDbSnapshot, reservationId: Int, form: PaymentForm, now: Instant = Instant.now()): LocalDbSnapshot {
    val before = snapshot.reservations.firstOrNull { it.id == reservationId } ?: return snapshot
    val entries = normalizedEntries(form)
    if (entries.isEmpty()) return snapshot
    val earned = if (form.pointAward.enabled) amountOf(form.pointAward.amount) else 0
    val after = before.copy(paymentCompleted = true, paymentMethod = entries[0].method, paymentEntries = entries, pointEarned = earned)

    val usageDiff = pointsUsed(entries) - pointsUsed(paymentEntriesOf(before))
    val earnDiff = earned - (before.pointEarned ?: 0)
    val customers = if (usageDiff == 0 && earnDiff == 0) snapshot.customers else snapshot.customers.map { c ->
        if (c.id != before.customerId || c.deleted == true) return@map c
        var balance = c.points ?: 0
        val histories = buildList {
            fun record(type: String, delta: Int, description: String) {
                balance += delta
                add(PointHistoryEntry(pointHistoryId(now, size), type, delta, balance, description, now.toString(), reservationId))
            }
            if (usageDiff != 0) record("payment_use", -usageDiff, "예약 결제 적립금 사용")
            if (earnDiff != 0) record(if (earnDiff > 0) "payment_earn" else "payment_adjust", earnDiff, if (earnDiff > 0) "예약 결제 적립" else "예약 결제 적립 조정")
        }
        c.copy(points = maxOf((c.points ?: 0) - usageDiff + earnDiff, 0), pointHistories = c.pointHistories.orEmpty() + histories)
    }
    return snapshot.copy(
        reservations = snapshot.reservations.map { if (it.id == reservationId) after else it },
        history = snapshot.history + ReservationHistoryEntry(reservationId, before, after, now.toString()),
        customers = customers,
    )
}

/** 웹 예약완료 — 결제한 예약만(아니면 그대로). 이력 한 줄. */
fun completeReservation(snapshot: LocalDbSnapshot, reservationId: Int, now: Instant = Instant.now()): LocalDbSnapshot {
    val before = snapshot.reservations.firstOrNull { it.id == reservationId } ?: return snapshot
    if (!hasCompletedPayment(before)) return snapshot
    val after = before.copy(status = ReservationStatus.COMPLETED)
    return snapshot.copy(
        reservations = snapshot.reservations.map { if (it.id == reservationId) after else it },
        history = snapshot.history + ReservationHistoryEntry(reservationId, before, after, now.toString()),
    )
}
