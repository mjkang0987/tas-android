package kr.co.takeaseat.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoreManageTest {
    @Test fun `업종 - 콤마로 이은 것 중 첫 번째로 아는 업종, 표시어는 그 분류`() {
        assertEquals("cafe", primaryIndustry("unknown, cafe,hair")?.value)
        assertNull(primaryIndustry(null))
        assertEquals(StoreLabels("테이블", "메뉴"), storeLabels("cafe"))
        assertEquals(StoreLabels("담당자", "서비스"), storeLabels("모름"))
        // 분류 이름이 모든 업종 분류를 덮는다
        assertEquals(SHOP_INDUSTRIES.map { it.category }.toSet(), CATEGORY_NAMES.keys.intersect(SHOP_INDUSTRIES.map { it.category }.toSet()))
    }

    @Test fun `매장 정보 - 이름 공백 제거, 비면 오류, 업종 선택 안 함은 null`() {
        val snap = LocalDbSnapshot(storeName = "옛 이름", shopType = "hair")
        val (saved, error) = saveStoreInfo(snap, "  새 매장  ", "")
        assertNull(error)
        assertEquals("새 매장" to null, saved.storeName to saved.shopType)
        assertEquals("매장 이름을 입력해 주세요." to snap, saveStoreInfo(snap, " ", "cafe").let { it.second to it.first })
    }

    @Test fun `휴업일 - 날짜순으로 넣고, 비거나 같은 날이면 오류`() {
        assertEquals(listOf("2026-10-01", "2026-10-22") to null, addClosedDate(listOf("2026-10-22"), "2026-10-01"))
        assertEquals("휴업일을 선택해 주세요.", addClosedDate(emptyList(), "").second)
        assertEquals("이미 등록된 휴업일입니다.", addClosedDate(listOf("2026-10-22"), "2026-10-22").second)
        assertEquals("2026년 10월 22일 (목)", formatDateLabel("2026-10-22"))
        assertEquals("x", formatDateLabel("x"))
    }
}
