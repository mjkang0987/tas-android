package kr.co.takeaseat.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit

/**
 * CSS line-height 처럼 한 줄 높이를 못박는다 — 글자는 그 상자 가운데 놓이고 넘치는 여백은 겹친다.
 *
 * 왜 필요한가: Compose 는 대체 글꼴 줄간격(fallback line spacing)을 강제로 켜 둬서, 한글(Noto CJK)이 들어간
 * 줄은 `lineHeight` 를 줘도 글꼴 본래 높이(약 1.45em)보다 줄지 않는다. 웹 카드는 1.2em 줄로 30분(50px)에
 * 세 줄을 넣는데, 그대로 두면 시술 칩이 잘린다. 한 줄짜리 글자에만 쓴다(여러 줄은 줄 사이가 겹친다).
 */
fun Modifier.lineBox(lineHeight: TextUnit): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    val height = lineHeight.roundToPx().coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(placeable.width, height) { placeable.place(0, (height - placeable.height) / 2) }
}
