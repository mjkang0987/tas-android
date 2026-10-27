package kr.co.takeaseat.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// 머티리얼 기본값이 웹과 어긋나면 웹을 따른다(CLAUDE.md) — 색은 웹 토큰으로 덮는다. 밝은/어두운 두 벌, 시스템 테마 따라.
private val TasLightScheme = lightColorScheme(
    primary = LightPalette.BrandFill,
    onPrimary = LightPalette.White,
    background = LightPalette.Surface,
    onBackground = LightPalette.Black,
    surface = LightPalette.Surface,
    onSurface = LightPalette.Black,
)

private val TasDarkScheme = darkColorScheme(
    primary = DarkPalette.BrandFill,
    onPrimary = DarkPalette.White,
    background = DarkPalette.Surface,
    onBackground = DarkPalette.Black,
    surface = DarkPalette.Surface,
    onSurface = DarkPalette.Black,
)

// 머티리얼 본문(bodyLarge)은 줄높이 24sp·자간 0.5sp 라 웹 글자(line-height normal·자간 0)보다 퍼진다.
// 줄높이를 비워 두면 한글 대체 글꼴(Noto CJK)의 긴 줄높이(약 1.45)가 그대로 나와, 웹에서 50px 에 들어가는
// 30분 카드 세 줄이 넘친다. 웹 `normal`(약 1.2)로 못박는다. 크기는 각 자리에서 웹 값으로 준다.
private val TasTypography = Typography(
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 1.2.em,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None),
    ),
)

@Composable
fun TasTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTasPalette provides if (dark) DarkPalette else LightPalette) {
        MaterialTheme(colorScheme = if (dark) TasDarkScheme else TasLightScheme, typography = TasTypography, content = content)
    }
}
