package kr.co.takeaseat.app.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ReservationPaymentTest {
    private val now = Instant.parse("2026-10-19T05:00:00Z")
    private val labels = storeLabels("hair")
    private val off = PointSettings()
    private val rate10 = PointSettings(enableServiceRate = true, serviceRate = 10.0)
    private val r = Reservation(id = 5, date = "2026-10-19", startTime = "10:00", endTime = "11:00", service = "남성커트", customerId = 3, price = 20000, status = ReservationStatus.ACTIVE)
    private val snap = LocalDbSnapshot(customers = listOf(Customer(id = 3, name = "김", tel = "010", points = 5000)), reservations = listOf(r))
    private fun form(vararg e: Pair<String, String>, award: PointAward = PointAward(false, "0")) =
        PaymentForm(e.map { PaymentDraft(it.first, it.second) }, award)

    @Test fun `열 때 기본 줄 - 저장된 줄, 네이버 예약금 + 나머지, 전액 한 줄`() {
        assertEquals(listOf(PaymentDraft("", "20000")), paymentDrafts(r, 20000))
        assertEquals(listOf(PaymentDraft("네이버 예약금", "5000"), PaymentDraft("", "15000")), paymentDrafts(r.copy(naverBookingId = "1", naverDeposit = 5000), 20000))
        assertEquals(listOf(PaymentDraft("카드", "20000")), paymentDrafts(r.copy(paymentEntries = listOf(PaymentEntry("카드", 20000)), paymentCompleted = true), 20000))
        // 적립: 매장 적립률 켜져 있으면 켜고 가격 × 률
        assertEquals(PointAward(true, "2000"), PaymentForm.open(r, 20000, rate10).pointAward)
        assertEquals(PointAward(false, "0"), PaymentForm.open(r, 20000, off).pointAward)
    }

    @Test fun `줄 바꾸면 받은 돈 기준으로 적립 다시 - 적립금·할인은 빼고, 직접 고쳤으면 그대로`() {
        val f = PaymentForm.open(r, 20000, rate10).withEntries(listOf(PaymentDraft("카드", "15000"), PaymentDraft("적립금", "5000")), rate10)
        assertEquals("1500", f.pointAward.amount)
        val manual = f.copy(isPointAwardManual = true).withEntries(listOf(PaymentDraft("카드", "20000")), rate10)
        assertEquals("1500", manual.pointAward.amount)
        assertEquals(listOf(PaymentDraft()), f.removeEntry(0, rate10).removeEntry(0, rate10).entries)
    }

    @Test fun `검사 문구·차례`() {
        assertEquals("결제 종류를 선택하지 않은 금액(20,000원)이 있습니다. 결제 종류를 선택해 주세요.", validatePayment(form("" to "20000"), r, 20000, 5000, labels))
        assertEquals("결제 종류를 선택하지 않은 금액(5,000원)이 있습니다. 결제 종류를 선택하거나 그 줄을 삭제해 주세요.", validatePayment(form("카드" to "15000", "" to "5000"), r, 20000, 5000, labels))
        assertEquals("결제 종류와 금액을 입력해 주세요.", validatePayment(form("" to ""), r, 20000, 5000, labels))
        assertEquals("결제 금액이 5,000원 부족합니다. (입력 15,000원 / 서비스 금액 20,000원)", validatePayment(form("카드" to "15000"), r, 20000, 5000, labels))
        assertEquals("결제 금액이 1,000원 많습니다. (입력 21,000원 / 서비스 금액 20,000원)", validatePayment(form("카드" to "21000"), r, 20000, 5000, labels))
        assertEquals("고객 적립금이 부족합니다.", validatePayment(form("카드" to "14000", "적립금" to "6000"), r, 20000, 5000, labels))
        assertNull(validatePayment(form("카드" to "15000", "적립금" to "5000"), r, 20000, 5000, labels))
    }

    @Test fun `네이버 예약금 - 줄이 있으면 전체 금액, 없으면 실결제, 예약금 금액이 다르면 막는다`() {
        val naver = r.copy(naverBookingId = "1", naverDeposit = 5000)
        assertNull(validatePayment(form("네이버 예약금" to "5000", "카드" to "15000"), naver, 20000, 0, labels))
        assertNull(validatePayment(form("카드" to "15000"), naver, 20000, 0, labels))
        assertEquals("결제 금액이 5,000원 많습니다. (입력 20,000원 / 실결제 금액 15,000원)", validatePayment(form("카드" to "20000"), naver, 20000, 0, labels))
        assertEquals("네이버 예약금은 5,000원입니다.", validatePayment(form("네이버 예약금" to "4000", "카드" to "16000"), naver, 20000, 0, labels))
    }

    @Test fun `저장 - 결제완료·줄·적립, 고객 적립금은 쓴 만큼 빼고 적립 더함, 이력 두 줄`() {
        val next = applyPayment(snap, 5, form("카드" to "15000", "적립금" to "5000", award = PointAward(true, "1500")), now)
        val saved = next.reservations.single()
        assertEquals(true, saved.paymentCompleted)
        assertEquals("카드", saved.paymentMethod)
        assertEquals(listOf(PaymentEntry("카드", 15000), PaymentEntry("적립금", 5000)), saved.paymentEntries)
        assertEquals(1500, saved.pointEarned)
        val c = next.customers.single()
        assertEquals(1500, c.points) // 5000 - 5000 + 1500
        assertEquals(listOf("payment_use" to -5000, "payment_earn" to 1500), c.pointHistories!!.map { it.type to it.delta })
        assertEquals(listOf(0, 1500), c.pointHistories!!.map { it.balance })
        assertEquals(1, next.history.size)

        // 결제수단 변경 — 적립금을 덜 쓰면 그만큼 돌려주고, 적립을 줄이면 '조정'
        val again = applyPayment(next, 5, form("카드" to "20000", award = PointAward(true, "1000")), now)
        val c2 = again.customers.single()
        assertEquals(6000, c2.points) // 1500 + 5000 - 500
        assertEquals(listOf("payment_use" to 5000, "payment_adjust" to -500), c2.pointHistories!!.drop(2).map { it.type to it.delta })
    }

    @Test fun `예약완료 - 결제한 예약만`() {
        assertSame(snap, completeReservation(snap, 5, now))
        val paid = applyPayment(snap, 5, form("카드" to "20000"), now)
        assertEquals(ReservationStatus.COMPLETED, completeReservation(paid, 5, now).reservations.single().status)
    }
}
