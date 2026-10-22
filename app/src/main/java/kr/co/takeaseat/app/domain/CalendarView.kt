package kr.co.takeaseat.app.domain

import java.time.LocalDate

// 웹 `utils/constants.ts`(ASIDE·A11Y_DIRECTION) + `utils/weekDates.ts`(getRangeDates) + `CalendarDirection`·`CalendarHeading` 이식.

/** 캘린더 보기. [move] = ‹ › 한 번에 옮기는 날 수, [a11y] = "이전{a11y}"·"다음{a11y}" 읽기. */
enum class CalendarView(val label: String, val move: Int, val a11y: String) {
    Day("일", 1, "날짜"),
    Three("3일", 3, "3일"),
    Week("주", 7, "주"),
    Month("월", 0, "달"), // 이동은 날 수가 아니라 달 단위(moveDate)
    Year("년", 0, "년"), // 해 단위
}

/** 그 날이 든 주의 일요일 — 웹 주는 일요일부터. */
fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value % 7).toLong())

/** 웹 getRangeDates — 일은 그날, 3일은 그날부터 셋, 주는 일요일부터 일곱, 월은 격자(monthGrid). */
fun viewDates(date: LocalDate, view: CalendarView): List<LocalDate> = when (view) {
    CalendarView.Day -> listOf(date)
    CalendarView.Three -> (0L..2L).map { date.plusDays(it) }
    CalendarView.Week -> weekStart(date).let { start -> (0L..6L).map { start.plusDays(it) } }
    CalendarView.Month -> monthGrid(date)
    CalendarView.Year -> emptyList() // 연 보기는 날짜 칸이 아니라 열두 달(YearView)
}

/** 웹 MonthWrap — 그 달 1일이 든 주의 일요일부터 말일이 든 주의 토요일까지(앞뒤 달 날짜 포함, 5~6주). */
fun monthGrid(date: LocalDate): List<LocalDate> {
    val first = date.withDayOfMonth(1)
    val last = date.withDayOfMonth(date.lengthOfMonth())
    val start = weekStart(first)
    val end = weekStart(last).plusDays(6)
    return generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
}

/**
 * 웹 filterWithinTarget — 달을 옮겨도 아래 목록은 열어 둔 채 따라간다. 고른 날이 그 달이면 그대로, 아니면 그 달 1일.
 * (웹은 '오늘'·날짜 선택으로 옮겨도 같은 규칙 — 다른 달이면 1일로 간다.)
 */
fun listDateWithin(selected: LocalDate, target: LocalDate): LocalDate =
    if (selected.year == target.year && selected.month == target.month) selected else target.withDayOfMonth(1)

/** 웹 ‹ › — 주는 그 주 일요일에서 7일, 월은 다음·이전 달 1일, 나머지는 move 만큼. */
fun moveDate(date: LocalDate, view: CalendarView, forward: Boolean): LocalDate {
    if (view == CalendarView.Month) return date.withDayOfMonth(1).plusMonths(if (forward) 1 else -1)
    if (view == CalendarView.Year) return date.plusYears(if (forward) 1 else -1)
    val step = if (forward) view.move.toLong() else -view.move.toLong()
    return if (view == CalendarView.Week) weekStart(date).plusDays(step) else date.plusDays(step)
}

/**
 * 웹 CalendarHeading — 일은 "2026 / 10 / 22", 3일·주는 "2026 / 10", 달이 걸치면 "2026 / 9 - 10", 해가 걸치면 "2025 / 12 - 2026 / 1".
 * 웹은 해가 걸칠 때 앞에 연도를 한 번 더 붙인다("2025 / 2025 / 12 - …") — 앱은 한 번만.
 */
fun calendarHeading(date: LocalDate, view: CalendarView): String {
    if (view == CalendarView.Day) return "${date.year} / ${date.monthValue} / ${date.dayOfMonth}"
    if (view == CalendarView.Month) return "${date.year} / ${date.monthValue}"
    if (view == CalendarView.Year) return "${date.year}"
    val dates = viewDates(date, view)
    val start = dates.first()
    val end = dates.last()
    return when {
        start.year != end.year -> "${start.year} / ${start.monthValue} - ${end.year} / ${end.monthValue}"
        start.monthValue != end.monthValue -> "${start.year} / ${start.monthValue} - ${end.monthValue}"
        else -> "${start.year} / ${start.monthValue}"
    }
}

/** 웹 연 보기 달 목록 — 그 해 열두 달(0=1월)의 예약을 한 번에 나눈다. 달마다 날짜·시작 순. */
fun reservationsByMonth(reservations: List<Reservation>, year: Int): List<List<Reservation>> {
    val prefix = "$year-"
    val byMonth = reservations.filter { it.date.startsWith(prefix) }
        .sortedWith(compareBy({ it.date }, { it.startTime }))
        .groupBy { it.date.substring(5, 7).toIntOrNull() }
    return (1..12).map { byMonth[it].orEmpty() }
}
