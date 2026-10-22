package kr.co.takeaseat.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarViewTest {
    private val thu = LocalDate.of(2026, 10, 22) // 목요일

    @Test fun `칸 날짜 - 일은 그날, 3일은 그날부터 셋, 주는 일요일부터 일곱`() {
        assertEquals(listOf(thu), viewDates(thu, CalendarView.Day))
        assertEquals(listOf(22, 23, 24), viewDates(thu, CalendarView.Three).map { it.dayOfMonth })
        assertEquals((18..24).toList(), viewDates(thu, CalendarView.Week).map { it.dayOfMonth })
        // 일요일 자신은 그 주의 시작
        assertEquals(LocalDate.of(2026, 10, 18), weekStart(LocalDate.of(2026, 10, 18)))
    }

    @Test fun `이동 - 일 1·3일 3, 주는 그 주 일요일에서 7일`() {
        assertEquals(thu.minusDays(1), moveDate(thu, CalendarView.Day, forward = false))
        assertEquals(thu.plusDays(3), moveDate(thu, CalendarView.Three, forward = true))
        assertEquals(LocalDate.of(2026, 10, 25), moveDate(thu, CalendarView.Week, forward = true))
        assertEquals(LocalDate.of(2026, 10, 11), moveDate(thu, CalendarView.Week, forward = false))
    }

    @Test fun `제목 - 일은 연월일, 걸친 달·해는 범위로`() {
        assertEquals("2026 / 10 / 22", calendarHeading(thu, CalendarView.Day))
        assertEquals("2026 / 10", calendarHeading(thu, CalendarView.Week))
        assertEquals("2026 / 10 - 11", calendarHeading(LocalDate.of(2026, 10, 30), CalendarView.Three))
        assertEquals("2026 / 9 - 10", calendarHeading(LocalDate.of(2026, 10, 1), CalendarView.Week))
        assertEquals("2025 / 12 - 2026 / 1", calendarHeading(LocalDate.of(2026, 1, 1), CalendarView.Week))
    }
}
