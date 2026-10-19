package kr.co.takeaseat.app.domain

import java.time.Instant
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservationEditTest {
    private val catalog = buildCatalogMap(SERVICE_CATALOG)
    private val known = buildServiceColorMap().keys
    private val labels = storeLabels("hair")
    private val original = Reservation(
        id = 5, date = "2026-10-19", startTime = "10:00", endTime = "10:30", service = "남성커트", customerId = 3,
        assigneeId = 1, status = ReservationStatus.ACTIVE, price = 18000, channel = "전화예약",
    )
    private val snap = LocalDbSnapshot(
        customers = listOf(Customer(id = 3, name = "김", tel = "010"), Customer(id = 4, name = "이", tel = "011")),
        reservations = listOf(original),
        assignees = DEFAULT_ASSIGNEES,
    )
    private fun form() = ReservationForm.fromReservation(original, known)

    @Test fun `예약을 폼으로 - 그대로면 바뀐 칸 없음`() {
        val f = form()
        assertEquals(listOf("남성커트"), f.selectedServices)
        assertEquals(18000, f.price)
        assertEquals(emptyList<ReservationDiff>(), changedFields(original, f, snap.assignees, labels, known))
        // 가격이 없던 옛 예약은 카탈로그 합, 경로가 없으면 전화예약
        val legacy = ReservationForm.fromReservation(original.copy(price = null, channel = null, service = "남자 일반펌"), known)
        assertEquals(50000, legacy.price)
        assertEquals("전화예약", legacy.channel)
        // 경로 없던 예약을 그대로 두면 변경 없음, 바꾸면 '전화예약 → 현장방문'
        val noChannel = original.copy(channel = null)
        val nf = ReservationForm.fromReservation(noChannel, known)
        assertTrue(changedFields(noChannel, nf, snap.assignees, labels, known).isEmpty())
        assertEquals(ReservationDiff("예약경로", "전화예약", "현장방문"), changedFields(noChannel, nf.copy(channel = "현장방문"), snap.assignees, labels, known).single())
    }

    @Test fun `바뀐 칸 - 웹 차례·표기`() {
        val f = form().toggleService("크리닉", catalog).copy(channel = "현장방문", memo = "앞머리", assigneeId = 0)
        val diffs = changedFields(original, f, snap.assignees, labels, known)
        assertEquals(listOf("서비스", "담당자", "종료시간", "가격", "요청사항", "예약경로"), diffs.map { it.label })
        assertEquals(ReservationDiff("서비스", "남성커트", "남성커트+크리닉"), diffs[0])
        assertEquals(ReservationDiff("담당자", "담당자 1", "미지정"), diffs[1])
        assertEquals(ReservationDiff("가격", "18,000원", "48,000원"), diffs[3])
    }

    @Test fun `서비스를 안 건드리면 원래 문자열 그대로(띄어쓰기 달라도 변경 아님)`() {
        val spaced = original.copy(service = "남성커트 + 크리닉")
        val f = ReservationForm.fromReservation(spaced, known)
        assertEquals("남성커트 + 크리닉", draftReservation(spaced, f, known).service)
        assertTrue(changedFields(spaced, f, snap.assignees, labels, known).isEmpty())
    }

    @Test fun `검사 - 자기 자신과는 안 겹치고 다른 예약과는 겹친다`() {
        assertNull(validateEdit(original, form(), snap, labels))
        val other = Reservation(id = 9, date = "2026-10-19", startTime = "11:00", endTime = "12:00", service = "", customerId = 4)
        val withOther = snap.copy(reservations = snap.reservations + other)
        assertEquals("이 예약(11:00~12:00)과 시간이 겹칩니다.", validateEdit(original, form().changeStartTime("11:00", catalog), withOther, labels)?.message)
        assertEquals("서비스를 선택해주세요.", validateEdit(original, form().toggleService("남성커트", catalog), snap, labels)?.message)
    }

    @Test fun `과거 판정`() {
        assertTrue(isPastStart(form(), LocalDateTime.of(2026, 10, 19, 10, 1)))
        assertFalse(isPastStart(form(), LocalDateTime.of(2026, 10, 19, 9, 59)))
    }

    @Test fun `저장 - 예약경로까지 저장, 결제 줄 하나면 금액을 새 가격에, 이력`() {
        val now = Instant.parse("2026-10-19T05:00:00Z")
        val next = applyEdit(snap, original, form().copy(channel = "현장방문").changePrice("20000"), known, now)
        assertEquals("현장방문", next.reservations.single().channel)
        assertEquals(20000, next.reservations.single().price)
        assertEquals(original, next.history.single().before)

        val paidOne = original.copy(paymentEntries = listOf(PaymentEntry("카드", 18000)), paymentCompleted = true)
        val p = applyEdit(snap.copy(reservations = listOf(paidOne)), paidOne, ReservationForm.fromReservation(paidOne, known).changePrice("25000"), known, now)
        assertEquals(listOf(PaymentEntry("카드", 25000)), p.reservations.single().paymentEntries)
        assertEquals("카드", p.reservations.single().paymentMethod)

        val paidTwo = original.copy(paymentEntries = listOf(PaymentEntry("카드", 10000), PaymentEntry("현금", 8000)), paymentCompleted = true)
        val p2 = applyEdit(snap.copy(reservations = listOf(paidTwo)), paidTwo, ReservationForm.fromReservation(paidTwo, known).changePrice("25000"), known, now)
        assertEquals(paidTwo.paymentEntries, p2.reservations.single().paymentEntries)
    }
}
