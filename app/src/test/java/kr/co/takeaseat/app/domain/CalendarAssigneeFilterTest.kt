package kr.co.takeaseat.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

// 웹 Calendar·Timeline·Month·Year 의 calendarAssigneeId 거르기와 같은 경우
class CalendarAssigneeFilterTest {
    private fun r(id: Int, assigneeId: Int?) = Reservation(id, "2026-10-27", "10:00", "11:00", "커트", 1, assigneeId)
    private val list = listOf(r(1, 1), r(2, 2), r(3, null), r(4, 0))

    private fun shown(filterId: Int?) = list.filter { matchesCalendarAssignee(it, filterId) }.map { it.id }

    @Test fun `전체보기는 모두`() = assertEquals(listOf(1, 2, 3, 4), shown(null))

    @Test fun `미지정은 담당자 없음 - null 과 0`() = assertEquals(listOf(3, 4), shown(UNASSIGNED_FILTER))

    @Test fun `담당자는 그 담당자만`() = assertEquals(listOf(2), shown(2))

    @Test fun `없는 담당자는 아무것도`() = assertEquals(emptyList<Int>(), shown(99))

    @Test fun `지운 담당자를 고르고 있으면 전체보기로`() {
        val assignees = listOf(Assignee(1, "원장"))
        assertEquals(null, effectiveCalendarAssignee(99, assignees))
        assertEquals(1, effectiveCalendarAssignee(1, assignees))
        assertEquals(UNASSIGNED_FILTER, effectiveCalendarAssignee(UNASSIGNED_FILTER, assignees))
        assertEquals(null, effectiveCalendarAssignee(null, assignees))
    }
}
