package kr.co.takeaseat.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 웹 `timeline-scale.test.ts` · `timelineEntries.test.ts` 와 같은 사례.
class TimelineTest {
    @Test fun `가장 짧은 시술이 단위를 정하고 어중간한 값은 내린다`() {
        assertEquals(30, resolveTimelineUnit(listOf(30, 60, 90)))
        assertEquals(10, resolveTimelineUnit(listOf(10, 30, 60)))
        assertEquals(5, resolveTimelineUnit(listOf(7)))
        assertEquals(20, resolveTimelineUnit(listOf(25)))
        assertEquals(30, resolveTimelineUnit(listOf(45)))
        assertEquals(5, resolveTimelineUnit(listOf(1)))
        assertEquals(60, resolveTimelineUnit(listOf(90, 120)))
    }

    @Test fun `서비스가 없거나 0분이면 기본 30분, 0분은 최솟값으로 치지 않는다`() {
        assertEquals(DEFAULT_TIMELINE_UNIT, resolveTimelineUnit(emptyList()))
        assertEquals(DEFAULT_TIMELINE_UNIT, resolveTimelineUnit(listOf(0, -10)))
        // 기본 카탈로그엔 0분(기장추가)과 20분(남자 드라이)이 있다
        assertEquals(20, resolveTimelineUnit(SERVICE_CATALOG.map { it.durationMinutes }))
    }

    @Test fun `단위 한 칸이 최소 카드 높이 이상이다`() {
        for (unit in TIMELINE_UNITS) assertTrue(hourHeightForUnit(unit) / 60 * unit >= MIN_CARD_HEIGHT)
        assertEquals(100f, hourHeightForUnit(15))
        assertEquals(120f, hourHeightForUnit(10))
        assertEquals(200f, hourHeightForUnit(5))
    }

    @Test fun `카드 높이와 내용 단계`() {
        assertEquals(50f, cardHeightFor(30, 100f / 60), 0.01f)
        assertEquals(MIN_CARD_HEIGHT, cardHeightFor(5, 100f / 60))
        assertEquals(CardDetail.Full, cardDetailForHeight(50f))
        assertEquals(CardDetail.Compact, cardDetailForHeight(49f))
        assertEquals(CardDetail.Compact, cardDetailForHeight(20f))
        assertEquals(CardDetail.Name, cardDetailForHeight(19f))
        // 30분 예약은 어느 매장에서도 세 줄
        for (unit in intArrayOf(5, 10, 30)) {
            assertEquals(CardDetail.Full, cardDetailForHeight(cardHeightFor(30, hourHeightForUnit(unit) / 60)))
        }
    }

    @Test fun `시간축 범위는 영업시간을 시 경계로 넓힌다`() {
        assertEquals(10 until 20, timelineRange(StoreBusinessHours("10:00", "20:00")))
        assertEquals(9 until 21, timelineRange(StoreBusinessHours("09:30", "20:30")))
        assertEquals(10 until 20, timelineRange(null))
        assertEquals(15 until 16, timelineRange(StoreBusinessHours("15:00", "12:00"))) // 비정상이면 최소 1시간
    }

    private fun r(id: Int, start: String, end: String) =
        Reservation(id = id, date = "2026-09-16", startTime = start, endTime = end, service = "커트", customerId = id)

    @Test fun `겹치지 않으면 한 칸`() {
        val placed = placeReservations(listOf(r(1, "10:00", "11:00"), r(2, "11:00", "12:00")))
        assertEquals(listOf(TimelineLane(0, 1), TimelineLane(0, 1)), placed.map { it.lane })
    }

    @Test fun `사슬로 이어진 겹침은 끝난 칸을 물려받는다 - 3건이 2칸`() {
        val placed = placeReservations(listOf(r(1, "10:00", "11:00"), r(2, "10:30", "11:30"), r(3, "11:00", "12:00")))
        assertEquals(listOf(TimelineLane(0, 2), TimelineLane(1, 2), TimelineLane(0, 2)), placed.map { it.lane })
    }

    @Test fun `진짜 3중 겹침은 3칸, 떨어진 예약은 따로`() {
        val placed = placeReservations(
            listOf(r(3, "11:00", "12:00"), r(1, "10:00", "12:00"), r(2, "10:30", "12:00"), r(4, "14:00", "15:00")),
        )
        assertEquals(listOf(1, 2, 3, 4), placed.map { it.reservation.id })
        assertEquals(
            listOf(TimelineLane(0, 3), TimelineLane(1, 3), TimelineLane(2, 3), TimelineLane(0, 1)),
            placed.map { it.lane },
        )
    }
}
