package kr.co.takeaseat.app.domain

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTest {
    private val now = LocalDateTime.of(2026, 10, 22, 9, 0)
    private val customers = mapOf(1 to "김민지")
    private fun r(id: Int, date: String, start: String, status: String? = null, service: String = "남성커트", customerId: Int = 1) =
        Reservation(id, date, start, "23:00", service, customerId, null, status, null)

    @Test fun `시점 표시`() {
        assertEquals(listOf("끔", "10분 전", "30분 전", "1시간 전"), REMINDER_LEAD_OPTIONS.map(::reminderLeadLabel))
    }

    @Test fun `알림 내용 - 시각·고객·서비스, 고객 없으면 '고객'`() {
        val reminder = reminderFor(r(1, "2026-10-22", "14:00"), customers, 30)!!
        assertEquals(LocalDateTime.of(2026, 10, 22, 13, 30), reminder.triggerAt)
        assertEquals("예약 30분 전", reminder.title)
        assertEquals("14:00 김민지 · 남성커트", reminder.text)
        assertEquals("14:00 고객", reminderFor(r(2, "2026-10-22", "14:00", service = "", customerId = 9), customers, 30)!!.text)
    }

    @Test fun `진행 중 예약만 - 취소·노쇼·완료·확정대기·끔·깨진 시각은 없음`() {
        listOf(ReservationStatus.CANCELLED, ReservationStatus.NOSHOW, ReservationStatus.COMPLETED, ReservationStatus.REQUESTED).forEach {
            assertNull(reminderFor(r(1, "2026-10-22", "14:00", status = it), customers, 30))
        }
        assertEquals(1, reminderFor(r(1, "2026-10-22", "14:00", status = ReservationStatus.ACTIVE), customers, 30)?.reservationId)
        assertNull(reminderFor(r(1, "2026-10-22", "14:00"), customers, 0))
        assertNull(reminderFor(r(1, "2026-10-22", "2시"), customers, 30))
    }

    @Test fun `맞출 알림 - 지금 이후·7일 안·이른 순·한도`() {
        val list = listOf(
            r(1, "2026-10-22", "09:20"), // 08:50 — 이미 지남
            r(2, "2026-10-22", "09:40"), // 09:10
            r(3, "2026-10-29", "10:00"), // 7일 넘음
            r(4, "2026-10-22", "09:31"), // 09:01
        )
        assertEquals(listOf(4, 2), upcomingReminders(list, customers, 30, now).map { it.reservationId })
        assertEquals(listOf(4), upcomingReminders(list, customers, 30, now, limit = 1).map { it.reservationId })
        assertEquals(emptyList<Int>(), upcomingReminders(list, customers, 0, now).map { it.reservationId })
    }

    @Test fun `지각 알림 - 시작 뒤 기준만큼, 진행 중이고 결제 안 된 예약만`() {
        val late = lateAlertFor(r(1, "2026-10-22", "14:00"), customers, 15)!!
        assertEquals(LocalDateTime.of(2026, 10, 22, 14, 15), late.triggerAt)
        assertEquals(ReminderKind.Late, late.kind)
        assertEquals("손님이 아직 안 오셨나요?", late.title)
        assertEquals("14:00 김민지 · 남성커트 — 예약 15분 지남", late.text)
        listOf(ReservationStatus.CANCELLED, ReservationStatus.NOSHOW, ReservationStatus.COMPLETED, ReservationStatus.REQUESTED).forEach {
            assertNull(lateAlertFor(r(1, "2026-10-22", "14:00", status = it), customers, 15))
        }
        // 결제까지 끝났으면 손님은 온 것
        assertNull(lateAlertFor(r(1, "2026-10-22", "14:00").copy(paymentCompleted = true), customers, 15))
        assertNull(lateAlertFor(r(1, "2026-10-22", "14:00"), customers, 0))
    }

    @Test fun `지각 기준 - 매장 설정이 없으면 15분, 표시`() {
        assertEquals(15, lateAlertMinutesOf(StoreSettings()))
        assertEquals(0, lateAlertMinutesOf(StoreSettings(lateAlertMinutes = 0)))
        assertEquals(listOf("끔", "5분 후", "30분 후"), listOf(0, 5, 30).map(::lateAlertLabel))
    }

    @Test fun `맞출 알림 - 시작 전과 지각을 함께, 시작 전 알림 꺼도 지각은 남는다`() {
        val list = listOf(r(1, "2026-10-22", "09:40")) // 시작 전 09:10, 지각 09:55
        val both = upcomingReminders(list, customers, 30, now, lateMinutes = 15)
        assertEquals(listOf(ReminderKind.Before, ReminderKind.Late), both.map { it.kind })
        assertEquals(listOf(ReminderKind.Late), upcomingReminders(list, customers, 0, now, lateMinutes = 15).map { it.kind })
        assertEquals(emptyList<ReminderKind>(), upcomingReminders(list, customers, 0, now, lateMinutes = 0).map { it.kind })
    }
}
