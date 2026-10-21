package kr.co.takeaseat.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ServiceManageTest {
    private val cut = ServiceItem("커트", 30, "헤어", 20000)
    private val perm = ServiceItem("펌", 60, "헤어", 50000)
    private val clinic = ServiceItem("크리닉", 30, "케어", 30000)
    private val snap = LocalDbSnapshot(services = listOf(cut, perm, clinic), categoryBaseColors = mapOf("헤어" to "#111111"))
    private val today = "2026-10-21"

    private fun r(id: Int, date: String, service: String, start: String, end: String, price: Int?, status: String? = null, entries: List<PaymentEntry>? = null) =
        Reservation(id, date, start, end, service, 1, status = status, price = price, paymentEntries = entries)

    @Test fun `검사 - 카테고리, 이름 비움, 같은 이름(수정은 자기 이름 허용)`() {
        assertEquals("카테고리를 선택하거나 새 카테고리를 추가해 주세요.", validateServiceAdd(ServiceForm(name = "x"), snap.services, "서비스"))
        assertEquals("새 카테고리명을 입력해 주세요.", validateServiceAdd(ServiceForm(category = NEW_CATEGORY, newCategory = " "), snap.services, "서비스"))
        assertEquals("서비스명을 입력해 주세요.", validateServiceAdd(ServiceForm(category = "헤어", name = "  "), snap.services, "서비스"))
        assertEquals("\"커트\" 메뉴는 이미 등록되어 있습니다.", validateServiceAdd(ServiceForm(category = "헤어", name = " 커트 "), snap.services, "메뉴"))
        assertNull(validateServiceAdd(ServiceForm(category = NEW_CATEGORY, newCategory = "네일", name = "젤"), snap.services, "서비스"))
        assertNull(validateServiceEdit(ServiceForm(name = "커트"), "커트", snap.services, "서비스"))
        assertEquals("\"펌\" 서비스는 이미 등록되어 있습니다.", validateServiceEdit(ServiceForm(name = "펌"), "커트", snap.services, "서비스"))
    }

    @Test fun `입력 → 서비스 - 이름 공백 제거, 숫자 아니면 0, 다국어는 빈 칸 빼고 다 비면 null`() {
        val form = ServiceForm(category = NEW_CATEGORY, newCategory = " 네일 ", name = " 젤 ", nameEn = " Gel ", duration = "", price = "abc")
        assertEquals(ServiceItem("젤", 0, "네일", 0, NameI18n(en = "Gel")), form.toItem(form.resolvedCategory()))
        assertNull(buildNameI18n(" ", "", ""))
        assertEquals(ServiceForm("헤어", "", "커트", "", "", "", "30", "20000"), ServiceForm.of(cut))
    }

    @Test fun `수정 - 앞으로의 진행·미결제 예약만 이름·가격·종료를 바꾸고, 손댄 가격·시간은 둔다`() {
        val s = snap.copy(reservations = listOf(
            r(1, "2026-10-22", "커트+크리닉", "10:00", "11:00", 50000),               // 그대로 따라감
            r(2, "2026-10-22", "커트", "12:00", "12:45", 15000),                       // 가격·시간 손댐 → 이름만
            r(3, "2026-10-20", "커트", "10:00", "10:30", 20000),                       // 지난 예약
            r(4, "2026-10-22", "커트", "10:00", "10:30", 20000, ReservationStatus.CANCELLED),
            r(5, "2026-10-22", "커트", "10:00", "10:30", 20000, entries = listOf(PaymentEntry("카드", 20000))), // 결제됨
            r(6, "2026-10-22", "펌", "10:00", "11:00", 50000),                          // 다른 서비스
        ))
        val (next, applied) = updateService(s, "커트", ServiceItem("남성커트", 40, "헤어", 25000), today)
        assertEquals(2, applied)
        assertEquals(Triple("남성커트+크리닉", 55000, "11:10"), next.reservations[0].let { Triple(it.service, it.price, it.endTime) })
        assertEquals(Triple("남성커트", 15000, "12:45"), next.reservations[1].let { Triple(it.service, it.price, it.endTime) })
        assertEquals(s.reservations.drop(2), next.reservations.drop(2))
        assertEquals(listOf("남성커트", "펌", "크리닉"), next.services.map { it.name })
    }

    @Test fun `카테고리 - 이름 바꾸면 서비스·대표색이 따라가고, 빈 이름·같은 이름은 그대로`() {
        val renamed = renameCategory(snap, "헤어", " 헤어컷 ")
        assertEquals(listOf("헤어컷", "헤어컷", "케어"), renamed.services.map { it.category })
        assertEquals(mapOf("헤어컷" to "#111111"), renamed.categoryBaseColors)
        assertSame(snap, renameCategory(snap, "헤어", "케어"))
        assertEquals("카테고리명을 입력해 주세요.", validateCategoryName(listOf("헤어", "케어"), "헤어", " "))
        assertEquals("\"케어\" 카테고리는 이미 있습니다.", validateCategoryName(listOf("헤어", "케어"), "헤어", "케어"))
        assertNull(validateCategoryName(listOf("헤어", "케어"), "헤어", "헤어"))
        assertEquals("#222222", setCategoryColor(snap, "케어", "#222222").categoryBaseColors["케어"])
        assertEquals(listOf("커트", "펌"), deleteService(snap, "크리닉").services.map { it.name })
        assertEquals("젤", addService(snap, ServiceItem("젤", 0, "네일", 0)).services.last().name)
    }
}
