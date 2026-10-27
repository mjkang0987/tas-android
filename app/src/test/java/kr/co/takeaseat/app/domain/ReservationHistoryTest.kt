package kr.co.takeaseat.app.domain

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ReservationHistoryTest {
    private val labels = storeLabels("hair")
    private val assignees = listOf(Assignee(1, "민호"))
    private val r = Reservation(id = 1, date = "2026-10-19", startTime = "10:00", endTime = "11:00", service = "남성커트", customerId = 1, assigneeId = 1, status = ReservationStatus.ACTIVE, price = 18000)
    private fun entry(before: Reservation, after: Reservation) = ReservationHistoryEntry(1, before, after, "2026-10-19T06:51:00Z")

    @Test fun `상태가 바뀐 줄은 상태 한 줄만`() {
        assertEquals(listOf(ReservationDiff("상태", "예약", "취소")), historyDiffs(entry(r, r.copy(status = ReservationStatus.CANCELLED, price = 0)), assignees, labels))
        assertEquals(HistoryKind.Noshow, historyKind(entry(r, r.copy(status = ReservationStatus.NOSHOW))))
        assertEquals(listOf(ReservationDiff("상태", "노쇼", "예약")), historyDiffs(entry(r.copy(status = ReservationStatus.NOSHOW), r), assignees, labels))
        assertEquals(HistoryKind.Completed, historyKind(entry(r, r.copy(status = ReservationStatus.COMPLETED))))
    }

    @Test fun `결제 - 결제상태·결제수단 줄`() {
        val paid = r.copy(paymentCompleted = true, paymentMethod = "카드", paymentEntries = listOf(PaymentEntry("카드", 15000), PaymentEntry("적립금", 3000)))
        assertEquals(
            listOf(ReservationDiff("결제상태", "미결제", "결제완료"), ReservationDiff("결제수단", "미입력", "카드 · 15,000원, 적립금 · 3,000원")),
            historyDiffs(entry(r, paid), assignees, labels),
        )
    }

    @Test fun `칸별 변경 - 웹 차례, 담당자 이름, 경로 없던 예약은 전화예약`() {
        val after = r.copy(service = "여성커트", assigneeId = null, startTime = "10:30", price = 20000, memo = "앞머리", channel = "전화예약")
        assertEquals(
            listOf("서비스", "담당자", "시작시간", "가격", "요청사항"),
            historyDiffs(entry(r, after), assignees, labels).map { it.label },
        )
        assertEquals(ReservationDiff("담당자", "민호", "미지정"), historyDiffs(entry(r, after), assignees, labels)[1])
    }

    @Test fun `시각은 기기 시간대로`() {
        assertEquals("2026-10-19 15:51", formatHistoryTimestamp("2026-10-19T06:51:00Z", ZoneId.of("Asia/Seoul")))
        assertEquals("엉망", formatHistoryTimestamp("엉망"))
    }
}
