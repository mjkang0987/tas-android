package kr.co.takeaseat.app.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class HexColorTest {
    @Test fun `CSS 색 문자열 - 3·6·8자리(#RRGGBBAA), 망가지면 회색`() {
        assertEquals(Color(0xFF999999), hexColor("#999"))
        assertEquals(Color(0xFF6526D9), hexColor("#6526d9"))
        assertEquals(Color(0x146526D9), hexColor("#6526D914")) // rgba(101,38,217,0.08)
        assertEquals(Color(0xFF999999), hexColor("nope"))
    }
}
