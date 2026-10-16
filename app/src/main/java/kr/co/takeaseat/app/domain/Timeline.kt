package kr.co.takeaseat.app.domain

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

// 웹 `features/reservations/timeline-scale.ts` · `utils/timelineRange.ts` ·
// `components/calendar/views/timelineEntries.ts` 이식. 시간축·카드·현재시각 바가 모두 이 값을 써야
// 눈금과 카드가 어긋나지 않는다.

val TIMELINE_UNITS = intArrayOf(5, 10, 15, 20, 30, 60)
const val DEFAULT_TIMELINE_UNIT = 30

/** 카드에 이름 한 줄이라도 들어가는 최소 높이(px). 10px 글자 × 1.2 + 상하 패딩 2px. */
const val MIN_CARD_HEIGHT = 16f
private const val FULL_MIN_HEIGHT = 50f
private const val COMPACT_MIN_HEIGHT = 20f

/** full = 시작시간·고객명·시술 세 줄 / compact = 한 줄 "시작 고객명 · 시술" / name = 고객명만 */
enum class CardDetail { Full, Compact, Name }

data class TimelineScale(val unit: Int, val hourHeight: Float) {
    val minuteHeight: Float get() = hourHeight / 60f
}

/** 가장 짧은 시술 이하의 가장 큰 단위. 시술이 없으면 30분. 1~4분 시술은 5분으로 본다. */
fun resolveTimelineUnit(durations: List<Int>): Int {
    val usable = durations.filter { it > 0 }
    if (usable.isEmpty()) return DEFAULT_TIMELINE_UNIT
    val shortest = usable.min()
    return TIMELINE_UNITS.reversed().firstOrNull { it <= shortest } ?: TIMELINE_UNITS[0]
}

fun hourHeightForUnit(unit: Int): Float = when {
    unit <= 5 -> 200f
    unit <= 10 -> 120f
    else -> 100f
}

fun timelineScaleFor(services: List<ServiceItem>): TimelineScale {
    val unit = resolveTimelineUnit(services.map { it.durationMinutes })
    return TimelineScale(unit, hourHeightForUnit(unit))
}

fun cardHeightFor(minutes: Int, minuteHeight: Float): Float = max(MIN_CARD_HEIGHT, minutes * minuteHeight)

fun cardDetailForHeight(height: Float): CardDetail = when {
    height >= FULL_MIN_HEIGHT -> CardDetail.Full
    height >= COMPACT_MIN_HEIGHT -> CardDetail.Compact
    else -> CardDetail.Name
}

private fun parseHour(value: String?, fallback: Double): Double {
    if (value.isNullOrEmpty()) return fallback
    val parts = value.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return fallback
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return h + m / 60.0
}

/** 영업시간으로 시간축 범위 [start, end)(시). 시 경계로 넓히고 0~24 로 자른다. 비정상이면 최소 1시간. */
fun timelineRange(hours: StoreBusinessHours?): IntRange {
    var start = floor(parseHour(hours?.start, 10.0)).toInt().coerceAtLeast(0)
    var end = ceil(parseHour(hours?.end, 20.0)).toInt().coerceAtMost(24)
    if (end <= start) end = minOf(24, start + 1)
    return start until end
}

/** "HH:MM" → 자정부터 분 */
fun toMinutes(time: String): Int {
    val parts = time.split(":")
    return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
}

/** 겹친 예약을 좌우로 나눌 때의 칸 — index 번째 칸, 전체 count 칸. count == 1 이면 가로 전체. */
data class TimelineLane(val index: Int, val count: Int)

data class PlacedReservation(val reservation: Reservation, val lane: TimelineLane)

/**
 * 겹친 예약을 접지 않고 좌우로 나눈다(웹 모바일 동작 — `splitUpTo = MAX_SAFE_INTEGER`).
 * 겹치는 사슬을 한 묶음으로 보고, 칸은 끝난 것부터 물려준다(그리디). 10-11, 10:30-11:30, 11-12 는 3칸이 아니라 2칸.
 */
fun placeReservations(reservations: List<Reservation>): List<PlacedReservation> {
    val sorted = reservations.sortedWith(compareBy<Reservation>({ it.startTime }, { it.endTime }, { it.id }))
    val result = mutableListOf<PlacedReservation>()
    var group = mutableListOf<Reservation>()
    var groupEnd = 0

    fun flush() {
        if (group.isEmpty()) return
        val laneEnds = mutableListOf<Int>()
        val placed = group.map { r ->
            val start = toMinutes(r.startTime)
            var index = laneEnds.indexOfFirst { start >= it }
            if (index == -1) {
                index = laneEnds.size
                laneEnds += 0
            }
            laneEnds[index] = toMinutes(r.endTime)
            r to index
        }
        placed.forEach { (r, index) -> result += PlacedReservation(r, TimelineLane(index, laneEnds.size)) }
        group = mutableListOf()
    }

    for (r in sorted) {
        val start = toMinutes(r.startTime)
        if (group.isNotEmpty() && start >= groupEnd) flush()
        if (group.isEmpty()) groupEnd = toMinutes(r.endTime) else groupEnd = max(groupEnd, toMinutes(r.endTime))
        group += r
    }
    flush()
    return result
}
