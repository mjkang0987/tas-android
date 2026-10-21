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
}
