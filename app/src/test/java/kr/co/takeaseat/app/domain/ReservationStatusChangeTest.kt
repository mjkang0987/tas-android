package kr.co.takeaseat.app.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ReservationStatusChangeTest {
    private val now = Instant.parse("2026-10-19T05:00:00Z")
    private val paid = Reservation(
        id = 5, date = "2026-10-19", startTime = "10:00", endTime = "11:00", service = "남성커트", customerId = 3,
        status = ReservationStatus.ACTIVE, price = 18000,
        paymentEntries = listOf(PaymentEntry("카드", 15000), PaymentEntry("적립금", 3000)), paymentCompleted = true, pointEarned = 500,
    )
    private val snap = LocalDbSnapshot(customers = listOf(Customer(id = 3, name = "김", tel = "010", points = 1000)), reservations = listOf(paid))

    @Test fun `하단 버튼 - 미결제 진행은 취소·노쇼, 결제했으면 없음, 취소·노쇼는 예약전환, 확정대기는 없음`() {
        val unpaid = paid.copy(paymentEntries = null, paymentCompleted = null)
        assertEquals(listOf(DetailAction.Cancel, DetailAction.Noshow), detailActions(unpaid))
        assertEquals(emptyList<DetailAction>(), detailActions(paid))
        assertEquals(listOf(DetailAction.Restore), detailActions(paid.copy(status = ReservationStatus.NOSHOW)))
        assertEquals(listOf(DetailAction.Restore), detailActions(paid.copy(status = ReservationStatus.CANCELLED)))
        assertEquals(emptyList<DetailAction>(), detailActions(unpaid.copy(status = ReservationStatus.REQUESTED)))
        assertEquals(listOf(DetailAction.Cancel, DetailAction.Noshow), detailActions(unpaid.copy(status = null))) // 옛 데이터 = 진행
    }

    @Test fun `취소 - 결제 비우고, 쓴 적립금 돌려주고 준 적립은 회수, 이력 남김`() {
        val next = changeReservationStatus(snap, 5, ReservationStatus.CANCELLED, now)
        val r = next.reservations.single()
        assertEquals(ReservationStatus.CANCELLED, r.status)
        assertEquals(emptyList<PaymentEntry>(), r.paymentEntries)
        assertEquals(false, r.paymentCompleted)
        assertEquals(0, r.pointEarned)
        val c = next.customers.single()
        assertEquals(3500, c.points) // 1000 + 3000 - 500
        assertEquals(2500, c.pointHistories!!.single().delta)
        assertEquals("예약 취소 — 결제에 쓴 적립금 반환", c.pointHistories!!.single().description)
        assertEquals(paid, next.history.single().before)
        assertEquals("2026-10-19T05:00:00Z", next.history.single().timestamp)
        // 다시 취소해도 또 돌려주지 않는다
        assertEquals(3500, changeReservationStatus(next, 5, ReservationStatus.CANCELLED, now).customers.single().points)
    }

    @Test fun `노쇼 - 상태만, 결제·적립금 그대로`() {
        val next = changeReservationStatus(snap, 5, ReservationStatus.NOSHOW, now)
        assertEquals(paid.copy(status = ReservationStatus.NOSHOW), next.reservations.single())
        assertEquals(1000, next.customers.single().points)
        assertNull(next.customers.single().pointHistories)
    }

    @Test fun `적립금 회수가 잔액보다 크면 0 에서 멈춘다, 삭제 고객은 건드리지 않는다`() {
        val earnedOnly = paid.copy(paymentEntries = listOf(PaymentEntry("카드", 18000)), pointEarned = 2000)
        val low = snap.copy(customers = listOf(snap.customers.single().copy(points = 500)), reservations = listOf(earnedOnly))
        val c = changeReservationStatus(low, 5, ReservationStatus.CANCELLED, now).customers.single()
        assertEquals(0, c.points)
        assertEquals(-1500, c.pointHistories!!.single().balance)
        assertEquals("예약 취소 — 결제 적립금 회수", c.pointHistories!!.single().description)
        val deleted = snap.copy(customers = listOf(snap.customers.single().copy(deleted = true)))
        assertEquals(1000, changeReservationStatus(deleted, 5, ReservationStatus.CANCELLED, now).customers.single().points)
    }

    @Test fun `예약전환 - 진행으로, 이력 남김, 없는 번호는 그대로`() {
        val cancelled = changeReservationStatus(snap, 5, ReservationStatus.CANCELLED, now)
        val restored = restoreReservation(cancelled, 5, now)
        assertEquals(ReservationStatus.ACTIVE, restored.reservations.single().status)
        assertEquals(2, restored.history.size)
        assertSame(snap, restoreReservation(snap, 99, now))
    }
}
