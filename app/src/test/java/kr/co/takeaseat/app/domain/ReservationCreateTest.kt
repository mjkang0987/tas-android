package kr.co.takeaseat.app.domain

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReservationCreateTest {
    private val catalog = buildCatalogMap(SERVICE_CATALOG)
    private val labels = storeLabels("hair")
    private val kim = Customer(id = 3, name = "김민수", tel = "01011112222")
    // 2026-10-19 = 월요일. 기본 담당자는 평일 10~20시.
    private val base = LocalDbSnapshot(customers = listOf(kim), assignees = DEFAULT_ASSIGNEES)
    private fun form() = ReservationForm.initial("2026-10-19", "10:00", base.assignees)
    private fun valid() = form().changeCustomerQuery("새고객").copy(customerTel = "010-2222-3333").toggleService("남성커트", catalog)

    @Test fun `시작 시각 - 30분 반올림, 자정 직전은 23시 30분`() {
        assertEquals("10:00", roundedStartTime(LocalDateTime.of(2026, 10, 19, 10, 14)))
        assertEquals("10:30", roundedStartTime(LocalDateTime.of(2026, 10, 19, 10, 15)))
        assertEquals("11:00", roundedStartTime(LocalDateTime.of(2026, 10, 19, 10, 45)))
        assertEquals("23:30", roundedStartTime(LocalDateTime.of(2026, 10, 19, 23, 50)))
    }

    @Test fun `시간 계산·표시가 웹과 같다`() {
        assertEquals("11:30", calcEndTime("10:00", 90))
        assertEquals("00:30", calcEndTime("23:30", 60))
        assertEquals("1시간30분", formatDuration(90))
        assertEquals("2시간", formatDuration(120))
        assertEquals("", formatDuration(0))
        // 옛 이름도 카탈로그에서 찾힌다
        assertEquals(90, sumDurationMinutes(listOf("남자 일반펌"), catalog))
    }

    @Test fun `기본값 - 끝은 시작 + 30분, 첫 재직 담당자`() {
        val f = form()
        assertEquals("10:30", f.endTime)
        assertEquals(1, f.assigneeId)
        assertEquals(0, ReservationForm.initial("2026-10-19", "10:00", emptyList()).assigneeId)
    }

    @Test fun `서비스 고르면 끝·가격 다시 계산, 가격 손댔으면 가격은 그대로`() {
        val f = form().toggleService("남성커트", catalog).toggleService("크리닉", catalog)
        assertEquals("11:00", f.endTime)
        assertEquals(48000, f.price)
        val manual = f.changePrice("1만5,000").toggleService("숱추가", catalog)
        assertEquals(15000, manual.price)
        assertEquals(listOf("남성커트"), f.toggleService("크리닉", catalog).selectedServices)
    }

    @Test fun `시작 바꾸면 끝도 - 끝을 손댔으면 그대로`() {
        val f = form().toggleService("일반펌", catalog)
        assertEquals("13:30", f.changeStartTime("12:00", catalog).endTime)
        assertEquals("15:00", f.changeEndTime("15:00").changeStartTime("12:00", catalog).endTime)
        // 서비스 없으면 끝은 그대로
        assertEquals("10:30", form().changeStartTime("12:00", catalog).endTime)
    }

    @Test fun `고객 고르면 연락처 채움, 이름 다시 치면 새 고객으로`() {
        val picked = form().selectCustomer(kim)
        assertEquals("01011112222", picked.customerTel)
        val retyped = picked.changeCustomerQuery("김민")
        assertEquals(0, retyped.customerId)
        assertEquals("", retyped.customerTel)
        // 이미 새 고객이면 친 연락처는 둔다
        assertEquals("0101", form().copy(customerTel = "0101").changeCustomerQuery("이").customerTel)
    }

    @Test fun `검사 순서·문구`() {
        assertEquals("담당자를 선택해주세요.", validateCreate(valid().copy(assigneeId = 0), base, labels)?.message)
        assertEquals("고객명을 입력해주세요.", validateCreate(form(), base, labels)?.message)
        assertEquals("연락처를 입력해주세요.", validateCreate(form().changeCustomerQuery("이"), base, labels)?.message)
        assertEquals("연락처 형식을 확인해주세요.", validateCreate(valid().copy(customerTel = "0212345678"), base, labels)?.message)
        assertEquals("서비스를 선택해주세요.", validateCreate(valid().toggleService("남성커트", catalog), base, labels)?.message)
        assertEquals("시작 시간은 종료 시간보다 앞서야 합니다.", validateCreate(valid().changeEndTime("09:00"), base, labels)?.message)
        assertEquals(FormField.Date, validateCreate(valid().copy(date = "2026-10-18"), base, labels)?.field) // 일요일 휴무
        assertEquals("담당자 1 담당자 근무시간은 10:00~20:00입니다.", validateCreate(valid().copy(startTime = "09:00"), base, labels)?.message)
        assertNull(validateCreate(valid(), base, labels))
        // 기존 고객을 골랐으면 연락처 형식은 보지 않는다(웹과 같다)
        assertNull(validateCreate(form().selectCustomer(kim.copy(tel = "021234567")).toggleService("남성커트", catalog), base, labels))
    }

    @Test fun `겹치는 예약 - 취소·노쇼는 빼고`() {
        val other = Reservation(id = 9, date = "2026-10-19", startTime = "10:20", endTime = "11:00", service = "남성커트", customerId = 3)
        val withOther = base.copy(reservations = listOf(other))
        assertEquals("김민수 예약(10:20~11:00)과 시간이 겹칩니다.", validateCreate(valid(), withOther, labels)?.message)
        assertNull(validateCreate(valid(), base.copy(reservations = listOf(other.copy(status = ReservationStatus.CANCELLED))), labels))
        // 끝 = 상대 시작이면 안 겹친다
        assertNull(validateCreate(valid().changeEndTime("10:20"), withOther, labels))
    }

    @Test fun `저장 - 새 고객은 최댓값+1(삭제 고객 포함), 같은 이름·연락처면 기존 고객`() {
        val deleted = Customer(id = 7, name = "삭제된 고객", tel = "", deleted = true)
        val snap = base.copy(customers = listOf(kim, deleted), reservations = listOf(Reservation(id = 4, date = "2026-10-01", startTime = "10:00", endTime = "10:30", service = "", customerId = 3)))
        val saved = applyCreate(valid().copy(memo = "  "), snap)
        val added = saved.customers.last()
        assertEquals(8, added.id)
        assertEquals("01022223333", added.tel)
        val r = saved.reservations.last()
        assertEquals(5, r.id)
        assertEquals(8, r.customerId)
        assertEquals("남성커트", r.service)
        assertEquals(18000, r.price)
        assertEquals(1, r.assigneeId)
        assertNull(r.memo)
        assertEquals("전화예약", r.channel)

        val same = applyCreate(valid().changeCustomerQuery(" 김민수 ").copy(customerTel = "010-1111-2222"), snap)
        assertEquals(2, same.customers.size)
        assertEquals(3, same.reservations.last().customerId)
        assertNull(applyCreate(valid().copy(assigneeId = 0), snap).reservations.last().assigneeId)
    }

    @Test fun `담당자 정렬 - 영문 먼저, 그다음 가나다`() {
        val list = listOf(Assignee(1, "하늘"), Assignee(2, "Amy"), Assignee(3, "가람"), Assignee(4, "7번", status = "재직"), Assignee(5, "나리", status = "휴직"))
        assertEquals(listOf("Amy", "가람", "하늘", "7번"), activeAssignees(list).map { it.name })
    }
}
