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

    @Test fun `가격이 없으면 서비스 가격 합, 모르는 서비스는 0`() {
        val catalog = listOf(ServiceItem("커트", 30, "커트", 20000), ServiceItem("다운펌+커트", 60, "펌", 50000), ServiceItem("크리닉", 30, "크리닉", 30000))
        assertEquals(50000, servicePriceTotal("커트+크리닉", catalog))
        assertEquals(80000, servicePriceTotal("다운펌+커트+크리닉", catalog)) // '+' 가 든 이름은 통째로
        assertEquals(20000, servicePriceTotal("커트+없는것", catalog))
    }

    @Test fun `배지 색 - 모르는 종류는 웹 기본 회색, 연락처에 이미 하이픈이 있어도 같은 꼴`() {
        assertEquals(BadgeColors("#F1F1F1", "#999999"), statusBadgeColors("없는종류"))
        assertEquals("#6526D914", statusBadgeColors("paid").bg)
        assertEquals("010-1234-5678", formatTel("010-1234-5678"))
    }

    @Test fun `네이버 예약금 줄 - 네이버 예약이고 예약금이 결제 줄 합과 다를 때만`() {
        val base = Reservation(1, "2026-10-19", "10:00", "11:00", "커트", customerId = 1, naverBookingId = "N1", naverDeposit = 10000)
        assertTrue(showsNaverDepositLine(base, emptyList()))
        assertFalse(showsNaverDepositLine(base, listOf(PaymentEntry("네이버 예약금", 10000))))
        assertTrue(showsNaverDepositLine(base, listOf(PaymentEntry("네이버 예약금", 5000))))
        assertFalse(showsNaverDepositLine(base.copy(naverBookingId = ""), emptyList())) // 빈 번호는 네이버 예약 아님(웹)
        assertFalse(showsNaverDepositLine(base.copy(naverDeposit = 0), emptyList()))
    }
}
