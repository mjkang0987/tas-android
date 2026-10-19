package kr.co.takeaseat.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertTrue
import org.junit.Test

/** 글자색으로 쓰는 토큰 — 흰 바탕·옅은 회색(#F7F7F7) 위 4.5:1 이상. 웹 badgeContrast.test.ts '글자색 토큰 대비' 와 같은 기준. */
class TextColorContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val (la, lb) = a.luminance().toDouble() to b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    @Test fun `글자색 토큰이 4_5 대 1 이상`() {
        val tokens = mapOf(
            "Blue" to TasColors.Blue, "Warning" to TasColors.Warning, "Danger" to TasColors.Danger,
            "Info" to TasColors.Info, "Caution" to TasColors.Caution,
        )
        for ((name, color) in tokens) {
            for (bg in listOf(Color.White, Color(0xFFF7F7F7))) {
                val r = ratio(color, bg)
                assertTrue("$name 대비 $r", r >= 4.5)
            }
        }
    }
}
