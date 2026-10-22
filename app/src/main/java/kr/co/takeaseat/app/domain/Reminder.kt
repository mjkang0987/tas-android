package kr.co.takeaseat.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// 예약 알림(기기 알림) — 웹·iOS 에 없는 안드로이드 기능(지시자 2026-10-22). 무엇을 언제 알릴지만 정한다(알람·알림은 notify/).

/** 설정에서 고르는 알림 시점(분). 0 = 끔. */
val REMINDER_LEAD_OPTIONS = listOf(0, 10, 30, 60)
const val DEFAULT_REMINDER_LEAD = 30

/** 시점 표시 — "끔" · "10분 전" · "1시간 전". */
fun reminderLeadLabel(minutes: Int): String = when {
    minutes <= 0 -> "끔"
    minutes % 60 == 0 -> "${minutes / 60}시간 전"
    else -> "${minutes}분 전"
}

/** 알림 종류 — 시작 전(설정의 시점) · 지각(매장 설정의 기준, 시작 뒤). */
enum class ReminderKind { Before, Late }

/** 매장 설정의 지각 알림 기준(분). 0 = 끔. */
val LATE_ALERT_OPTIONS = listOf(0, 5, 10, 15, 20, 30)
const val DEFAULT_LATE_ALERT = 15

fun lateAlertLabel(minutes: Int): String = if (minutes <= 0) "끔" else "${minutes}분 후"

/** 매장 설정값 — 없으면 기본 15분. */
fun lateAlertMinutesOf(settings: StoreSettings): Int = settings.lateAlertMinutes ?: DEFAULT_LATE_ALERT

data class ReservationReminder(
    val reservationId: Int,
    val triggerAt: LocalDateTime,
    val title: String,
    val text: String,
    val kind: ReminderKind = ReminderKind.Before,
)

/** 알릴 예약 — 진행 중(상태 없음·active)만. 취소·노쇼·완료는 끝났고, 확정대기는 아직 오는지 모른다. */
private fun isRemindable(r: Reservation) = r.status == null || r.status == ReservationStatus.ACTIVE

private fun startOf(r: Reservation): LocalDateTime? =
    runCatching { LocalDateTime.of(LocalDate.parse(r.date), LocalTime.parse(r.startTime)) }.getOrNull()

private fun summaryOf(r: Reservation, customers: Map<Int, String>): String {
    val who = customers[r.customerId]?.takeIf { it.isNotBlank() } ?: "고객"
    val what = r.service.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
    return "${r.startTime} $who$what"
}

/** 한 예약의 시작 전 알림 — 시각을 못 읽거나 알릴 예약이 아니면 null. */
fun reminderFor(r: Reservation, customers: Map<Int, String>, leadMinutes: Int): ReservationReminder? {
    if (leadMinutes <= 0 || !isRemindable(r)) return null
    val start = startOf(r) ?: return null
    return ReservationReminder(r.id, start.minusMinutes(leadMinutes.toLong()), "예약 ${reminderLeadLabel(leadMinutes)}", summaryOf(r, customers))
}

/**
 * 한 예약의 지각 알림 — 시작 뒤 [lateMinutes] 가 지나도 진행 중(완료·노쇼·취소 처리 안 됨)이면.
 * 결제까지 끝났으면 손님은 온 것이라 알리지 않는다. 누르면 상세에서 바로 노쇼 처리할 수 있다.
 */
fun lateAlertFor(r: Reservation, customers: Map<Int, String>, lateMinutes: Int): ReservationReminder? {
    if (lateMinutes <= 0 || !isRemindable(r) || hasCompletedPayment(r)) return null
    val start = startOf(r) ?: return null
    return ReservationReminder(
        r.id,
        start.plusMinutes(lateMinutes.toLong()),
        "손님이 아직 안 오셨나요?",
        "${summaryOf(r, customers)} — 예약 ${lateMinutes}분 지남",
        ReminderKind.Late,
    )
}

/** 종류에 맞는 알림(울릴 때 다시 확인용). */
fun reminderOf(kind: ReminderKind, r: Reservation, customers: Map<Int, String>, leadMinutes: Int, lateMinutes: Int): ReservationReminder? =
    when (kind) {
        ReminderKind.Before -> reminderFor(r, customers, leadMinutes)
        ReminderKind.Late -> lateAlertFor(r, customers, lateMinutes)
    }

/**
 * 맞춰 둘 알림 — 지금 이후에 울릴 것, [horizonDays] 안, 이른 순 [limit] 건까지.
 * 기기 알람은 앱당 개수 제한(약 500)이 있어 가까운 것만 잡고, 알림이 울릴 때·데이터가 바뀔 때 다시 채운다.
 */
fun upcomingReminders(
    reservations: List<Reservation>,
    customers: Map<Int, String>,
    leadMinutes: Int,
    now: LocalDateTime,
    horizonDays: Long = 7,
    limit: Int = 100,
    lateMinutes: Int = 0,
): List<ReservationReminder> {
    if (leadMinutes <= 0 && lateMinutes <= 0) return emptyList()
    val until = now.plusDays(horizonDays)
    return reservations.asSequence()
        .flatMap { sequenceOf(reminderFor(it, customers, leadMinutes), lateAlertFor(it, customers, lateMinutes)) }
        .filterNotNull()
        .filter { it.triggerAt.isAfter(now) && !it.triggerAt.isAfter(until) }
        .sortedBy { it.triggerAt }
        .take(limit)
        .toList()
}
