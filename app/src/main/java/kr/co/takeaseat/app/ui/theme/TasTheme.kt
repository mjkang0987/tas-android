package kr.co.takeaseat.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// 머티리얼 기본값이 웹과 어긋나면 웹을 따른다(CLAUDE.md) — 색은 웹 토큰으로 덮는다. 라이트만(웹에 다크 모드 없음).
private val TasColorScheme = lightColorScheme(
    primary = TasColors.Brand,
    onPrimary = TasColors.White,
    background = TasColors.White,
    onBackground = TasColors.Black,
    surface = TasColors.White,
    onSurface = TasColors.Black,
)

@Composable
fun TasTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TasColorScheme, content = content)
}
