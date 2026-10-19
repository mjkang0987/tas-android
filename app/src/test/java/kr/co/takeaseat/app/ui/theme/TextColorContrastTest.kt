package kr.co.takeaseat.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.ui.components.backgroundForWhiteText

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

    @Test fun `흰 글자 바탕 - 밝은 색만 같은 색을 진하게, 웹과 같은 값`() {
        // 웹 features/colors/contrast.ts backgroundForWhiteText 결과
        val expected = mapOf(
            "#4285F4" to "#206ff2", "#34A853" to "#298542", "#EA4335" to "#e42718", "#FBBC04" to "#926d02",
            "#FF6D01" to "#c35300", "#46BDC6" to "#2a8087", "#9334E6" to "#9334E6", "#E91E8C" to "#dd1682",
            "#2D7FF9" to "#0f6df8", "#E85D75" to "#e12c4b", "#00A896" to "#008476", "#FB8C00" to "#ae6100",
        )
        for ((input, out) in expected) {
            assertEquals(input, hexColor(out), backgroundForWhiteText(hexColor(input)))
        }
        for (v in 0..255) {
            val bg = backgroundForWhiteText(Color(v, v, v))
            assertTrue("회색 $v", ratio(Color.White, bg) >= 4.5)
        }
    }
}
