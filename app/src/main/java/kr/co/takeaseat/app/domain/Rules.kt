package kr.co.takeaseat.app.domain

import java.time.LocalDate
import java.time.format.DateTimeParseException

// 웹 `features/assignees`·`customers`·`store-settings` 의 판정 함수 이식.

private val WEEKDAY_COUNT = 7 // 월 … 일

/** 웹 createDefaultSchedule — 평일만 근무, 10:00~20:00. 0=월 … 6=일 */
fun createDefaultSchedule(): List<DaySchedule> =
    List(WEEKDAY_COUNT) { index -> DaySchedule(enabled = index < 5, start = "10:00", end = "20:00") }

private val DEFAULT_ASSIGNEE_COLORS = listOf("#2D7FF9", "#E85D75", "#00A896", "#FB8C00", "#6D6F78", "#7E57C2")

/** 담당자 미지정 예약 카드 색(웹 `#8E8E93`). */
const val UNASSIGNED_COLOR = "#8E8E93"

fun assigneeColor(assignee: Assignee?): String = when {
    assignee == null -> UNASSIGNED_COLOR
    !assignee.color.isNullOrEmpty() -> assignee.color
    else -> DEFAULT_ASSIGNEE_COLORS[Math.abs(assignee.id) % DEFAULT_ASSIGNEE_COLORS.size]
}

fun buildAssigneeColorMap(assignees: List<Assignee>): Map<Int, String> = assignees.associate { it.id to assigneeColor(it) }

val DEFAULT_ASSIGNEES: List<Assignee> = listOf(
    Assignee(id = 1, name = "담당자 1", status = "재직", phone = "", note = "", color = DEFAULT_ASSIGNEE_COLORS[1]),
)

/**
 * 웹 syncCustomerFirstVisitDates — 고객별 첫 방문일(취소·노쇼 제외한 가장 이른 예약 날짜).
 * 웹은 저장된 firstVisitDate 를 예약이 바뀔 때마다 이 값으로 덮어쓴다. 앱은 그릴 때 계산한다.
 */
fun firstVisitDates(reservations: List<Reservation>): Map<Int, String> {
    val result = mutableMapOf<Int, String>()
    for (r in reservations) {
        if (r.status == ReservationStatus.CANCELLED || r.status == ReservationStatus.NOSHOW) continue
        val existing = result[r.customerId]
        if (existing == null || r.date < existing) result[r.customerId] = r.date
    }
    return result
}

fun isNewCustomerVisit(firstVisitDate: String?, reservationDate: String?): Boolean =
    !firstVisitDate.isNullOrEmpty() && !reservationDate.isNullOrEmpty() && firstVisitDate == reservationDate

enum class StoreClosedKind(val label: String) {
    Date("휴업일"),
    Weekday("정기휴무"),
}

/** 임시 휴업일이 정기 휴무보다 이긴다(오너가 직접 찍은 날이 더 구체적). closedWeekdays 는 0=월 … 6=일. */
fun storeClosedKind(settings: StoreSettings, dateKey: String): StoreClosedKind? {
    if (dateKey.isEmpty()) return null
    if (dateKey in settings.closedDates) return StoreClosedKind.Date
    if (settings.closedWeekdays.isEmpty()) return null
    val date = try {
        LocalDate.parse(dateKey)
    } catch (_: DateTimeParseException) {
        return null
    }
    // java DayOfWeek: 월=1 … 일=7 → 0=월 … 6=일
    return if ((date.dayOfWeek.value - 1) in settings.closedWeekdays) StoreClosedKind.Weekday else null
}

/** 웹 toDateKey 형식 "YYYY-MM-DD". */
fun LocalDate.toDateKey(): String = toString()
