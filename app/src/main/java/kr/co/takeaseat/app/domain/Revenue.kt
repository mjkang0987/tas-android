package kr.co.takeaseat.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

// 웹 `utils/revenue.ts`(resolveRevenue·집계 대상·getRangeRevenue·getRevenueInsights 중 요약 카드 몫),
// `features/sales/model.ts`(buildStoreSales·addSalesToRevenue), `revenueChartUtils.ts`(기간 이동·상한),
// `pages/settings.tsx`(기본 기간·빠른 기간) 이식. 넓은 화면 그래프 몫은 `RevenueCharts.kt`.

/** 웹 RevenueFilterMode — 예약완료만 / 취소·노쇼·요청 뺀 전체 예약. */
enum class RevenueFilterMode { Completed, Booked }

/** 웹 RevenueQuickRange — 오늘 · 7일(오늘−7) · 30일(오늘−30). */
enum class RevenueQuickRange { Today, Week, Month }

/** 매출 기간. start·end 는 고른 그대로(뒤집혀 있을 수 있다 — 웹과 같다), 계산은 [from]~[to]. */
data class RevenueRange(val start: LocalDate, val end: LocalDate) {
    val from: LocalDate get() = minOf(start, end)
    val to: LocalDate get() = maxOf(start, end)
}

/** 웹: 시작~종료 간격 상한(일). 365면 윤년 포함 만 1년. */
const val REVENUE_MAX_RANGE_DAYS = 365L

/** 웹 기본 — 이번 달 1일 ~ 오늘. */
fun defaultRevenueRange(today: LocalDate) = RevenueRange(today.withDayOfMonth(1), today)

fun quickRevenueRange(quick: RevenueQuickRange, today: LocalDate) = when (quick) {
    RevenueQuickRange.Today -> RevenueRange(today, today)
    RevenueQuickRange.Week -> RevenueRange(today.minusDays(7), today)
    RevenueQuickRange.Month -> RevenueRange(today.minusDays(30), today)
}

/** 지금 기간이 빠른 기간 중 하나와 같으면 그것(버튼 강조), 아니면 null. */
fun quickRangeOf(range: RevenueRange, today: LocalDate): RevenueQuickRange? =
    RevenueQuickRange.entries.firstOrNull { quickRevenueRange(it, today) == range }

/** 웹 clampRevenueRange — 상한을 넘으면 끝을 남기고 시작을 당긴다. */
fun clampRevenueRange(range: RevenueRange): RevenueRange {
    if (ChronoUnit.DAYS.between(range.from, range.to) <= REVENUE_MAX_RANGE_DAYS) return range
    val clamped = range.to.minusDays(REVENUE_MAX_RANGE_DAYS)
    return if (range.start <= range.end) range.copy(start = clamped) else range.copy(end = clamped)
}

/** 웹 handleMoveRange — 오늘이면 하루씩, 7일·30일이면 그만큼, 직접 고른 기간은 그 길이(최소 1일)만큼. 오늘을 넘지 않는다. */
fun moveRevenueRange(range: RevenueRange, forward: Boolean, today: LocalDate): RevenueRange {
    val quick = quickRangeOf(range, today)
    val span = ChronoUnit.DAYS.between(range.from, range.to)
    if (quick == RevenueQuickRange.Today) {
        val next = minOf(range.to.plusDays(if (forward) 1 else -1), today)
        return RevenueRange(next, next)
    }
    val shift = when (quick) {
        RevenueQuickRange.Week -> 7L
        RevenueQuickRange.Month -> 30L
        else -> maxOf(span, 1L)
    }
    if (!forward) return RevenueRange(range.from.minusDays(shift), range.to.minusDays(shift))
    val end = minOf(range.to.plusDays(shift), today)
    return RevenueRange(end.minusDays(span), end)
}

/** 웹 resolveRevenue — 가격(없으면 서비스 합)에서 할인·적립금·회원권으로 낸 만큼 뺀 받은 돈. */
fun reservationRevenue(r: Reservation, catalog: List<ServiceItem>): Int {
    val price = r.price ?: servicePriceTotal(r.service, catalog)
    val deducted = r.paymentEntries.orEmpty().filter { !isRevenuePaymentMethod(it.method) && it.amount > 0 }.sumOf { it.amount }
    return maxOf(price - deducted, 0)
}

/** 웹 getDailyRevenue(…, 'booked').total — 캘린더 목록 머리줄 합계(취소·노쇼·신청 뺀 예약의 매출). */
fun bookedRevenueTotal(reservations: List<Reservation>, catalog: List<ServiceItem>): Int =
    reservations.filter { isRevenueTarget(it, null, RevenueFilterMode.Booked) }.sumOf { reservationRevenue(it, catalog) }

fun isRevenueTarget(r: Reservation, assigneeId: Int?, mode: RevenueFilterMode): Boolean {
    if (assigneeId != null && r.assigneeId != assigneeId) return false
    return when (mode) {
        RevenueFilterMode.Completed -> r.status == ReservationStatus.COMPLETED
        RevenueFilterMode.Booked -> r.status != ReservationStatus.CANCELLED && r.status != ReservationStatus.NOSHOW && r.status != ReservationStatus.REQUESTED
    }
}

/** 웹 resolvePaymentEntries — 받은 돈 줄(할인·적립금·회원권 뺌), 줄 없는 옛 결제는 결제수단 하나 + 매출 전체. */
fun receivedPayments(r: Reservation, catalog: List<ServiceItem>): List<PaymentEntry> = when {
    !r.paymentEntries.isNullOrEmpty() -> r.paymentEntries.filter { it.amount > 0 && isRevenuePaymentMethod(it.method) }
    r.paymentCompleted == true && !r.paymentMethod.isNullOrEmpty() -> listOf(PaymentEntry(r.paymentMethod, reservationRevenue(r, catalog)))
    else -> emptyList()
}

private fun paidAmount(r: Reservation, catalog: List<ServiceItem>): Int = receivedPayments(r, catalog).sumOf { it.amount }

/** 웹 StoreSale — 예약 밖에서 받은 돈. 게스트는 적립금 충전뿐(회원권 판매는 서버 전용). */
data class StoreSale(val date: LocalDate, val amount: Int, val method: String?, val customerId: Int, val label: String)

/** 웹 buildStoreSales(충전분) — 받은 금액이 기록된 충전만(예전 충전은 모름), 받은 날(기기 시간대) 매출. */
fun storeSales(customers: List<Customer>, zone: ZoneId = ZoneId.systemDefault()): List<StoreSale> = customers.flatMap { c ->
    c.pointHistories.orEmpty().mapNotNull { h ->
        val paid = h.paidAmount ?: 0
        if (h.type != "recharge" || paid <= 0) return@mapNotNull null
        val date = try { Instant.parse(h.createdAt).atZone(zone).toLocalDate() } catch (_: DateTimeParseException) { return@mapNotNull null }
        // 빈 결제수단은 없는 것으로(웹 `if (sale.method)` — 결제완료에 넣지 않는다)
        StoreSale(date, paid, h.paymentMethod?.takeIf { it.isNotEmpty() }, c.id, "적립금 충전")
    }
}

/** 일별 목록 한 줄. count 는 예약 수(충전은 세지 않는다 — 웹과 같다). */
data class RevenueDay(val date: LocalDate, val total: Int, val count: Int, val reservations: List<Reservation>, val sales: List<StoreSale>)

data class RevenueSummary(
    /** 최근 날짜부터. 예약이나 충전이 있는 날만. */
    val days: List<RevenueDay>,
    val total: Int,
    val count: Int,
    /** 총 매출 중 충전분 — 요약 카드 '회원권·충전 n원 포함' */
    val salesTotal: Int,
    val paidTotal: Int,
    val newCustomerCount: Int,
    val returningCustomerCount: Int,
)

/**
 * 웹 RevenueSection 의 요약 카드·일별 목록 몫. 담당자를 고르면 충전(담당자 없는 매출)은 뺀다.
 * 신규 = 집계 대상 예약 중 첫 방문(전체 기간 기준)이 이 기간 안인 고객, 나머지는 재방문.
 */
fun revenueSummary(
    snapshot: LocalDbSnapshot,
    range: RevenueRange,
    assigneeId: Int?,
    mode: RevenueFilterMode,
    zone: ZoneId = ZoneId.systemDefault(),
): RevenueSummary {
    val catalog = snapshot.services
    val targets = snapshot.reservations.filter { isRevenueTarget(it, assigneeId, mode) }
    val firstVisit = targets.groupBy { it.customerId }.mapValues { (_, list) -> list.minOf { it.date } }
    val byDate = targets.groupBy { it.date }
    val sales = if (assigneeId == null) storeSales(snapshot.customers, zone).filter { it.date in range.from..range.to } else emptyList()
    val salesByDate = sales.groupBy { it.date }

    val days = mutableListOf<RevenueDay>()
    val customerIds = mutableSetOf<Int>()
    var paidTotal = 0
    var date = range.from
    while (date <= range.to) {
        val reservations = byDate[date.toDateKey()].orEmpty().sortedBy { it.startTime }
        val daySales = salesByDate[date].orEmpty()
        if (reservations.isNotEmpty() || daySales.isNotEmpty()) {
            days += RevenueDay(date, reservations.sumOf { reservationRevenue(it, catalog) } + daySales.sumOf { it.amount }, reservations.size, reservations, daySales)
        }
        reservations.forEach { customerIds += it.customerId; paidTotal += paidAmount(it, catalog) }
        date = date.plusDays(1)
    }
    val fromKey = range.from.toDateKey()
    val toKey = range.to.toDateKey()
    val newCount = customerIds.count { id -> firstVisit[id]?.let { it in fromKey..toKey } == true }
    return RevenueSummary(
        days = days.asReversed(),
        total = days.sumOf { it.total },
        count = days.sumOf { it.count },
        salesTotal = sales.sumOf { it.amount },
        paidTotal = paidTotal + sales.filter { it.method != null }.sumOf { it.amount },
        newCustomerCount = newCount,
        returningCustomerCount = customerIds.size - newCount,
    )
}

/** 웹 RevenueMetricKey — 요약 카드 순서. */
enum class RevenueMetric(val title: String) { Sales("총 매출 상세"), Count("예약 건수 상세"), New("신규 고객 상세"), Returning("재방문 고객 상세"), Paid("결제완료 상세") }

/** 신규·재방문 창의 고객 한 줄 — 기간 안 첫 방문일(재방문은 그 전 방문일도). */
data class MetricCustomer(val customer: Customer, val visitDate: String, val prevVisitDate: String?)

data class RevenueMetricDetail(val summary: String, val reservations: List<Reservation>, val customers: List<MetricCustomer>)

/** 웹 isPaidReservationTarget — 취소·노쇼·신청 빼고, 받은 돈 줄이 있거나(줄 없는 옛 데이터는 결제완료 표시) 한 예약. */
fun isPaidTarget(r: Reservation, assigneeId: Int?): Boolean {
    if (assigneeId != null && r.assigneeId != assigneeId) return false
    if (r.status == ReservationStatus.CANCELLED || r.status == ReservationStatus.NOSHOW || r.status == ReservationStatus.REQUESTED) return false
    return if (!r.paymentEntries.isNullOrEmpty()) r.paymentEntries.any { it.amount > 0 } else r.paymentCompleted == true
}

/** 웹 신규·재방문 설명 — 집계 기준에 따라. */
fun metricSubtitle(metric: RevenueMetric, mode: RevenueFilterMode): String? = when (metric) {
    RevenueMetric.New -> if (mode == RevenueFilterMode.Completed) "선택 기간 안에서 첫 예약완료가 발생한 고객 목록" else "선택 기간 안에서 첫 예약이 발생한 고객 목록"
    RevenueMetric.Returning -> if (mode == RevenueFilterMode.Completed) "선택 기간 내 예약완료가 있고, 그 이전 예약완료 이력이 있는 고객 목록" else "선택 기간 내 예약이 있고, 그 이전 예약 이력이 있는 고객 목록"
    else -> null
}

/**
 * 웹 RevenueSection metricLayer — 기간 안 집계 대상 예약(날짜·시작 순). 신규 = 고객별 기간 안 첫 예약이 전체 첫 방문인 것,
 * 재방문 = 그 전에 방문이 있는 것(이전 방문 = 그 예약 전 가장 최근 방문일). 요약 금액은 요약 카드와 같은 값([summary]).
 */
fun revenueMetricDetail(
    snapshot: LocalDbSnapshot,
    range: RevenueRange,
    assigneeId: Int?,
    mode: RevenueFilterMode,
    metric: RevenueMetric,
    summary: RevenueSummary,
): RevenueMetricDetail {
    val targets = snapshot.reservations.filter { isRevenueTarget(it, assigneeId, mode) }
    val fromKey = range.from.toDateKey()
    val toKey = range.to.toDateKey()
    val inRange = targets.filter { it.date in fromKey..toKey }.sortedWith(compareBy({ it.date }, { it.startTime }))
    val visitDates = targets.groupBy { it.customerId }.mapValues { (_, list) -> list.map { it.date }.distinct() }
    val firstVisit = visitDates.mapValues { (_, dates) -> dates.min() }
    val firstInRange = inRange.distinctBy { it.customerId }
    val customers = snapshot.customers.associateBy { it.id }
    fun entries(list: List<Reservation>, withPrev: Boolean) = list.mapNotNull { r ->
        val c = customers[r.customerId] ?: return@mapNotNull null
        MetricCustomer(c, r.date, if (withPrev) visitDates[r.customerId].orEmpty().filter { it < r.date }.maxOrNull() else null)
    }
    return when (metric) {
        RevenueMetric.Sales -> RevenueMetricDetail("${inRange.size}건 · ${formatPrice(summary.total)}", inRange, emptyList())
        RevenueMetric.Count -> RevenueMetricDetail("${inRange.size}건", inRange, emptyList())
        RevenueMetric.New -> firstInRange.filter { firstVisit[it.customerId] == it.date }
            .let { RevenueMetricDetail("${it.size}명", it, entries(it, withPrev = false)) }
        RevenueMetric.Returning -> firstInRange.filter { firstVisit[it.customerId]?.let { first -> first < it.date } == true }
            .let { RevenueMetricDetail("${it.size}명", it, entries(it, withPrev = true)) }
        RevenueMetric.Paid -> inRange.filter { isPaidTarget(it, assigneeId) }
            .let { RevenueMetricDetail("${it.size}건 · ${formatPrice(summary.paidTotal)}", it, emptyList()) }
    }
}

/** 웹 formatVisitGap — 7일 안은 n일전, 4주 안은 n주전, 1년 안은 n달전(30일, 최소 1), 그 위는 n년전. */
fun formatVisitGap(visitDate: String, prevVisitDate: String): String {
    val days = ChronoUnit.DAYS.between(LocalDate.parse(prevVisitDate), LocalDate.parse(visitDate))
    return when {
        days < 7 -> "${days}일전"
        days < 28 -> "${days / 7}주전"
        // 웹은 28~29일을 30으로 나눠 '0달전'이라 했다 — 최소 1달
        days < 365 -> "${maxOf(days / 30, 1)}달전"
        else -> "${days / 365}년전"
    }
}
