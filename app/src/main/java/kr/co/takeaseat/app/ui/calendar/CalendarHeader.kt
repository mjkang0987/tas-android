package kr.co.takeaseat.app.ui.calendar

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 헤더 1줄(`CalendarDirection` + `CalendarHeading`) — 일 뷰 기준.
 * 날짜를 누르면 시스템 날짜 선택이 뜬다(웹도 네이티브 `<input type="date">`).
 * shortcut: + 예약 버튼은 예약 추가 화면이 생길 때 붙인다.
 */
@Composable
fun CalendarHeader(date: LocalDate, onDateChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // 웹: header padding 4 10 2 + 줄 padding 3 0 5
    Row(
        modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PillButton(onClick = { onDateChange(LocalDate.now()) }, contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text("오늘", color = TasColors.Black, fontSize = TasSizes.SmallFont, maxLines = 1)
        }
        PillButton(onClick = { onDateChange(date.minusDays(1)) }, description = "이전날짜") {
            Icon(directionIcon(left = true), contentDescription = null, tint = TasColors.Icon, modifier = Modifier.size(18.dp))
        }
        PillButton(onClick = { onDateChange(date.plusDays(1)) }, description = "다음날짜") {
            Icon(directionIcon(left = false), contentDescription = null, tint = TasColors.Icon, modifier = Modifier.size(18.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .height(32.dp)
                    .clip(TasShapes.Chip)
                    .background(TasColors.Gray2)
                    // 웹 aria-label "날짜 선택" + 값. 보이는 "2026 / 10 / 17" 은 읽지 않는다(아래 clearAndSetSemantics).
                    .semantics { contentDescription = "날짜 선택, ${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일" }
                    .clickable(role = Role.Button) {
                        DatePickerDialog(context, { _, y, m, d -> onDateChange(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                    }
                    .padding(start = 10.dp, end = 6.dp), // 웹 모바일 padding 0 6 0 10
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "${date.year} / ${date.monthValue} / ${date.dayOfMonth}",
                    color = Color.Black,
                    fontSize = TasSizes.LargeFont,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics {},
                )
                Icon(chevronDown, contentDescription = null, tint = TasColors.DarkGray2, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * 웹 `ButtonSquare`(오늘) · `StyledDirectionButton`(‹ ›) — 흰 알약 32, 1px 테두리, 그림자 sm, 누르면 회색.
 * 32dp 는 웹과 같은 크기다(WCAG 2.2 AA 최소 24 충족, 안드로이드 권장 48 보다는 작다).
 */
@Composable
private fun PillButton(
    onClick: () -> Unit,
    description: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        Modifier
            .height(32.dp)
            .widthIn(min = 32.dp)
            .dropShadow(TasShapes.Chip, Shadow(radius = TasSizes.ShadowSmBlur, color = TasColors.ShadowSm, offset = DpOffset(0.dp, TasSizes.ShadowSmOffsetY)))
            .clip(TasShapes.Chip)
            .background(if (pressed) TasColors.Gray2 else TasColors.White)
            .border(1.dp, TasColors.Border, TasShapes.Chip)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// 웹 DirectionIcon(viewBox 32) · CalendarHeading 셰브론(viewBox 24) 경로 그대로.
private fun directionIcon(left: Boolean): ImageVector =
    strokeIcon(32f, if (left) "M18.5 10.5L13 16L18.5 21.5" else "M13.5 10.5L19 16L13.5 21.5")

private val chevronDown: ImageVector = strokeIcon(24f, "M6 9.5L12 15.5L18 9.5")

private fun strokeIcon(viewBox: Float, path: String): ImageVector =
    ImageVector.Builder(defaultWidth = viewBox.dp, defaultHeight = viewBox.dp, viewportWidth = viewBox, viewportHeight = viewBox)
        .addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
