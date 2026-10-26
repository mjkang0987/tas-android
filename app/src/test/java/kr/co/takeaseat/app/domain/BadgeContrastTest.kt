package kr.co.takeaseat.app.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/** 상태 배지 글자 대비 — 배경 위 4.5:1 이상(WCAG AA, 작은 글자). 웹 badgeContrast.test.ts 와 같은 기준. */
class BadgeContrastTest {
    // CSS #RRGGBB / #RRGGBBAA — 알파가 있으면 흰 바탕에 섞는다
    private fun rgb(hex: String): DoubleArray {
        val s = hex.removePrefix("#")
        val a = if (s.length == 8) s.substring(6, 8).toInt(16) / 255.0 else 1.0
        return DoubleArray(3) { i -> s.substring(i * 2, i * 2 + 2).toInt(16) * a + 255 * (1 - a) }
    }

    private fun luminance(c: DoubleArray): Double {
        fun f(v: Double): Double = (v / 255).let { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * f(c[0]) + 0.7152 * f(c[1]) + 0.0722 * f(c[2])
    }

    @Test fun `모든 상태 배지가 4_5 대 1 이상`() {
        for ((type, colors) in RESERVATION_STATUS_BADGE_STYLES) {
            val (a, b) = luminance(rgb(colors.color)) to luminance(rgb(colors.bg))
            val ratio = (max(a, b) + 0.05) / (min(a, b) + 0.05)
            assertTrue("$type 대비 $ratio", ratio >= 4.5)
        }
    }

    // 어두운 화면 — 글자 7:1 이상, 바탕이 카드(#1C1C1E)와 1.3:1 이상 구분(iOS·웹과 같은 기준)
    @Test fun `어두운 화면 배지 - 글자 7 대 1 이상, 카드와 구분`() {
        assertTrue(RESERVATION_STATUS_BADGE_STYLES_DARK.keys == RESERVATION_STATUS_BADGE_STYLES.keys)
        fun r(x: String, y: String): Double {
            val (a, b) = luminance(rgb(x)) to luminance(rgb(y))
            return (max(a, b) + 0.05) / (min(a, b) + 0.05)
        }
        for ((type, colors) in RESERVATION_STATUS_BADGE_STYLES_DARK) {
            assertTrue("$type 글자 ${r(colors.color, colors.bg)}", r(colors.color, colors.bg) >= 7.0)
            assertTrue("$type 바탕 ${r(colors.bg, "#1C1C1E")}", r(colors.bg, "#1C1C1E") >= 1.3)
        }
    }
}
