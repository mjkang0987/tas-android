package kr.co.takeaseat.app.domain

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

// 웹 `utils/revenue.ts`(getRevenueInsights 의 추이·결제수단·채널·담당자 몫, getOperationInsights),
// `revenueChartUtils.ts`(색·순서), `RevenueSection.tsx`(차트 항목·차트 상세) 이식 — 넓은 화면(641px 이상)의 RevenueChartGrid 가 쓴다.

/** 웹 CHANNEL_ORDER·CHANNEL_COLORS — 채널이 없는 예약은 전화예약. */
val CHANNEL_ORDER = listOf("전화예약", "현장방문", "네이버예약", "온라인예약")
val CHANNEL_COLORS = mapOf("전화예약" to "#FB8C00", "현장방문" to "#4285F4", "네이버예약" to "#2DB400", "온라인예약" to "#7C3AED")

/** 웹 PAYMENT_METHOD_COLORS — 결제수단 순서(받은 돈 7종, RECEIVED_PAYMENT_METHODS)대로 돌려 쓴다. */
val PAYMENT_METHOD_COLORS = listOf("#2D7FF9", "#00A896", "#FB8C00", "#E85D75", "#7E57C2", "#4C6EF5", "#8E8E93", "#34A853")

data class ChartPoint(val date: LocalDate, val total: Int)
data class PaymentChartItem(val method: String, val total: Int, val color: String)
data class ChannelChartItem(val channel: String, val count: Int, val color: String)
data class AssigneeChartItem(val assigneeId: Int, val name: String, val color: String, val total: Int, val count: Int)
/** 취소율(담당자, id 없으면 미지정)·노쇼율(고객) 한 줄 — [hit] 은 취소·노쇼 건수. */
data class RateItem(val id: Int?, val total: Int, val hit: Int, val rate: Int)

data class RevenueCharts(
    /** 기간의 모든 날(0원 포함), 충전 포함(담당자를 고르면 뺀다). */
    val series: List<ChartPoint>,
    val paidTotal: Int,
    /** 받은 돈 7종 모두(0원 포함) — 웹 paymentChartItems. */
    val payments: List<PaymentChartItem>,
    val channels: List<ChannelChartItem>,
    val assignees: List<AssigneeChartItem>,
    /** 상위 5 */
    val cancellations: List<RateItem>,
    /** 상위 5 */
    val noshows: List<RateItem>,
    val totalCancelled: Int,
    val totalCancelledRate: Int,
    val totalNoshow: Int,
    val totalNoshowRate: Int,
) {
    val chartMax: Int get() = maxOf(series.maxOfOrNull { it.total } ?: 0, 1)
    /** 매출이 0뿐인 기간은 빈 상태(웹 hasTrend). */
    val hasTrend: Boolean get() = series.any { it.total > 0 }
    val channelTotal: Int get() = channels.sumOf { it.count }
}

private fun percent(part: Int, total: Int) = if (total > 0) (part * 100.0 / total).roundToInt() else 0

fun revenueCharts(
    snapshot: LocalDbSnapshot,
    range: RevenueRange,
    assigneeId: Int?,
    mode: RevenueFilterMode,
    zone: ZoneId = ZoneId.systemDefault(),
): RevenueCharts {
    val catalog = snapshot.services
    val fromKey = range.from.toDateKey()
    val toKey = range.to.toDateKey()
    val inRange = snapshot.reservations.filter { it.date in fromKey..toKey }
    val targets = inRange.filter { isRevenueTarget(it, assigneeId, mode) }
    val sales = if (assigneeId == null) storeSales(snapshot.customers, zone).filter { it.date in range.from..range.to } else emptyList()

    val totalByDate = targets.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { reservationRevenue(it, catalog) } }
    val salesByDate = sales.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.amount } }
    val series = generateSequence(range.from) { it.plusDays(1) }.takeWhile { it <= range.to }
        .map { ChartPoint(it, (totalByDate[it.toDateKey()] ?: 0) + (salesByDate[it] ?: 0)) }.toList()

    val received = targets.flatMap { receivedPayments(it, catalog) }.map { it.method to it.amount } +
        sales.mapNotNull { s -> s.method?.let { it to s.amount } }
    val byMethod = received.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
    val payments = RECEIVED_PAYMENT_METHODS.mapIndexed { i, m -> PaymentChartItem(m, byMethod[m] ?: 0, PAYMENT_METHOD_COLORS[i % PAYMENT_METHOD_COLORS.size]) }

    val byChannel = targets.groupingBy { it.channel ?: "전화예약" }.eachCount()
    val channels = CHANNEL_ORDER.map { ChannelChartItem(it, byChannel[it] ?: 0, CHANNEL_COLORS.getValue(it)) }

    // 웹: 담당자 목록 기준(미지정 매출은 막대에 없다), 고르면 그 담당자만, 퇴직자는 매출이 있을 때만
    val byAssignee = targets.groupBy { it.assigneeId }
    val assignees = snapshot.assignees
        .filter { assigneeId == null || it.id == assigneeId }
        .mapNotNull { a ->
            val list = byAssignee[a.id].orEmpty()
            val total = list.sumOf { reservationRevenue(it, catalog) }
            if (assigneeStatus(a) == "퇴직" && total == 0) null else AssigneeChartItem(a.id, a.name, assigneeColor(a), total, list.size)
        }

    // 웹 getOperationInsights — 상태와 무관한 기간 안 예약(담당자 필터만)
    val operation = inRange.filter { assigneeId == null || it.assigneeId == assigneeId }
    val cancellations = operation.groupBy { it.assigneeId }.map { (id, list) ->
        val hit = list.count { it.status == ReservationStatus.CANCELLED }
        RateItem(id, list.size, hit, percent(hit, list.size))
    }.sortedWith(compareByDescending<RateItem> { it.rate }.thenByDescending { it.hit }.thenByDescending { it.total })
    val noshows = operation.groupBy { it.customerId }.map { (id, list) ->
        val hit = list.count { it.status == ReservationStatus.NOSHOW }
        RateItem(id, list.size, hit, percent(hit, list.size))
    }.sortedWith(compareByDescending<RateItem> { it.rate }.thenByDescending { it.hit }.thenByDescending { it.total })
    val totalCancelled = cancellations.sumOf { it.hit }
    val totalNoshow = noshows.sumOf { it.hit }

    return RevenueCharts(
        series = series,
        paidTotal = received.sumOf { it.second },
        payments = payments,
        channels = channels,
        assignees = assignees,
        cancellations = cancellations.take(5),
        noshows = noshows.take(5),
        totalCancelled = totalCancelled,
        totalCancelledRate = percent(totalCancelled, operation.size),
        totalNoshow = totalNoshow,
        totalNoshowRate = percent(totalNoshow, operation.size),
    )
}

/** 웹 ChartDetailKey — 차트를 누르면 여는 상세. */
sealed interface ChartDetailKey {
    data class Date(val date: LocalDate) : ChartDetailKey
    data class Payment(val method: String) : ChartDetailKey
    data class Assignee(val assigneeId: Int) : ChartDetailKey
    data class Cancellation(val assigneeId: Int?) : ChartDetailKey
    data class Noshow(val customerId: Int) : ChartDetailKey
    data class Channel(val channel: String) : ChartDetailKey
}

data class ChartDetail(val title: String, val summary: String, val reservations: List<Reservation>)

/**
 * 웹 RevenueSection chartDetailLayer. 날짜·결제수단·담당자·채널은 집계 대상 예약(metricReservations·paidReservations),
 * 취소·노쇼는 상태와 무관한 기간 안 예약(allRangeReservations). 날짜 상세 합계엔 충전이 없다(웹과 같다).
 */
fun chartDetail(snapshot: LocalDbSnapshot, range: RevenueRange, assigneeId: Int?, mode: RevenueFilterMode, key: ChartDetailKey): ChartDetail {
    val catalog = snapshot.services
    val fromKey = range.from.toDateKey()
    val toKey = range.to.toDateKey()
    val all = snapshot.reservations
        .filter { it.date in fromKey..toKey && (assigneeId == null || it.assigneeId == assigneeId) }
        .sortedWith(compareBy({ it.date }, { it.startTime }))
    val targets = all.filter { isRevenueTarget(it, assigneeId, mode) }
    fun assigneeName(id: Int?) = snapshot.assignees.firstOrNull { it.id == id }?.name ?: "미지정"
    fun withTotal(title: String, list: List<Reservation>, total: Int = list.sumOf { reservationRevenue(it, catalog) }) =
        ChartDetail(title, "${list.size}건 · ${formatPrice(total)}", list)
    return when (key) {
        is ChartDetailKey.Date -> withTotal("${key.date.toDateKey()} 매출 상세", targets.filter { it.date == key.date.toDateKey() })
        is ChartDetailKey.Payment -> {
            val list = targets.filter { isPaidTarget(it, assigneeId) }.filter { r ->
                if (!r.paymentEntries.isNullOrEmpty()) r.paymentEntries.any { it.method == key.method && it.amount > 0 }
                else r.paymentCompleted == true && r.paymentMethod == key.method
            }
            // 이 결제수단으로 받은 금액만(차트 조각과 같게), 줄 없는 옛 예약은 매출 전체
            val total = list.sumOf { r ->
                if (!r.paymentEntries.isNullOrEmpty()) r.paymentEntries.filter { it.method == key.method }.sumOf { it.amount }
                else reservationRevenue(r, catalog)
            }
            withTotal("${key.method} 결제 상세", list, total)
        }
        is ChartDetailKey.Assignee -> withTotal("${assigneeName(key.assigneeId)} 매출 상세", targets.filter { it.assigneeId == key.assigneeId })
        is ChartDetailKey.Cancellation -> all.filter { it.assigneeId == key.assigneeId && it.status == ReservationStatus.CANCELLED }
            .let { ChartDetail("${assigneeName(key.assigneeId)} 취소 예약 상세", "${it.size}건", it) }
        is ChartDetailKey.Noshow -> all.filter { it.customerId == key.customerId && it.status == ReservationStatus.NOSHOW }
            .let { list -> ChartDetail("${snapshot.customers.firstOrNull { it.id == key.customerId }?.name ?: "고객 미지정"} 노쇼 예약 상세", "${list.size}건", list) }
        is ChartDetailKey.Channel -> withTotal("${key.channel} 상세", targets.filter { (it.channel ?: "전화예약") == key.channel })
    }
}
