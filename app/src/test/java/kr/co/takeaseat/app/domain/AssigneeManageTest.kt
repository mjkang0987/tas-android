package kr.co.takeaseat.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AssigneeManageTest {
    private val a1 = Assignee(id = 1, name = "지수", status = "재직")
    private val a5 = Assignee(id = 5, name = "민호", status = "휴직", color = "#123456")
    private val snap = LocalDbSnapshot(
        assignees = listOf(a1, a5),
        reservations = listOf(Reservation(1, "2026-10-22", "10:00", "11:00", "커트", 1, assigneeId = 5), Reservation(2, "2026-10-22", "10:00", "11:00", "커트", 1, assigneeId = 1)),
    )

    @Test fun `추가 - 이름 비면 그대로, 번호는 가장 큰 번호 + 1, 색 안 고르면 그 번호 기본 색, 근무는 평일 기본`() {
        assertSame(snap, addAssignee(snap, "  ", "재직", "", "", null))
        val added = addAssignee(snap, " 하늘 ", "휴직", " 010 ", " 메모 ", null).assignees.last()
        assertEquals(listOf<Any?>(6, "하늘", "휴직", "010", "메모"), listOf(added.id, added.name, added.status, added.phone, added.note))
        assertEquals(assigneeColor(Assignee(id = 6, name = "")), added.color)
        assertEquals(listOf(true, true, true, true, true, false, false), added.schedule.map { it.enabled })
        assertEquals("#ABCDEF", addAssignee(snap, "x", "재직", "", "", "#ABCDEF").assignees.last().color)
    }

    @Test fun `고치기·요일 - 그 담당자만, 요일 범위 밖은 그대로`() {
        val renamed = updateAssignee(snap, 5) { it.copy(name = "민호2") }
        assertEquals(listOf("지수", "민호2"), renamed.assignees.map { it.name })
        val off = updateAssigneeDay(snap, 1, 0) { it.copy(enabled = false) }
        assertEquals(false, off.assignees[0].schedule[0].enabled)
        assertSame(snap, updateAssigneeDay(snap, 1, 7) { it.copy(enabled = false) })
    }

    @Test fun `영구 삭제 - 담당자는 빠지고 그 예약은 미지정으로 남는다`() {
        val deleted = deleteAssignee(snap, 5)
        assertEquals(listOf(1), deleted.assignees.map { it.id })
        assertEquals(listOf(null, 1), deleted.reservations.map { it.assigneeId })
    }

    @Test fun `다국어 이름 - 빈 칸은 빼고 다 비면 null`() {
        val en = withNameI18n(null, "en", "Jisu")
        assertEquals(NameI18n(en = "Jisu"), en)
        assertNull(withNameI18n(en, "en", " "))
        assertEquals(NameI18n(en = "Jisu", zh = "智秀"), withNameI18n(en, "zh", "智秀"))
    }

    @Test fun `근무 요약 - 이웃한 같은 근무끼리, 휴무는 휴무`() {
        val day = { on: Boolean, end: String -> DaySchedule(on, "10:00", end) }
        val schedule = listOf(day(true, "20:00"), day(true, "20:00"), day(true, "20:00"), day(true, "20:00"), day(true, "20:00"), day(true, "18:00"), day(false, "20:00"))
        assertEquals(listOf("월~금" to "10:00~20:00", "토" to "10:00~18:00", "일" to "휴무"), summarizeSchedule(schedule))
        assertEquals(listOf("월~일" to "휴무"), summarizeSchedule(List(7) { day(false, "20:00") }))
        val (active, leave, resigned) = splitAssigneesByStatus(listOf(a5, a1, Assignee(id = 9, name = "x", status = "퇴직")))
        assertEquals(listOf(listOf(1), listOf(5), listOf(9)), listOf(active, leave, resigned).map { l -> l.map { it.id } })
    }
}
