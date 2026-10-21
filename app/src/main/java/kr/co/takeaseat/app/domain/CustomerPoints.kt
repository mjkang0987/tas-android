package kr.co.takeaseat.app.domain

import java.time.Instant
import kotlin.random.Random

// 웹 `address/AddressCustomerRecharge.tsx`(handleCharge) + `features/customers/model.ts`(POINT_HISTORY_LABELS·appendPointHistories) 이식.

/** 웹 POINT_HISTORY_LABELS. */
val POINT_HISTORY_LABELS = mapOf(
    "manual_add" to "수동 적립",
    "manual_subtract" to "수동 차감",
    "recharge" to "충전",
    "payment_use" to "결제 사용",
    "payment_earn" to "결제 적립",
    "payment_adjust" to "적립 조정",
)

/** 웹 RECEIVED_PAYMENT_METHODS — 실제로 돈을 받는 결제수단(충전에 고른다). */
val RECEIVED_PAYMENT_METHODS = listOf("현금", "현금+현금영수증", "카드", "네이버페이", "지역화폐", "지역화폐+현금영수증", "상품권")

/** 웹 appendPointHistories 의 id — 시각-순번-36진수 6자리. */
internal fun pointHistoryId(now: Instant, index: Int): String =
    "${now.toEpochMilli()}-$index-${Random.nextLong(36L * 36 * 36 * 36 * 36 * 36).toString(36).padStart(6, '0')}"

/** 충전 선택지 하나 — 적립(total) = 기준 + 보너스, 받은 돈(paid) = 기준. */
data class RechargeOption(val label: String, val total: Int, val paid: Int)

fun rechargeOptions(settings: PointSettings): List<RechargeOption> = settings.rechargeRules.map {
    RechargeOption("${formatPrice(it.baseAmount)} + ${formatPrice(it.bonusAmount)}", it.baseAmount + it.bonusAmount, it.baseAmount)
}

/**
 * 웹 handleCharge — 고객 적립금 + [amount], 이력 '충전'(받은 돈·결제수단 함께). 직접입력은 받은 돈 = 충전액. 0 이하면 그대로.
 */
fun rechargePoints(
    snapshot: LocalDbSnapshot,
    customerId: Int,
    amount: Int,
    paidAmount: Int,
    paymentMethod: String,
    custom: Boolean,
    now: Instant = Instant.now(),
): LocalDbSnapshot {
    if (amount <= 0) return snapshot
    // 잔액이 Int 범위를 넘으면(9자리 직접입력을 여러 번) 음수로 뒤집힌다 — 그런 충전은 받지 않는다
    val current = snapshot.customers.firstOrNull { it.id == customerId }?.points ?: 0
    if (current.toLong() + amount > Int.MAX_VALUE) return snapshot
    return snapshot.copy(customers = snapshot.customers.map { c ->
        if (c.id != customerId) return@map c
        val balance = (c.points ?: 0) + amount
        val entry = PointHistoryEntry(
            id = pointHistoryId(now, 0),
            type = "recharge",
            delta = amount,
            balance = balance,
            description = if (custom) "적립금 직접 충전" else "적립금 기준 충전",
            createdAt = now.toString(),
            paidAmount = paidAmount,
            paymentMethod = paymentMethod,
        )
        c.copy(points = balance, pointHistories = c.pointHistories.orEmpty() + entry)
    })
}
