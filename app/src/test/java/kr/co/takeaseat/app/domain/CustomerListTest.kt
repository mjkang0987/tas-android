package kr.co.takeaseat.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerListTest {
    private val today = "2026-10-19"
    private fun r(id: Int, date: String, start: String, status: String? = ReservationStatus.ACTIVE, service: String = "커트") =
        Reservation(id = id, date = date, startTime = start, endTime = start, service = service, customerId = 1, status = status)

    @Test fun `초성 검색`() {
        assertEquals("ㄱㅁㅅ", chosungOf("김민수"))
        assertTrue(matchesChosung("김민수", "ㅁㅅ"))
        assertFalse(matchesChosung("김민수", "ㄱ민")) // 초성만으로 된 질의가 아니면 X
        assertFalse(matchesChosung("김민수", ""))
    }

    // 웹 search-highlight.test.ts 와 같은 경우
    @Test fun `검색어 구간 - 일반 부분일치 먼저, 다음 초성`() {
        assertEquals(1 until 3, findMatchRange("김민수", "민수"))
        assertNull(findMatchRange("Kim", "kim"))
        assertEquals(0 until 3, findMatchRange("Kim", "kim", caseInsensitive = true))
        assertEquals(0 until 3, findMatchRange("김민수", "ㄱㅁㅅ"))
        assertEquals(1 until 4, findMatchRange("이김민수", "ㄱㅁㅅ"))
        assertEquals(0 until 1, findMatchRange("가나다", "가"))
        assertNull(findMatchRange("김민수", "박서준"))
        assertNull(findMatchRange("김민수", "ㅂㅅㅈ"))
        assertNull(findMatchRange("김민수", ""))
        assertEquals(0 until 3, findMatchRange("김민수", "ㄱㅁㅅ", caseInsensitive = true))
        // 🙂 는 UTF-16 두 칸 — 웹과 같은 인덱스
        assertEquals(3 until 5, findMatchRange("🙂김민수", "민수"))
        assertEquals(2 until 5, findMatchRange("🙂김민수", "ㄱㅁㅅ"))
    }

    @Test fun `통계 - 지난 진행은 완료, 최근 서비스는 취소·노쇼 빼고 가장 늦은 것`() {
        val list = listOf(
            r(1, "2026-10-01", "10:00", service = "옛날"), r(2, "2026-10-25", "11:00", service = "다음주"),
            r(3, "2026-10-30", "10:00", ReservationStatus.CANCELLED, "취소된"), r(4, "2026-10-19", "09:00", ReservationStatus.NOSHOW),
            r(5, "2026-10-19", "15:00", ReservationStatus.COMPLETED, "오늘"),
        )
        val s = customerStats(list, today)
        assertEquals("다음주", s.recentService)
        assertEquals(1, s.count(CustomerReservationGroup.Booked))
        assertEquals(2, s.count(CustomerReservationGroup.Completed))
        assertEquals(1, s.count(CustomerReservationGroup.Cancelled))
        assertEquals(1, s.count(CustomerReservationGroup.Noshow))
        assertNull(customerStats(emptyList(), today).recentService)
        // 묶음: 예약·완료·취소·노쇼 차례, 안은 미래 → 과거
        val groups = groupedCustomerReservations(list, today)
        assertEquals(listOf(CustomerReservationGroup.Booked, CustomerReservationGroup.Completed, CustomerReservationGroup.Cancelled, CustomerReservationGroup.Noshow), groups.map { it.first })
        assertEquals(listOf(5, 1), groups[1].second.map { it.id })
    }

    @Test fun `정렬 - 영문 먼저, 가나다, 삭제 고객 제외`() {
        val list = listOf(Customer(1, "하늘", ""), Customer(2, "Amy", ""), Customer(3, "가람", ""), Customer(4, "삭제된 고객", "", deleted = true))
        assertEquals(listOf("Amy", "가람", "하늘"), customerListOrder(list).map { it.name })
    }

    @Test fun `검색 - 이름·초성·연락처(하이픈 무시)·메모 태그`() {
        val list = listOf(
            Customer(1, "김민지", "01012345678", memoTags = listOf(CustomerMemoTag("VIP 단골", "#9334E6"))),
            Customer(2, "Bob", "01099990000"),
        )
        assertEquals(listOf(1), searchCustomers(list, "민지").map { it.customer.id })
        assertEquals(listOf(1), searchCustomers(list, "ㄱㅁ").map { it.customer.id })
        assertEquals(listOf(2), searchCustomers(list, "9999-0000").map { it.customer.id })
        assertEquals(listOf(2), searchCustomers(list, "bob").map { it.customer.id })
        val byTag = searchCustomers(list, "vip").single()
        assertEquals("VIP 단골", byTag.matchedTags.single().text)
        assertEquals(2, searchCustomers(list, "  ").size)
    }

    @Test fun `메모 태그 - 공백 빼고 끝에 추가, 빈 글자·같은 글자는 그대로, 삭제는 같은 글자`() {
        val snap = LocalDbSnapshot(customers = listOf(Customer(id = 1, name = "김", tel = "010", memoTags = listOf(CustomerMemoTag("VIP", "#9334E6")))))
        val added = addMemoTag(snap, 1, "  단골 ", "#4285F4")
        assertEquals(listOf("VIP", "단골"), added.customers.single().memoTags!!.map { it.text })
        assertEquals("#4285F4", added.customers.single().memoTags!!.last().color)
        assertSame(snap, addMemoTag(snap, 1, "   ", "#4285F4"))
        assertSame(snap, addMemoTag(snap, 1, "VIP ", "#4285F4"))
        assertSame(snap, addMemoTag(snap, 9, "단골", "#4285F4"))
        assertEquals(listOf("단골"), removeMemoTag(added, 1, "VIP").customers.single().memoTags!!.map { it.text })
        // 태그가 없던 고객도 추가된다
        assertEquals(1, addMemoTag(snap.copy(customers = listOf(Customer(id = 1, name = "김", tel = "010"))), 1, "a", "#4285F4").customers.single().memoTags!!.size)
    }
}
