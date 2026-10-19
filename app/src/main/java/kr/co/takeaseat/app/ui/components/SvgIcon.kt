package kr.co.takeaseat.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 웹 SVG 선 아이콘(stroke-linecap/linejoin round, fill none)을 그대로 옮긴다. 색은 Icon tint 가 덮으므로 검정.
 * [fills] 는 채운 도형(원 점 등).
 */
fun svgLineIcon(
    viewBox: Float,
    strokes: List<String>,
    strokeWidth: Float,
    fills: List<String> = emptyList(),
    name: String = "svg",
): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = viewBox.dp, defaultHeight = viewBox.dp, viewportWidth = viewBox, viewportHeight = viewBox)
        .apply {
            strokes.forEach { d ->
                addPath(
                    pathData = PathParser().parsePathString(d).toNodes(),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
            fills.forEach { d -> addPath(pathData = PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.Black)) }
        }
        .build()

/** 아래 셰브론(웹 CalendarHeading·select 화살표, viewBox 24). */
val ChevronDownIcon: ImageVector = svgLineIcon(24f, listOf("M6 9.5L12 15.5L18 9.5"), strokeWidth = 2.2f, name = "chevron-down")
