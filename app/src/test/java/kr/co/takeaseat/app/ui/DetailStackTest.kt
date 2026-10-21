package kr.co.takeaseat.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DetailStackTest {
    @Test fun `상세 창 쌓기 - 새 창은 위에, 이미 열린 창이면 그 위를 닫고 그 창으로`() {
        assertEquals(listOf("r1"), pushDetail(emptyList(), "r1"))
        assertEquals(listOf("r1", "c3"), pushDetail(listOf("r1"), "c3"))
        assertEquals(listOf("r1", "c3", "r2"), pushDetail(listOf("r1", "c3"), "r2"))
        // 고객 상세에서 처음 연 예약을 다시 누르면 고객 창을 닫고 그 예약으로 돌아간다(같은 창 두 장 방지)
        assertEquals(listOf("r1"), pushDetail(listOf("r1", "c3"), "r1"))
        assertEquals(listOf("r1", "c3"), pushDetail(listOf("r1", "c3"), "c3"))
    }
}
