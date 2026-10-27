package kr.co.takeaseat.app.domain

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RevenueChartsTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val catalog = listOf(ServiceItem("커트", 30, "헤어", 20000))
    private val range = RevenueRange(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 21))
    private val assignees = listOf(
        Assignee(1, "원장", color = "#111111"),
        Assignee(2, "민실장", color = "#222222"),
        Assignee(3, "퇴직자", status = "퇴직", color = "#333333"),
    )

    private fun r(id: Int, date: String, status: String? = ReservationStatus.COMPLETED, price: Int? = 20000, customerId: Int = 1,
                  assigneeId: Int? = 1, entries: List<PaymentEntry>? = null, channel: String? = null) =
        Reservation(id, date, "10:00", "11:00", "커트", customerId, assigneeId, status, price, paymentEntries = entries, channel = channel)

    private fun snap(vararg rs: Reservation, customers: List<Customer> = listOf(Customer(1, "김", "010"), Customer(2, "이", "011"))) =
        LocalDbSnapshot(customers = customers, reservations = rs.toList(), services = catalog, assignees = assignees)

    @Test fun `추이 - 기간 모든 날, 충전 포함, 담당자를 고르면 충전은 뺀다`() {
        val charge = Customer(2, "이", "011", pointHistories = listOf(
            PointHistoryEntry("h", "recharge", 55000, 55000, "충전", "2026-10-20T01:00:00Z", paidAmount = 50000, paymentMethod = "카드"),
        ))
        val s = snap(r(1, "2026-10-19"), r(2, "2026-10-21", price = 30000), customers = listOf(Customer(1, "김", "010"), charge))
        val all = revenueCharts(s, range, null, RevenueFilterMode.Completed, seoul)
        assertEquals(listOf(20000, 50000, 30000), all.series.map { it.total })
        assertEquals(50000, all.chartMax)
        assertEquals(listOf(20000, 0, 30000), revenueCharts(s, range, 1, RevenueFilterMode.Completed, seoul).series.map { it.total })
    }

    @Test fun `추이 - 매출이 0뿐이면 빈 상태, 눈금 최대는 1`() {
        val c = revenueCharts(snap(r(1, "2026-10-19", status = ReservationStatus.CANCELLED)), range, null, RevenueFilterMode.Completed, seoul)
        assertFalse(c.hasTrend)
        assertEquals(1, c.chartMax)
        assertEquals(3, c.series.size)
    }

    @Test fun `결제수단 - 받은 돈 7종 순서·색, 적립금은 빼고, 옛 결제는 매출 전체`() {
        val s = snap(
            r(1, "2026-10-19", entries = listOf(PaymentEntry("카드", 15000), PaymentEntry("적립금", 5000))),
            r(2, "2026-10-20").copy(paymentCompleted = true, paymentMethod = "현금"),
            r(3, "2026-10-20", status = ReservationStatus.ACTIVE, entries = listOf(PaymentEntry("카드", 9000))), // 예약완료만이라 빠짐
        )
        val c = revenueCharts(s, range, null, RevenueFilterMode.Completed, seoul)
        assertEquals(RECEIVED_PAYMENT_METHODS, c.payments.map { it.method })
        assertEquals(PAYMENT_METHOD_COLORS.take(7), c.payments.map { it.color })
        assertEquals(15000, c.payments.first { it.method == "카드" }.total)
        assertEquals(20000, c.payments.first { it.method == "현금" }.total)
        assertEquals(35000, c.paidTotal)
    }

    @Test fun `채널 - 없으면 전화예약, 웹 순서 4종`() {
        val s = snap(r(1, "2026-10-19"), r(2, "2026-10-19", channel = "네이버예약"), r(3, "2026-10-20", channel = "네이버예약"))
        val c = revenueCharts(s, range, null, RevenueFilterMode.Completed, seoul)
        assertEquals(CHANNEL_ORDER, c.channels.map { it.channel })
        assertEquals(listOf(1, 0, 2, 0), c.channels.map { it.count })
        assertEquals(3, c.channelTotal)
    }

    @Test fun `담당자별 - 목록 기준, 퇴직자는 매출 있을 때만, 고르면 그 담당자만`() {
        val s = snap(r(1, "2026-10-19", assigneeId = 1), r(2, "2026-10-20", assigneeId = 1), r(3, "2026-10-20", assigneeId = null))
        val c = revenueCharts(s, range, null, RevenueFilterMode.Completed, seoul)
        assertEquals(listOf(1, 2), c.assignees.map { it.assigneeId })
        assertEquals(AssigneeChartItem(1, "원장", "#111111", 40000, 2), c.assignees[0])
        assertEquals(0, c.assignees[1].total)
        val retired = revenueCharts(snap(r(1, "2026-10-19", assigneeId = 3)), range, null, RevenueFilterMode.Completed, seoul)
        assertTrue(retired.assignees.any { it.assigneeId == 3 })
        assertEquals(listOf(2), revenueCharts(s, range, 2, RevenueFilterMode.Completed, seoul).assignees.map { it.assigneeId })
    }

    @Test fun `취소율·노쇼율 - 상태 무관 전체 예약 기준, 반올림, 웹 정렬, 상위 5`() {
        val s = snap(
            r(1, "2026-10-19", assigneeId = 1, status = ReservationStatus.CANCELLED),
            r(2, "2026-10-19", assigneeId = 1),
            r(3, "2026-10-20", assigneeId = 1),
            r(4, "2026-10-20", assigneeId = 2, status = ReservationStatus.CANCELLED, customerId = 2),
            r(5, "2026-10-21", assigneeId = null, status = ReservationStatus.NOSHOW, customerId = 2),
            r(6, "2026-10-22", assigneeId = 2, status = ReservationStatus.CANCELLED), // 기간 밖
        )
        val c = revenueCharts(s, range, null, RevenueFilterMode.Completed, seoul)
        assertEquals(listOf(RateItem(2, 1, 1, 100), RateItem(1, 3, 1, 33), RateItem(null, 1, 0, 0)), c.cancellations)
        assertEquals(2, c.totalCancelled)
        assertEquals(40, c.totalCancelledRate)
        assertEquals(listOf(RateItem(2, 2, 1, 50), RateItem(1, 3, 0, 0)), c.noshows)
        assertEquals(20, c.totalNoshowRate)
        val many = snap(*(1..7).map { r(it, "2026-10-19", customerId = it) }.toTypedArray())
        assertEquals(5, revenueCharts(many, range, null, RevenueFilterMode.Completed, seoul).noshows.size)
    }

    @Test fun `비율 반올림 - 웹 Math round 나누기 먼저와 같다`() {
        assertEquals(57, chartPercent(23, 40)) // 곱하기 먼저면 58
        assertEquals(14, chartPercent(29, 200)) // 곱하기 먼저면 15
        assertEquals(33, chartPercent(1, 3))
        assertEquals(0, chartPercent(5, 0))
    }

    @Test fun `노쇼율 동점 - 저장 순서가 아니라 첫 예약 날짜가 이른 고객이 앞(웹과 같다)`() {
        // 늦은 날짜 예약을 먼저 저장 — 웹은 날짜순으로 묶어 2(19일)가 1(21일)보다 앞
        val s = snap(r(1, "2026-10-21", customerId = 1), r(2, "2026-10-19", customerId = 2))
        assertEquals(listOf(2, 1), revenueCharts(s, range, null, RevenueFilterMode.Completed, seoul).noshows.map { it.id })
    }

    @Test fun `차트 상세 - 웹 chartDetailLayer 제목·요약`() {
        val s = snap(
            r(1, "2026-10-19", entries = listOf(PaymentEntry("카드", 15000), PaymentEntry("현금", 5000))),
            r(2, "2026-10-20", assigneeId = 2, status = ReservationStatus.CANCELLED),
            r(3, "2026-10-21", customerId = 2, status = ReservationStatus.NOSHOW, channel = "현장방문"),
            r(4, "2026-10-21", channel = "현장방문"),
        )
        fun d(key: ChartDetailKey) = chartDetail(s, range, null, RevenueFilterMode.Completed, key)
        assertEquals(ChartDetail("2026-10-19 매출 상세", "1건 · 20,000원", listOf(s.reservations[0])), d(ChartDetailKey.Date(LocalDate.of(2026, 10, 19))))
        assertEquals("1건 · 15,000원", d(ChartDetailKey.Payment("카드")).summary)
        assertEquals(ChartDetail("민실장 취소 예약 상세", "1건", listOf(s.reservations[1])), d(ChartDetailKey.Cancellation(2)))
        assertEquals("이 노쇼 예약 상세", d(ChartDetailKey.Noshow(2)).title)
        assertEquals(ChartDetail("현장방문 상세", "1건 · 20,000원", listOf(s.reservations[3])), d(ChartDetailKey.Channel("현장방문")))
        assertEquals("원장 매출 상세", d(ChartDetailKey.Assignee(1)).title)
    }
}
