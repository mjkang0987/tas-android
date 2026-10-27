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

// ── 담당자 고르기(웹 features/assignees/model.ts) ──

fun assigneeStatus(assignee: Assignee): String = assignee.status ?: "재직"

private val koCollator: java.text.Collator = java.text.Collator.getInstance(java.util.Locale.KOREAN)

private fun assigneeNameOrder(name: String): Int = when {
    name.firstOrNull()?.let { it in 'a'..'z' || it in 'A'..'Z' } == true -> 0
    name.firstOrNull()?.let { it in '가'..'힣' || it in 'ㄱ'..'ㅣ' } == true -> 1
    else -> 2
}

/** 캘린더 담당자 필터 값 — null 전체보기, [UNASSIGNED_FILTER] 미지정, 그 밖은 담당자 id(웹 calendarAssigneeId). */
const val UNASSIGNED_FILTER = 0

/** 웹 Calendar·Timeline·Month·Year 거르기 — 미지정은 담당자 없음(null·0, 웹 `!reservation.assigneeId`). */
fun matchesCalendarAssignee(reservation: Reservation, filterId: Int?): Boolean = when (filterId) {
    null -> true
    UNASSIGNED_FILTER -> reservation.assigneeId == null || reservation.assigneeId == 0
    else -> reservation.assigneeId == filterId
}

/** 웹 compareAssigneeName — 영문 → 한글 → 그 밖, 같은 묶음은 가나다순. */
fun compareAssigneeName(a: String, b: String): Int {
    val oa = assigneeNameOrder(a)
    val ob = assigneeNameOrder(b)
    return if (oa != ob) oa - ob else koCollator.compare(a, b)
}

/** 웹 sortAssignees. */
fun sortAssignees(list: List<Assignee>): List<Assignee> = list.sortedWith { a, b -> compareAssigneeName(a.name, b.name) }

/** 새 예약에 고를 수 있는 담당자(재직), 이름순. */
fun activeAssignees(assignees: List<Assignee>): List<Assignee> = sortAssignees(assignees).filter { assigneeStatus(it) == "재직" }

/** 웹 getAssigneeAvailabilityState 의 결과. 메시지는 웹 문구 그대로. */
sealed interface AssigneeAvailability {
    data object Available : AssigneeAvailability
    data class OffDay(val message: String) : AssigneeAvailability
    data class OutsideHours(val message: String) : AssigneeAvailability
}

fun assigneeAvailability(assignees: List<Assignee>, assigneeId: Int, date: String, startTime: String, endTime: String): AssigneeAvailability {
    if (assigneeId == 0) return AssigneeAvailability.Available
    val assignee = assignees.firstOrNull { it.id == assigneeId } ?: return AssigneeAvailability.Available
    val index = try {
        LocalDate.parse(date).dayOfWeek.value - 1 // 0=월 … 6=일
    } catch (_: DateTimeParseException) {
        null
    }
    val schedule = index?.let { assignee.schedule.getOrNull(it) }
    if (schedule == null || !schedule.enabled) return AssigneeAvailability.OffDay("${assignee.name} 담당자 휴무일입니다.")
    if (startTime < schedule.start || endTime > schedule.end) {
        return AssigneeAvailability.OutsideHours("${assignee.name} 담당자 근무시간은 ${schedule.start}~${schedule.end}입니다.")
    }
    return AssigneeAvailability.Available
}

// ── 고객(웹 features/customers/model.ts) ──

fun normalizeTel(tel: String): String = tel.filter { it.isDigit() }

fun isSelectableCustomer(customer: Customer): Boolean = customer.deleted != true

/** 이름과 연락처가 **둘 다** 같은 기존 고객(삭제 고객 제외) — 연락처만 같으면 가족 공유번호일 수 있어 다른 사람으로 본다. */
fun findExactCustomer(customers: List<Customer>, name: String, tel: String): Customer? {
    val trimmed = name.trim()
    val digits = normalizeTel(tel)
    if (trimmed.isEmpty() || digits.isEmpty()) return null
    return customers.firstOrNull { isSelectableCustomer(it) && it.name.trim() == trimmed && normalizeTel(it.tel) == digits }
}

// shortcut: 웹 Intl.Collator('ko', {numeric:true}) 의 숫자 비교("고객2" < "고객10")가 없다, 숫자 든 이름 정렬이 거슬리면 숫자 묶음 비교를 붙인다.
fun sortCustomersByName(list: List<Customer>): List<Customer> =
    list.sortedWith { a, b -> koCollator.compare(a.name, b.name).takeIf { it != 0 } ?: (a.id - b.id) }

/** 웹 findOverlap — 같은 날 취소·노쇼 아닌 예약 중 시간이 겹치는 첫 예약. */
fun findOverlap(reservations: List<Reservation>, date: String, startTime: String, endTime: String, excludeId: Int? = null): Reservation? =
    reservations.firstOrNull {
        it.date == date && it.status != ReservationStatus.CANCELLED && it.status != ReservationStatus.NOSHOW &&
            it.id != excludeId && startTime < it.endTime && endTime > it.startTime
    }
