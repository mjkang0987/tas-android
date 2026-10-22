package kr.co.takeaseat.app.domain

import java.time.LocalDate

// 웹 `utils/constants.ts`(ASIDE·A11Y_DIRECTION) + `utils/weekDates.ts`(getRangeDates) + `CalendarDirection`·`CalendarHeading` 이식.

/** 캘린더 보기. [move] = ‹ › 한 번에 옮기는 날 수, [a11y] = "이전{a11y}"·"다음{a11y}" 읽기. */
enum class CalendarView(val label: String, val move: Int, val a11y: String) {
    Day("일", 1, "날짜"),
    Three("3일", 3, "3일"),
    Week("주", 7, "주"),
}

/** 그 날이 든 주의 일요일 — 웹 주는 일요일부터. */
fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value % 7).toLong())

/** 웹 getRangeDates — 일은 그날, 3일은 그날부터 셋, 주는 일요일부터 일곱. */
fun viewDates(date: LocalDate, view: CalendarView): List<LocalDate> = when (view) {
    CalendarView.Day -> listOf(date)
    CalendarView.Three -> (0L..2L).map { date.plusDays(it) }
    CalendarView.Week -> weekStart(date).let { start -> (0L..6L).map { start.plusDays(it) } }
}

/** 웹 ‹ › — 주는 그 주 일요일에서 7일, 나머지는 move 만큼. */
fun moveDate(date: LocalDate, view: CalendarView, forward: Boolean): LocalDate {
    val step = if (forward) view.move.toLong() else -view.move.toLong()
    return if (view == CalendarView.Week) weekStart(date).plusDays(step) else date.plusDays(step)
}

/**
 * 웹 CalendarHeading — 일은 "2026 / 10 / 22", 3일·주는 "2026 / 10", 달이 걸치면 "2026 / 9 - 10", 해가 걸치면 "2025 / 12 - 2026 / 1".
 * 웹은 해가 걸칠 때 앞에 연도를 한 번 더 붙인다("2025 / 2025 / 12 - …") — 앱은 한 번만.
 */
fun calendarHeading(date: LocalDate, view: CalendarView): String {
    if (view == CalendarView.Day) return "${date.year} / ${date.monthValue} / ${date.dayOfMonth}"
    val dates = viewDates(date, view)
    val start = dates.first()
    val end = dates.last()
    return when {
        start.year != end.year -> "${start.year} / ${start.monthValue} - ${end.year} / ${end.monthValue}"
        start.monthValue != end.monthValue -> "${start.year} / ${start.monthValue} - ${end.monthValue}"
        else -> "${start.year} / ${start.monthValue}"
    }
}
