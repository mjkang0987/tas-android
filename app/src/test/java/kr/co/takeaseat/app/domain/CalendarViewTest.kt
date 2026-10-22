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
        // 토요일은 그 주의 끝 — 다음 주로 넘어가지 않는다
        assertEquals(LocalDate.of(2026, 10, 18), weekStart(LocalDate.of(2026, 10, 24)))
    }

    @Test fun `이동 - 일 1·3일 3, 주는 그 주 일요일에서 7일`() {
        assertEquals(thu.minusDays(1), moveDate(thu, CalendarView.Day, forward = false))
        assertEquals(thu.plusDays(3), moveDate(thu, CalendarView.Three, forward = true))
        assertEquals(thu.minusDays(3), moveDate(thu, CalendarView.Three, forward = false))
        assertEquals(LocalDate.of(2026, 10, 25), moveDate(thu, CalendarView.Week, forward = true))
        assertEquals(LocalDate.of(2026, 10, 11), moveDate(thu, CalendarView.Week, forward = false))
    }

    @Test fun `제목 - 일은 연월일, 걸친 달·해는 범위로`() {
        assertEquals("2026 / 10 / 22", calendarHeading(thu, CalendarView.Day))
        assertEquals("2026 / 10", calendarHeading(thu, CalendarView.Week))
        assertEquals("2026 / 10", calendarHeading(thu, CalendarView.Three))
        assertEquals("2026 / 10 - 11", calendarHeading(LocalDate.of(2026, 10, 30), CalendarView.Three))
        assertEquals("2026 / 9 - 10", calendarHeading(LocalDate.of(2026, 10, 1), CalendarView.Week))
        assertEquals("2025 / 12 - 2026 / 1", calendarHeading(LocalDate.of(2026, 1, 1), CalendarView.Week))
    }

    @Test fun `월 격자 - 1일이 든 주 일요일부터 말일이 든 주 토요일까지`() {
        val oct = monthGrid(thu)
        assertEquals(LocalDate.of(2026, 9, 27), oct.first())
        assertEquals(LocalDate.of(2026, 10, 31), oct.last())
        assertEquals(35, oct.size)
        // 2026-02 는 일요일에 시작해 토요일에 끝난다 — 앞뒤 달 칸이 없다
        assertEquals(28, monthGrid(LocalDate.of(2026, 2, 10)).size)
        // 6주짜리 달: 2026-08 은 토요일 시작
        assertEquals(42, monthGrid(LocalDate.of(2026, 8, 1)).size)
    }

    @Test fun `월 이동·제목·목록 날짜`() {
        assertEquals(LocalDate.of(2026, 11, 1), moveDate(thu, CalendarView.Month, forward = true))
        assertEquals(LocalDate.of(2026, 9, 1), moveDate(LocalDate.of(2026, 10, 31), CalendarView.Month, forward = false))
        assertEquals(LocalDate.of(2027, 1, 1), moveDate(LocalDate.of(2026, 12, 31), CalendarView.Month, forward = true))
        assertEquals("2026 / 10", calendarHeading(thu, CalendarView.Month))
        // 같은 달이면 고른 날 그대로, 다른 달로 가면 그 달 1일(웹 filterWithinTarget)
        assertEquals(thu, listDateWithin(thu, LocalDate.of(2026, 10, 1)))
        assertEquals(LocalDate.of(2026, 11, 1), listDateWithin(thu, LocalDate.of(2026, 11, 1)))
        assertEquals(LocalDate.of(2027, 10, 1), listDateWithin(thu, LocalDate.of(2027, 10, 22)))
    }
}
