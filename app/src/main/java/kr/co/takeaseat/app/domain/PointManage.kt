package kr.co.takeaseat.app.domain

import java.time.Instant
import kotlin.math.floor

// 웹 `settings/PointManageSection.tsx`(applyPoints·고객 목록) + `PointSettingsTab.tsx`(정책 칸) 이식.

/** 웹 customers — 고를 수 있는(지우지 않은) 고객, 이름순. */
fun pointCustomers(customers: List<Customer>): List<Customer> = sortCustomersByName(customers.filter(::isSelectableCustomer))

/** 웹 customersWithPoints — 잔액이 남은 고객, 잔액 큰 순(같으면 이름순). */
fun customersWithPoints(customers: List<Customer>): List<Customer> =
    pointCustomers(customers).filter { (it.points ?: 0) > 0 }.sortedByDescending { it.points ?: 0 }

/** 웹 filteredCustomers — 이름이나 연락처에 검색어가 들어 있는 고객(공백만이면 전체). */
fun searchPointCustomers(customers: List<Customer>, keyword: String): List<Customer> {
    val k = keyword.trim()
    return if (k.isEmpty()) customers else customers.filter { it.name.contains(k) || it.tel.contains(k) }
}

/**
 * 웹 applyPoints — 수동 적립·차감 + 이력('수동 적립'·'수동 차감'). 0 이하면 그대로.
 * 웹은 차감을 0 아래로 안 내리면서 이력엔 친 금액을 적어 잔액이 음수로 남았다 → 실제로 뺀 만큼만 적는다(뺄 게 없으면 그대로).
 * 잔액이 Int 를 넘는 적립은 받지 않는다(충전과 같은 막기).
 */
fun adjustPoints(snapshot: LocalDbSnapshot, customerId: Int, amount: Int, add: Boolean, now: Instant = Instant.now()): LocalDbSnapshot {
    if (amount <= 0) return snapshot
    val customer = snapshot.customers.firstOrNull { it.id == customerId } ?: return snapshot
    val current = customer.points ?: 0
    val delta = if (add) amount else -minOf(amount, current)
    if (delta == 0 || (add && current.toLong() + amount > Int.MAX_VALUE)) return snapshot
    return snapshot.copy(customers = snapshot.customers.map { c ->
        if (c.id != customerId) return@map c
        val balance = (c.points ?: 0) + delta
        val entry = PointHistoryEntry(
            id = pointHistoryId(now, 0),
            type = if (add) "manual_add" else "manual_subtract",
            delta = delta,
            balance = balance,
            description = if (add) "수동 적립" else "수동 차감",
            createdAt = now.toString(),
        )
        c.copy(points = balance, pointHistories = c.pointHistories.orEmpty() + entry)
    })
}

/** 웹 적립 퍼센트 칸 — 숫자만, 100 넘으면 100. */
fun parseServiceRate(raw: String): Double = minOf(raw.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0, 100).toDouble()

/** 웹 충전 기준 삭제 — 하나만 남으면 지우지 않고 0 으로. */
fun removeRechargeRule(rules: List<PointRechargeRule>, index: Int): List<PointRechargeRule> =
    if (rules.size > 1) rules.filterIndexed { i, _ -> i != index } else listOf(PointRechargeRule(0, 0))

/** 웹 '추가 적립 n%' — 추가금 ÷ 기준금액(JS Math.round), 기준이 0 이면 0%. */
fun rechargeBonusPercent(rule: PointRechargeRule): String =
    if (rule.baseAmount > 0) "${floor(rule.bonusAmount.toDouble() / rule.baseAmount * 100 + 0.5).toInt()}%" else "0%"
