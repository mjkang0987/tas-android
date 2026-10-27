package kr.co.takeaseat.app.domain

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 웹 notifications.test.ts 와 같은 경우. */
class UpcomingTest {
    private val now = LocalDateTime.of(2026, 10, 22, 13, 40)
    private fun r(id: Int, start: String, status: String? = null, date: String = "2026-10-22") =
        Reservation(id, date, start, "23:00", "커트", 1, null, status, null)

    @Test fun `1시간 안(이른 순)과 그 뒤 다음 예약 하나 - 진행 중 예약만`() {
        val list = listOf(
            r(1, "14:30"), r(2, "14:00"), r(3, "13:30"), r(4, "14:10", ReservationStatus.CANCELLED), r(5, "16:00"),
            r(6, "14:50"), r(7, "14:20", ReservationStatus.REQUESTED), r(8, "10:00", date = "2026-10-23"),
        )
        val (soon, next) = upcomingReservations(list, now)
        assertEquals(listOf(2 to 20, 1 to 50), soon.map { it.reservation.id to it.minutesUntil })
        assertEquals(6, next?.reservation?.id)
        assertEquals(70, next?.minutesUntil)
    }

    @Test fun `정각 시작은 '지금', 오늘 남은 예약이 없으면 둘 다 비어 있다`() {
        assertEquals(0, upcomingReservations(listOf(r(1, "13:40")), now).soon.first().minutesUntil)
        val empty = upcomingReservations(listOf(r(1, "09:00")), now)
        assertEquals(emptyList<UpcomingReservation>(), empty.soon)
        assertNull(empty.next)
        assertEquals(listOf("지금", "20분 후", "1시간 후", "2시간 30분 후"), listOf(0, 20, 60, 150).map(::untilLabel))
    }

    @Test fun `종 표시 - 처리할 게 있으면 숫자, 곧 시작만 있으면 점`() {
        assertEquals(BellIndicator(2, false), bellIndicator(todo = 2, soon = 3))
        assertEquals(BellIndicator(0, true), bellIndicator(todo = 0, soon = 1))
        assertEquals(BellIndicator(0, false), bellIndicator(todo = 0, soon = 0))
    }
}
