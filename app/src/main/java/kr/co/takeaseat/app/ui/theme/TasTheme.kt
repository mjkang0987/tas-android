package kr.co.takeaseat.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// 머티리얼 기본값이 웹과 어긋나면 웹을 따른다(CLAUDE.md) — 색은 웹 토큰으로 덮는다. 라이트만(웹에 다크 모드 없음).
private val TasColorScheme = lightColorScheme(
    primary = TasColors.Brand,
    onPrimary = TasColors.White,
    background = TasColors.White,
    onBackground = TasColors.Black,
    surface = TasColors.White,
    onSurface = TasColors.Black,
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
fun TasTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TasColorScheme, typography = TasTypography, content = content)
}
