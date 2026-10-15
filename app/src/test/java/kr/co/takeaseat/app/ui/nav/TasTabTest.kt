package kr.co.takeaseat.app.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Test

class TasTabTest {

    // 웹 MobileTabBar.tsx TABS 와 같은 순서·라벨이어야 한다.
    @Test
    fun tabsMatchWebOrderAndLabels() {
        assertEquals(listOf("캘린더", "고객", "매출", "설정"), TasTab.entries.map { it.label })
    }

    // 웹 SVG 를 옮겨 적은 path 가 하나라도 깨지면 아이콘이 빈 칸이 된다.
    @Test
    fun everyIconPathParses() {
        TabIconKind.entries.forEach { kind ->
            val vector = kind.toImageVector(strokeWidth = 1.5f)
            assertEquals(kind.name, kind.strokes.size + kind.fills.size, vector.root.size)
        }
    }
}
