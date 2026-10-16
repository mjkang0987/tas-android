package kr.co.takeaseat.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
    // 기대값은 웹 `buildServiceColorMap()`·`getCategoryBaseColor()` 를 그대로 돌려 받은 값이다.
    @Test fun `서비스 색이 웹과 같다`() {
        val map = buildServiceColorMap()
        assertEquals("#2d7ff9", map["남성커트"])
        assertEquals("#3b8dff", map["여성커트"])
        assertEquals("#1f71eb", map["주니어커트"])
        assertEquals("#ff5d59", map["볼륨매직"]) // 펌 6번째 → 밝기 +36
        assertEquals("#9c9ca1", map["숱추가"])
        assertEquals("#f34743", map["다운펌+커트"]) // 옛 이름은 현재 이름 색
        assertEquals("#c430cf", categoryBaseColor("네일"))
        assertEquals("#cf3036", categoryBaseColor("Nail"))
        assertEquals("#3b8dff", serviceColor("여성커트 롱", map)) // 부분 일치
        assertEquals(FALLBACK_SERVICE_COLOR, serviceColor("없는시술", map))
    }

    @Test fun `서비스 문자열 분해는 '+' 가 든 이름을 살린다`() {
        assertEquals(listOf("커트", "크리닉"), parseServiceString("커트 + 크리닉"))
        assertEquals(listOf("다운펌+커트", "크리닉"), parseServiceString("다운펌+커트+크리닉", setOf("다운펌+커트", "크리닉")))
        assertEquals(emptyList<String>(), parseServiceString("  "))
    }

    @Test fun `담당자 색은 지정 색, 없으면 id 순환, 없는 담당자는 회색`() {
        assertEquals("#E85D75", assigneeColor(Assignee(id = 1, name = "a")))
        assertEquals("#2D7FF9", assigneeColor(Assignee(id = 6, name = "a")))
        assertEquals("#123456", assigneeColor(Assignee(id = 1, name = "a", color = "#123456")))
        assertEquals(UNASSIGNED_COLOR, assigneeColor(null))
    }

    @Test fun `첫 방문일은 취소·노쇼를 뺀 가장 이른 날`() {
        val list = listOf(
            Reservation(1, "2026-10-01", "10:00", "11:00", "커트", customerId = 7, status = ReservationStatus.CANCELLED),
            Reservation(2, "2026-10-03", "10:00", "11:00", "커트", customerId = 7),
            Reservation(3, "2026-10-05", "10:00", "11:00", "커트", customerId = 7),
        )
        val first = firstVisitDates(list)
        assertEquals("2026-10-03", first[7])
        assertTrue(isNewCustomerVisit(first[7], "2026-10-03"))
        assertFalse(isNewCustomerVisit(first[7], "2026-10-05"))
        assertFalse(isNewCustomerVisit(null, "2026-10-05"))
    }

    @Test fun `휴무 판정 - 휴업일이 정기휴무보다 이긴다`() {
        // 2026-10-17 은 토요일 → 5
        val settings = StoreSettings(closedDates = listOf("2026-10-17"), closedWeekdays = listOf(5))
        assertEquals(StoreClosedKind.Date, storeClosedKind(settings, "2026-10-17"))
        assertEquals(StoreClosedKind.Weekday, storeClosedKind(settings, "2026-10-24"))
        assertNull(storeClosedKind(settings, "2026-10-19"))
        assertNull(storeClosedKind(StoreSettings(closedWeekdays = listOf(5)), "망가진값"))
    }
}
