package kr.co.takeaseat.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.co.takeaseat.app.domain.Assignee
import kr.co.takeaseat.app.domain.UNASSIGNED_COLOR
import kr.co.takeaseat.app.domain.UPCOMING_MAX_SHOWN
import kr.co.takeaseat.app.domain.Upcoming
import kr.co.takeaseat.app.domain.UpcomingReservation
import kr.co.takeaseat.app.domain.untilLabel
import kr.co.takeaseat.app.ui.components.AssigneeLabel
import kr.co.takeaseat.app.ui.components.DetailSheet
import kr.co.takeaseat.app.ui.components.LabelBadge
import kr.co.takeaseat.app.ui.components.LabelTone
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.ui.components.svgLineIcon
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 헤더 알림 종(`NotificationCenter` 버튼) — 32 투명 버튼, 진회색 종.
 * 처리할 일은 빨간 숫자(9 넘으면 9+), 곧 시작하는 예약만 있으면 보라 점.
 */
@Composable
fun NotificationBell(count: Int, dot: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = when {
        count > 0 -> "알림, 처리할 일 ${count}건"
        dot -> "알림, 곧 시작하는 예약 있음"
        else -> "알림"
    }
    Box(
        modifier
            .size(32.dp)
            .clip(TasShapes.RadiusMd)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(bellIcon, contentDescription = null, tint = TasColors.DarkGray, modifier = Modifier.size(18.dp))
        if (count > 0) {
            Text(
                if (count > 9) "9+" else "$count",
                color = TasColors.White,
                fontSize = TasSizes.TinyFont,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .background(TasColors.DangerFill, CircleShape)
                    .padding(horizontal = 4.dp),
            )
        } else if (dot) {
            // 웹은 --brand-fill. 어두운 화면에선 진한 보라가 바탕에 묻혀 '곧 시작' 태그 글자색(밝아짐)을 쓴다 — iOS 와 같다.
            Box(Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 6.dp).size(8.dp).background(TasColors.Brand, CircleShape))
        }
    }
}

/**
 * 웹 `NotificationCenter` 레이어. 안드로이드는 아직 게스트 전용이라 쓸 수 있는 칸은 '곧 시작하는 예약' 하나 —
 * 칸이 하나라 종류 탭도 없다(웹 `tabs.length > 2`). 예약 요청·네이버·쿠폰/회원권 만료는 로그인 때.
 */
@Composable
fun NotificationCenterSheet(
    upcoming: Upcoming,
    customerNames: Map<Int, String>,
    assignees: Map<Int, Assignee>,
    assigneeColors: Map<Int, String>,
    onOpenReservation: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val shown = if (upcoming.soon.isEmpty()) listOfNotNull(upcoming.next) else upcoming.soon.take(UPCOMING_MAX_SHOWN)
    DetailSheet(title = "알림", onDismiss = onDismiss) {
        // 웹 StyledSectionHead — 회색 띠 + 진한 글씨
        Text(
            if (upcoming.soon.isEmpty() && upcoming.next != null) "다음 예약" else "곧 시작하는 예약",
            color = TasColors.Black,
            fontSize = TasSizes.SmallFont,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .clip(TasShapes.RadiusMd)
                .background(TasColors.Gray2)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics { heading() },
        )
        if (shown.isEmpty()) {
            Text(
                "오늘 남은 예약이 없습니다",
                color = TasColors.DarkGray2,
                fontSize = TasSizes.SmallFont,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            )
        } else {
            Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                shown.forEach { item ->
                    UpcomingRow(item, customerNames, assignees, assigneeColors) {
                        onDismiss()
                        onOpenReservation(item.reservation.id)
                    }
                }
            }
        }
    }
}

/** 웹 renderUpcoming(StyledItem) — 연한 파랑 줄, 위에 '20분 후'·시각·고객·서비스, 아래 담당자. 누르면 그 예약 상세. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpcomingRow(
    item: UpcomingReservation,
    customerNames: Map<Int, String>,
    assignees: Map<Int, Assignee>,
    assigneeColors: Map<Int, String>,
    onClick: () -> Unit,
) {
    val r = item.reservation
    val name = customerNames[r.customerId]?.takeIf { it.isNotBlank() } ?: "고객 #${r.customerId}"
    val assignee = r.assigneeId?.let { assignees[it] }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(TasShapes.RadiusMd)
            .background(TasColors.NotificationUnreadBg)
            .clickable(role = Role.Button, onClickLabel = "예약 상세 열기", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            LabelBadge(untilLabel(item.minutesUntil), LabelTone.Purple, fontSize = TasSizes.TinyFont)
            Text(r.startTime, color = TasColors.Black, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.Bold)
            Text(name, color = TasColors.Black, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
            if (r.service.isNotBlank()) Text(r.service, color = TasColors.DarkGray, fontSize = TasSizes.SmallFont)
        }
        if (assignee != null) AssigneeLabel(assignee.name, hexColor(assigneeColors[assignee.id] ?: UNASSIGNED_COLOR))
    }
}

// 웹 NotificationCenter 종 svg(viewBox 24, 선 2) 경로 그대로.
private val bellIcon = svgLineIcon(24f, listOf("M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9", "M13.73 21a2 2 0 0 1-3.46 0"), strokeWidth = 2f)
