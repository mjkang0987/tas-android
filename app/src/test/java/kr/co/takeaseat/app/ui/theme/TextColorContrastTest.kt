package kr.co.takeaseat.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertTrue
import org.junit.Test
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.ui.components.readableTextOn

/** 글자색으로 쓰는 토큰 — 흰 바탕·옅은 회색(#F7F7F7) 위 4.5:1 이상. 웹 badgeContrast.test.ts '글자색 토큰 대비' 와 같은 기준. */
class TextColorContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val (la, lb) = a.luminance().toDouble() to b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    @Test fun `글자색 토큰이 4_5 대 1 이상`() {
        val tokens = mapOf(
            "Blue" to TasColors.Blue, "Warning" to TasColors.Warning, "Danger" to TasColors.Danger,
            "Info" to TasColors.Info, "Caution" to TasColors.Caution, "DarkGray2" to TasColors.DarkGray2, "Link" to TasColors.Link,
        )
        for ((name, color) in tokens) {
            for (bg in listOf(Color.White, Color(0xFFF7F7F7))) {
                val r = ratio(color, bg)
                assertTrue("$name 대비 $r", r >= 4.5)
            }
        }
    }

    @Test fun `색 바탕 위 글자 - 메모 태그·담당자 팔레트와 회색 전부 4_5 이상, 노랑은 검정 보라는 흰`() {
        val palette = listOf("#4285F4", "#34A853", "#EA4335", "#FBBC04", "#FF6D01", "#46BDC6", "#9334E6", "#E91E8C",
            "#6526D9", "#2D7FF9", "#E85D75", "#00A896", "#FB8C00", "#6D6F78", "#7E57C2").map(::hexColor) +
            (0..255).map { Color(it, it, it) }
        for (bg in palette) {
            val r = ratio(readableTextOn(bg), bg)
            assertTrue("$bg 대비 $r", r >= 4.5)
        }
        assertTrue(readableTextOn(hexColor("#FBBC04")) == Color.Black)
        assertTrue(readableTextOn(hexColor("#6526D9")) == Color.White)
    }
}
