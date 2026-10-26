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

    private fun textTokens(p: TasPalette) = mapOf(
        "Blue" to p.Blue, "Warning" to p.Warning, "Danger" to p.Danger, "Info" to p.Info, "Caution" to p.Caution,
        "DarkGray2" to p.DarkGray2, "Link" to p.Link, "Brand" to p.Brand, "Black" to p.Black, "DarkGray" to p.DarkGray,
        "Success" to p.Success, "SuccessText" to p.SuccessText, "WarningText" to p.WarningText, "NeutralText" to p.NeutralText,
        "NewCustomer" to p.NewCustomer, "CardInk" to p.CardInk,
    )

    @Test fun `글자색 토큰이 4_5 대 1 이상`() {
        val p = LightPalette
        val tokens = mapOf(
            "Blue" to p.Blue, "Warning" to p.Warning, "Danger" to p.Danger,
            "Info" to p.Info, "Caution" to p.Caution, "DarkGray2" to p.DarkGray2, "Link" to p.Link,
        )
        for ((name, color) in tokens) {
            for (bg in listOf(Color.White, Color(0xFFF7F7F7))) {
                val r = ratio(color, bg)
                assertTrue("$name 대비 $r", r >= 4.5)
            }
        }
    }

    // 어두운 화면 — 카드 바탕(Surface #1C1C1E)·옅은 회색(Gray2 #2C2C2E) 위 4.5:1 이상. 웹 '어두운 화면 글자색 토큰 대비' 와 같은 기준.
    @Test fun `어두운 화면 글자색 토큰이 4_5 대 1 이상`() {
        for ((name, color) in textTokens(DarkPalette)) {
            for (bg in listOf(DarkPalette.Surface, DarkPalette.Gray2)) {
                val r = ratio(color, bg)
                assertTrue("어두운 $name 대비 $r", r >= 4.5)
            }
        }
    }

    // 흰 글자를 얹는 채움은 모드와 상관없이 4.5:1 이상
    @Test fun `채움 위 흰 글자 4_5 대 1 이상`() {
        for (p in listOf(LightPalette, DarkPalette)) {
            for (fill in listOf(p.BrandFill, p.BlueFill, p.DangerFill, p.NaverFill)) {
                assertTrue("채움 $fill", ratio(p.White, fill) >= 4.5)
            }
            assertTrue("뒤집은 칩", ratio(p.InverseText, p.InverseBg) >= 4.5)
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
