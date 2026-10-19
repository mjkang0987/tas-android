package kr.co.takeaseat.app.domain

import java.time.Instant
import kotlin.random.Random

// 웹 `store/calendarStore.ts` cancelReservation·restoreReservation(게스트 경로) 이식 — 스냅샷을 받아 바뀐 스냅샷을 돌려준다.

/**
 * 상세 하단 버튼 — 웹 ReservationDetailFooterActions 보기 모드(차례도 웹과 같다):
 * 결제완료/결제수단 변경 · 예약완료(결제했고 아직 완료 전) · 취소·노쇼(미결제) · 변경.
 * 삭제는 웹도 게스트엔 없다(owner·manager 만). 삭제(익명)된 고객의 예약은 결제·변경을 막는다(웹과 같다).
 */
enum class DetailAction { Pay, Complete, Cancel, Noshow, Edit, Restore }

fun detailActions(reservation: Reservation, customerDeleted: Boolean = false): List<DetailAction> = when (reservation.status) {
    ReservationStatus.REQUESTED -> emptyList() // 확정·거절은 온라인 예약(로그인) 전용
    ReservationStatus.CANCELLED, ReservationStatus.NOSHOW -> listOf(DetailAction.Restore)
    else -> buildList {
        val paid = hasCompletedPayment(reservation)
        if (!customerDeleted) add(DetailAction.Pay)
        if (paid && reservation.status != ReservationStatus.COMPLETED) add(DetailAction.Complete)
        if (!paid) addAll(listOf(DetailAction.Cancel, DetailAction.Noshow))
        if (!customerDeleted) add(DetailAction.Edit)
    }
}

private fun historyEntry(before: Reservation, after: Reservation, now: Instant) =
    ReservationHistoryEntry(reservationId = before.id, before = before, after = after, timestamp = now.toString())

/**
 * 취소([ReservationStatus.CANCELLED]) 또는 노쇼. 취소면 결제를 비우고(웹 PATCH 와 같은 결과 — 남기면 되돌린 뒤 다시 취소할 때 또 돌려준다)
 * 결제에 쓴 적립금을 돌려준다(그 결제로 준 적립은 회수). 이력 한 줄을 남긴다.
 */
fun changeReservationStatus(snapshot: LocalDbSnapshot, reservationId: Int, status: String, now: Instant = Instant.now()): LocalDbSnapshot {
    val before = snapshot.reservations.firstOrNull { it.id == reservationId } ?: return snapshot
    val after = if (status == ReservationStatus.CANCELLED && before.status != ReservationStatus.CANCELLED) {
        before.copy(status = status, paymentEntries = emptyList(), paymentCompleted = false, paymentMethod = null, pointEarned = 0)
    } else {
        before.copy(status = status)
    }
    val refund = if (status == ReservationStatus.CANCELLED) pointRefundOnCancel(before) else 0
    return snapshot.copy(
        reservations = snapshot.reservations.map { if (it.id == reservationId) after else it },
        history = snapshot.history + historyEntry(before, after, now),
        customers = if (refund == 0) snapshot.customers else snapshot.customers.map { c ->
            if (c.id != before.customerId || c.deleted == true) c else refundPoints(c, refund, reservationId, now)
        },
    )
}

/** 웹 restoreReservation — 취소·노쇼를 진행으로 되돌린다(결제는 취소 때 비웠으니 그대로 빈 채). */
fun restoreReservation(snapshot: LocalDbSnapshot, reservationId: Int, now: Instant = Instant.now()): LocalDbSnapshot {
    val before = snapshot.reservations.firstOrNull { it.id == reservationId } ?: return snapshot
    val after = before.copy(status = ReservationStatus.ACTIVE)
    return snapshot.copy(
        reservations = snapshot.reservations.map { if (it.id == reservationId) after else it },
        history = snapshot.history + historyEntry(before, after, now),
    )
}

// 웹 refundPointsOnCancel + appendPointHistories: 잔액은 0 아래로 안 내려가고, 이력 잔액은 (이전 잔액 + 변화)
private fun refundPoints(customer: Customer, delta: Int, reservationId: Int, now: Instant): Customer {
    val balance = (customer.points ?: 0) + delta
    val entry = PointHistoryEntry(
        id = "${now.toEpochMilli()}-0-${Random.nextLong(36L * 36 * 36 * 36 * 36 * 36).toString(36).padStart(6, '0')}", // 웹: 시각-순번-36진수 6자리
        type = "payment_adjust",
        delta = delta,
        balance = balance,
        description = if (delta > 0) "예약 취소 — 결제에 쓴 적립금 반환" else "예약 취소 — 결제 적립금 회수",
        createdAt = now.toString(),
        relatedReservationId = reservationId,
    )
    return customer.copy(points = maxOf(balance, 0), pointHistories = customer.pointHistories.orEmpty() + entry)
}
