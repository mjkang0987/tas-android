package kr.co.takeaseat.app.domain

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RevenueTest {
    private val today = LocalDate.of(2026, 10, 21)
    private val seoul = ZoneId.of("Asia/Seoul")
    private val catalog = listOf(ServiceItem("커트", 30, "헤어", 20000), ServiceItem("펌", 60, "헤어", 50000))

    private fun r(id: Int, date: String, status: String? = null, price: Int? = null, customerId: Int = 1, assigneeId: Int? = 1,
                  entries: List<PaymentEntry>? = null, start: String = "10:00") =
        Reservation(id, date, start, "11:00", "커트", customerId, assigneeId, status, price, paymentEntries = entries)

    @Test fun `기간 - 기본은 이번 달 1일부터, 빠른 기간은 오늘 기준, 같으면 강조`() {
        assertEquals(RevenueRange(LocalDate.of(2026, 10, 1), today), defaultRevenueRange(today))
        assertEquals(RevenueRange(LocalDate.of(2026, 10, 14), today), quickRevenueRange(RevenueQuickRange.Week, today))
        assertEquals(RevenueRange(LocalDate.of(2026, 9, 21), today), quickRevenueRange(RevenueQuickRange.Month, today))
        assertEquals(RevenueQuickRange.Today, quickRangeOf(RevenueRange(today, today), today))
        assertNull(quickRangeOf(defaultRevenueRange(today), today))
    }

    @Test fun `기간 이동 - 오늘은 하루씩, 7일은 7일씩, 직접 고른 기간은 그 길이만큼, 오늘을 넘지 않는다`() {
        assertEquals(RevenueRange(today.minusDays(1), today.minusDays(1)), moveRevenueRange(RevenueRange(today, today), false, today))
        assertEquals(RevenueRange(today, today), moveRevenueRange(RevenueRange(today, today), true, today))
        val week = quickRevenueRange(RevenueQuickRange.Week, today)
        assertEquals(RevenueRange(today.minusDays(14), today.minusDays(7)), moveRevenueRange(week, false, today))
        val custom = RevenueRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10))
        assertEquals(RevenueRange(LocalDate.of(2026, 9, 22), LocalDate.of(2026, 10, 1)), moveRevenueRange(custom, false, today))
        // 앞으로 가다 오늘에 닿으면 끝을 오늘로, 길이는 그대로
        assertEquals(RevenueRange(LocalDate.of(2026, 10, 12), today), moveRevenueRange(RevenueRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 14)), true, today))
    }

    @Test fun `기간 상한 - 365일을 넘으면 끝을 남기고 시작을 당긴다`() {
        val long = RevenueRange(LocalDate.of(2024, 1, 1), today)
        assertEquals(RevenueRange(today.minusDays(365), today), clampRevenueRange(long))
        val reversed = RevenueRange(today, LocalDate.of(2024, 1, 1))
        assertEquals(RevenueRange(today, today.minusDays(365)), clampRevenueRange(reversed))
    }

    @Test fun `매출 - 가격 없으면 서비스 합, 할인·적립금은 뺀다`() {
        assertEquals(20000, reservationRevenue(r(1, "2026-10-20"), catalog))
        assertEquals(15000, reservationRevenue(r(1, "2026-10-20", price = 20000, entries = listOf(PaymentEntry("카드", 15000), PaymentEntry("적립금", 5000))), catalog))
        assertEquals(0, reservationRevenue(r(1, "2026-10-20", price = 1000, entries = listOf(PaymentEntry("할인", 3000))), catalog))
    }

    @Test fun `집계 대상 - 예약매출은 취소·노쇼·요청 빼고, 예약완료매출은 완료만, 담당자 고르면 그 담당자만`() {
        val list = listOf(null, ReservationStatus.COMPLETED, ReservationStatus.CANCELLED, ReservationStatus.NOSHOW, ReservationStatus.REQUESTED)
            .mapIndexed { i, s -> r(i, "2026-10-20", s) }
        assertEquals(listOf(0, 1), list.filter { isRevenueTarget(it, null, RevenueFilterMode.Booked) }.map { it.id })
        assertEquals(listOf(1), list.filter { isRevenueTarget(it, null, RevenueFilterMode.Completed) }.map { it.id })
        assertEquals(false, isRevenueTarget(r(9, "2026-10-20", assigneeId = 2), 1, RevenueFilterMode.Booked))
    }

    @Test fun `요약 - 일별은 최근부터, 충전은 받은 날 매출(건수엔 안 셈), 결제완료는 받은 돈만, 신규·재방문`() {
        val snap = LocalDbSnapshot(
            services = catalog,
            customers = listOf(
                Customer(1, "김", "010"),
                Customer(2, "이", "011", pointHistories = listOf(
                    // 한국 시각 10-20 09:00 = UTC 10-20 00:00
                    PointHistoryEntry("h1", "recharge", 110000, 110000, "충전", "2026-10-20T00:00:00Z", paidAmount = 100000, paymentMethod = "카드"),
                    PointHistoryEntry("h2", "recharge", 5000, 115000, "예전 충전", "2026-10-20T00:00:00Z"),
                    PointHistoryEntry("h3", "recharge", 3000, 118000, "결제수단 빈 충전", "2026-10-20T00:00:00Z", paidAmount = 3000, paymentMethod = ""),
                )),
            ),
            reservations = listOf(
                r(1, "2026-09-01", ReservationStatus.COMPLETED, customerId = 1),               // 기간 밖 첫 방문 → 고객1은 재방문
                r(2, "2026-10-19", ReservationStatus.COMPLETED, price = 30000, customerId = 1, entries = listOf(PaymentEntry("현금", 25000), PaymentEntry("할인", 5000))),
                r(3, "2026-10-20", null, customerId = 2, start = "15:00"),
                r(4, "2026-10-20", null, customerId = 2, start = "09:00"),
                r(5, "2026-10-20", ReservationStatus.CANCELLED, customerId = 1),
            ),
        )
        val s = revenueSummary(snap, RevenueRange(LocalDate.of(2026, 10, 1), today), null, RevenueFilterMode.Booked, seoul)
        assertEquals(listOf(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 19)), s.days.map { it.date })
        assertEquals(listOf(4, 3), s.days[0].reservations.map { it.id })
        assertEquals(40000 + 100000 + 3000, s.days[0].total)
        assertEquals(2, s.days[0].count)
        assertEquals(listOf(168000, 3, 103000), listOf(s.total, s.count, s.salesTotal))
        // 결제수단이 빈 충전은 매출엔 들어가도 결제완료엔 안 든다(웹과 같다)
        assertEquals(25000 + 100000, s.paidTotal)
        assertEquals(null, s.days[0].sales.last().method)
        assertEquals(1 to 1, s.newCustomerCount to s.returningCustomerCount)

        // 담당자를 고르면 충전은 빠진다
        val byAssignee = revenueSummary(snap, RevenueRange(LocalDate.of(2026, 10, 1), today), 1, RevenueFilterMode.Booked, seoul)
        assertEquals(listOf(65000, 0), listOf(byAssignee.total, byAssignee.salesTotal))
        // 예약완료매출은 완료만 — 10-19 하나
        val completed = revenueSummary(snap, RevenueRange(LocalDate.of(2026, 10, 1), today), null, RevenueFilterMode.Completed, seoul)
        assertEquals(listOf(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 19)), completed.days.map { it.date })
        assertEquals(listOf(0, 1), completed.days.map { it.count })
    }

    @Test fun `지표 상세 - 기간 안 예약은 날짜·시작 순, 신규·재방문은 고객별 기간 안 첫 예약, 이전 방문은 그 전 가장 최근`() {
        val snap = LocalDbSnapshot(
            services = catalog,
            customers = listOf(Customer(1, "김", "010"), Customer(2, "이", "011"), Customer(3, "박", "012")),
            reservations = listOf(
                r(1, "2026-09-01", customerId = 1),
                r(2, "2026-09-15", customerId = 1),
                r(3, "2026-10-05", customerId = 1, start = "15:00"),
                r(4, "2026-10-05", customerId = 2, start = "09:00", entries = listOf(PaymentEntry("카드", 20000))),
                r(5, "2026-10-07", customerId = 2),
                r(6, "2026-10-08", ReservationStatus.CANCELLED, customerId = 3),
                r(7, "2026-10-09", customerId = 1, entries = listOf(PaymentEntry("적립금", 20000))), // 웹은 금액만 본다 → 적립금만 낸 것도 결제완료
            ),
        )
        val range = RevenueRange(LocalDate.of(2026, 10, 1), today)
        val summary = revenueSummary(snap, range, null, RevenueFilterMode.Booked, seoul)
        fun detail(m: RevenueMetric) = revenueMetricDetail(snap, range, null, RevenueFilterMode.Booked, m, summary)

        assertEquals(listOf(4, 3, 5, 7), detail(RevenueMetric.Count).reservations.map { it.id })
        assertEquals("4건", detail(RevenueMetric.Count).summary)
        assertEquals("4건 · ${formatPrice(summary.total)}", detail(RevenueMetric.Sales).summary)
        val new = detail(RevenueMetric.New)
        assertEquals(listOf(2) to "1명", new.customers.map { it.customer.id } to new.summary)
        val returning = detail(RevenueMetric.Returning).customers.single()
        assertEquals(MetricCustomer(snap.customers[0], "2026-10-05", "2026-09-15"), returning)
        assertEquals(listOf(4, 7), detail(RevenueMetric.Paid).reservations.map { it.id })
        assertEquals("선택 기간 안에서 첫 예약완료가 발생한 고객 목록", metricSubtitle(RevenueMetric.New, RevenueFilterMode.Completed))
        assertNull(metricSubtitle(RevenueMetric.Sales, RevenueFilterMode.Booked))
    }

    @Test fun `방문 간격 - 일·주·달(30일)·년`() {
        assertEquals("6일전", formatVisitGap("2026-10-07", "2026-10-01"))
        assertEquals("3주전", formatVisitGap("2026-10-22", "2026-10-01"))
        // 28~29일 — 웹은 0달전으로 보였다(30으로 나눠 버림) → 1달전
        assertEquals("1달전", formatVisitGap("2026-10-29", "2026-10-01"))
        assertEquals("1달전", formatVisitGap("2026-11-15", "2026-10-01"))
        assertEquals("1년전", formatVisitGap("2027-10-01", "2026-10-01"))
    }

    @Test fun `결제완료 대상 - 받은 돈 줄, 줄 없으면 결제완료 표시, 취소·노쇼·신청 제외`() {
        assertEquals(true, isPaidTarget(r(1, "d", entries = listOf(PaymentEntry("할인", 1000))), null)) // 웹: 금액만 본다
        assertEquals(false, isPaidTarget(r(1, "d", entries = emptyList()), null))
        assertEquals(true, isPaidTarget(r(1, "d").copy(paymentCompleted = true), null))
        assertEquals(false, isPaidTarget(r(1, "d", ReservationStatus.NOSHOW).copy(paymentCompleted = true), null))
    }
}
