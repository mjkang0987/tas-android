package kr.co.takeaseat.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailRulesTest {
    @Test fun `금액·연락처 표시가 웹과 같다`() {
        assertEquals("1,000,000원", formatPrice(1_000_000))
        assertEquals("0원", formatPrice(0))
        assertEquals("010-1234-5678", formatTel("01012345678"))
        assertEquals("031-123-4567", formatTel("0311234567"))
        assertEquals("1588", formatTel("1588"))
    }

    @Test fun `업종 표시어 - 첫 번째로 아는 업종, 모르면 기타`() {
        assertEquals(StoreLabels("담당자", "서비스"), storeLabels("hair"))
        assertEquals(StoreLabels("테이블", "메뉴"), storeLabels("없는업종, cafe, gym"))
        assertEquals(StoreLabels("담당자", "서비스"), storeLabels(null))
    }

    private fun r(entries: List<PaymentEntry>? = null, completed: Boolean? = null, method: String? = null) = Reservation(
        id = 1, date = "2026-10-19", startTime = "10:00", endTime = "11:00", service = "커트", customerId = 1,
        price = 20000, paymentEntries = entries, paymentCompleted = completed, paymentMethod = method,
    )

    @Test fun `결제 줄 - 줄이 있으면 그대로, 옛 데이터는 결제수단 + 가격 한 줄`() {
        assertEquals(listOf(PaymentEntry("카드", 15000)), paymentEntriesOf(r(entries = listOf(PaymentEntry("카드", 15000)))))
        assertEquals(listOf(PaymentEntry("현금", 20000)), paymentEntriesOf(r(completed = true, method = "현금")))
        assertTrue(paymentEntriesOf(r(completed = true)).isEmpty())
        assertTrue(hasCompletedPayment(r(completed = true)))
        assertFalse(hasCompletedPayment(r()))
    }
}
