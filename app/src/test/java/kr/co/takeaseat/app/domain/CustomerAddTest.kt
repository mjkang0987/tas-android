package kr.co.takeaseat.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomerAddTest {
    @Test fun `검사 - 이름 필수, 연락처는 비워도 되고 넣으면 휴대폰 번호`() {
        assertEquals("고객명을 입력해 주세요.", validateCustomerAdd("  ", "01012345678"))
        assertNull(validateCustomerAdd("김", ""))
        assertNull(validateCustomerAdd("김", "010-1234-5678"))
        assertEquals("올바른 연락처를 입력해 주세요.", validateCustomerAdd("김", "0212345678"))
        assertEquals("올바른 연락처를 입력해 주세요.", validateCustomerAdd("김", "0101234"))
    }

    @Test fun `추가 - 번호는 삭제 고객 포함 최댓값 + 1, 이름 공백·연락처 하이픈 정리, 적립금 0`() {
        val snap = LocalDbSnapshot(customers = listOf(Customer(id = 3, name = "a", tel = ""), Customer(id = 7, name = "삭제된 고객", tel = "", deleted = true)))
        val added = addCustomer(snap, " 김민지 ", "010-1234-5678").customers.last()
        assertEquals(Customer(id = 8, name = "김민지", tel = "01012345678", points = 0, pointHistories = emptyList()), added)
        assertEquals(1, addCustomer(LocalDbSnapshot(), "a", "").customers.single().id)
    }
}
