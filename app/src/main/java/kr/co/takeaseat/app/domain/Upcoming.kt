package kr.co.takeaseat.app.domain

import java.time.LocalDateTime
import java.time.LocalTime

// 알림 종의 '곧 시작하는 예약' — 웹 `features/notifications/upcoming.ts` 이식. 날짜는 기기 날짜(캘린더와 같은 기준).

/** '곧 시작'으로 보여 주는 범위(분). */
const val UPCOMING_WINDOW_MINUTES = 60

/** 곧 시작 칸에 보이는 최대 건수(웹 UPCOMING_MAX_SHOWN). */
const val UPCOMING_MAX_SHOWN = 3

data class UpcomingReservation(
    val reservation: Reservation,
    /** 지금부터 시작까지 남은 분(0 = 지금 시작) */
    val minutesUntil: Int,
)

data class Upcoming(val soon: List<UpcomingReservation>, val next: UpcomingReservation?)

/** 진행 중인 예약만 — 취소·노쇼·완료는 끝났고, 확정대기는 아직 오는지 모른다(예약 알림과 같은 규칙). */
private fun isUpcomingCandidate(r: Reservation) = r.status == null || r.status == ReservationStatus.ACTIVE

/** 오늘 예약 중 지금부터 [windowMinutes] 안에 시작하는 것(이른 순)과, 그 뒤 오늘의 다음 예약 하나. */
fun upcomingReservations(reservations: List<Reservation>, now: LocalDateTime, windowMinutes: Int = UPCOMING_WINDOW_MINUTES): Upcoming {
    val today = now.toLocalDate().toString()
    val nowMinutes = now.hour * 60 + now.minute
    val later = reservations
        .filter { it.date == today && isUpcomingCandidate(it) }
        .mapNotNull { r -> runCatching { LocalTime.parse(r.startTime) }.getOrNull()?.let { r to it.hour * 60 + it.minute } }
        .filter { it.second >= nowMinutes }
        .sortedWith(compareBy({ it.second }, { it.first.id }))
        .map { (r, start) -> UpcomingReservation(r, start - nowMinutes) }
    return Upcoming(later.filter { it.minutesUntil <= windowMinutes }, later.firstOrNull { it.minutesUntil > windowMinutes })
}

/** "지금" · "20분 후" · "1시간 후" · "2시간 30분 후". */
fun untilLabel(minutes: Int): String {
    if (minutes <= 0) return "지금"
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}분 후"
        m == 0 -> "${h}시간 후"
        else -> "${h}시간 ${m}분 후"
    }
}

data class BellIndicator(val count: Int, val dot: Boolean)

/**
 * 종 표시 — 처리할 것은 빨간 숫자, 곧 시작하는 예약만 있으면 보라 점. 곧 시작은 숫자에 넣지 않는다 — 영업 중엔 늘 있어 처리할 일이 묻힌다.
 * 안드로이드는 아직 게스트 전용이라 처리할 것(예약 요청 등 로그인 기능)이 없다 — 로그인 때 [todo] 를 채운다.
 */
fun bellIndicator(todo: Int, soon: Int) = BellIndicator(todo, todo == 0 && soon > 0)
