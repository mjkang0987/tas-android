package kr.co.takeaseat.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import kotlin.math.roundToInt

// 한글 글자 몸통의 세로 가운데가 기준선(baseline)에서 얼마나 위에 있는지(em). Noto Sans CJK 실측 — 숫자·영문도 0.36 언저리라 함께 쓴다.
private const val INK_CENTER_EM = 0.38f

/**
 * CSS line-height 처럼 한 줄 높이를 못박고, 글자를 그 상자 **눈으로 보이는 가운데**에 놓는다.
 *
 * 왜 필요한가:
 * - Compose 는 대체 글꼴 줄간격을 강제로 켜 둬서 한글(Noto CJK) 줄이 `lineHeight` 를 줘도 약 1.45em 아래로 안 준다.
 *   웹 카드는 1.2em 줄로 30분(50px)에 세 줄을 넣는데, 그대로 두면 시술 칩이 잘린다.
 * - 글자 상자(위 1.16em·아래 0.29em)를 그냥 가운데 두면 위쪽 여백이 커서 글자가 아래로 1~2dp 처진다(배지 상하 여백이
 *   11:6 로 어긋났다). 그래서 상자가 아니라 기준선으로 맞춘다.
 * 한 줄짜리 글자에만 쓴다.
 */
fun Modifier.lineBox(lineHeight: TextUnit, fontSize: TextUnit): Modifier {
    require(lineHeight.isSp && fontSize.isSp) { "lineBox 는 sp 만 받는다(em 은 글자 크기 없이는 픽셀로 못 바꾼다)" }
    return lineBoxLayout(lineHeight, fontSize)
}

private fun Modifier.lineBoxLayout(lineHeight: TextUnit, fontSize: TextUnit): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    val height = lineHeight.roundToPx().coerceIn(constraints.minHeight, constraints.maxHeight)
    val baseline = placeable[FirstBaseline]
    val y = if (baseline == androidx.compose.ui.layout.AlignmentLine.Unspecified) {
        (height - placeable.height) / 2
    } else {
        (height / 2f + INK_CENTER_EM * fontSize.toPx() - baseline).roundToInt()
    }
    layout(placeable.width, height) { placeable.place(0, y) }
}
