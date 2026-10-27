package kr.co.takeaseat.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomerDetailTest {
    private val kim = Customer(id = 1, name = "김민지", tel = "01012345678", memoTags = listOf(CustomerMemoTag("VIP", "#9334E6")))
    private val lee = Customer(id = 2, name = "이서연", tel = "010-2345-6789")

    @Test fun `태그 - 공백 빼고 추가, 빈 글자는 그대로, 같은 글자는 오류, 삭제`() {
        val f = CustomerEditForm.of(kim)
        assertEquals(listOf("VIP", "단골"), f.addTag(" 단골 ", "#4285F4").first.memoTags.map { it.text })
        assertEquals(f to null, f.addTag("  ", "#4285F4"))
        assertEquals("같은 메모 태그가 이미 있습니다.", f.addTag("VIP", "#4285F4").second)
        assertEquals(emptyList<CustomerMemoTag>(), f.removeTag("VIP").memoTags)
    }

    @Test fun `검사 - 이름·연락처 필수`() {
        assertEquals("고객명을 입력해 주세요.", validateCustomerEdit(CustomerEditForm(" ", "010", emptyList())))
        assertEquals("연락처를 입력해 주세요.", validateCustomerEdit(CustomerEditForm("김", "-", emptyList())))
        assertNull(validateCustomerEdit(CustomerEditForm("김", "010", emptyList())))
    }

    @Test fun `번호 중복 - 하이픈 무시, 자기 자신·빈 번호 제외`() {
        val all = listOf(kim, lee, Customer(id = 3, name = "번호없음", tel = ""))
        assertEquals(lee, findTelDuplicate(all, 1, "01023456789"))
        assertNull(findTelDuplicate(all, 2, "010-2345-6789"))
        assertNull(findTelDuplicate(all, 1, ""))
    }

    @Test fun `저장 - 이름 공백 제거, 연락처 숫자만, 태그 통째로, 노쇼 수`() {
        val snap = LocalDbSnapshot(customers = listOf(kim, lee))
        val saved = applyCustomerEdit(snap, 1, CustomerEditForm(" 김민지2 ", "010-9999-0000", emptyList())).customers.first()
        assertEquals(Customer(id = 1, name = "김민지2", tel = "01099990000", memoTags = emptyList()), saved)
        val r = Reservation(id = 1, date = "2026-10-20", startTime = "10:00", endTime = "11:00", service = "커트", customerId = 1)
        assertEquals(1, noshowCount(listOf(r.copy(status = ReservationStatus.NOSHOW), r.copy(id = 2, status = ReservationStatus.ACTIVE))))
    }
}
