package kr.co.takeaseat.app.ui.nav

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// 웹 `client/components/layout/AsideMenuIcon.tsx` 의 SVG 를 그대로 옮긴다(viewBox 24, 선 아이콘).
// SVG 의 rect·circle 은 같은 모양의 path 로 풀어 적었다. 색은 Icon tint 가 덮으므로 검정으로 둔다.
enum class TabIconKind(
    val strokes: List<String>,
    val fills: List<String> = emptyList(),
) {
    // case 'calendarManage': <rect x=3.5 y=4.5 w=17 h=16 rx=3/> + path
    CalendarManage(
        strokes = listOf(
            "M6.5 4.5H17.5A3 3 0 0 1 20.5 7.5V17.5A3 3 0 0 1 17.5 20.5H6.5A3 3 0 0 1 3.5 17.5V7.5A3 3 0 0 1 6.5 4.5Z",
            "M7.5 3.5V6.5M16.5 3.5V6.5M3.5 9H20.5M8 13H16M8 17H13.5",
        ),
    ),

    // case 'customers': 가로줄 3 + 점 3(<circle r=1 fill>)
    Customers(
        strokes = listOf("M8 7H19M8 12H19M8 17H19"),
        fills = listOf(
            "M4 7A1 1 0 1 0 6 7A1 1 0 1 0 4 7Z",
            "M4 12A1 1 0 1 0 6 12A1 1 0 1 0 4 12Z",
            "M4 17A1 1 0 1 0 6 17A1 1 0 1 0 4 17Z",
        ),
    ),

    // case 'revenue'
    Revenue(
        strokes = listOf(
            "M12 4V20M16 7.5C16 6.1 14.2 5 12 5C9.8 5 8 6.1 8 7.5C8 8.9 9.8 10 12 10C14.2 10 16 11.1 16 12.5C16 13.9 14.2 15 12 15C9.8 15 8 13.9 8 12.5M8.5 17C9.3 17.8 10.5 18.2 12 18.2C14.2 18.2 16 17.1 16 15.7",
        ),
    ),

    // case 'settings': <circle cx=12 cy=12 r=3.2/> + 톱니 path
    Settings(
        strokes = listOf(
            "M8.8 12A3.2 3.2 0 1 0 15.2 12A3.2 3.2 0 1 0 8.8 12Z",
            "M19.4 15A1.65 1.65 0 0 0 19.73 16.82L19.79 16.88A2 2 0 1 1 16.96 19.71L16.9 19.65A1.65 1.65 0 0 0 15.08 19.32A1.65 1.65 0 0 0 14.08 20.84V20.99A2 2 0 1 1 10.08 20.99V20.9A1.65 1.65 0 0 0 9 19.39A1.65 1.65 0 0 0 7.18 19.72L7.12 19.78A2 2 0 1 1 4.29 16.95L4.35 16.89A1.65 1.65 0 0 0 4.68 15.07A1.65 1.65 0 0 0 3.16 14.07H3.01A2 2 0 1 1 3.01 10.07H3.1A1.65 1.65 0 0 0 4.61 9A1.65 1.65 0 0 0 4.28 7.18L4.22 7.12A2 2 0 1 1 7.05 4.29L7.11 4.35A1.65 1.65 0 0 0 8.93 4.68A1.65 1.65 0 0 0 9.93 3.16V3.01A2 2 0 1 1 13.93 3.01V3.1A1.65 1.65 0 0 0 15 4.61A1.65 1.65 0 0 0 16.82 4.28L16.88 4.22A2 2 0 1 1 19.71 7.05L19.65 7.11A1.65 1.65 0 0 0 19.32 8.93A1.65 1.65 0 0 0 20.84 9.93H20.99A2 2 0 1 1 20.99 13.93H20.9A1.65 1.65 0 0 0 19.39 15Z",
        ),
    ),
}

/** 웹 `StyledMenuIcon`(stroke-linecap/linejoin round, fill none)과 같은 선 아이콘. 선 두께는 탭 상태에 따라 바뀐다. */
fun TabIconKind.toImageVector(strokeWidth: Float): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
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
            fills.forEach { d ->
                addPath(pathData = PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.Black))
            }
        }
        .build()
