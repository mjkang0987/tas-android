package kr.co.takeaseat.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.ReservationStatus
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.hasCompletedPayment
import kr.co.takeaseat.app.ui.components.AssigneeLabel
import kr.co.takeaseat.app.ui.components.ServiceChipList
import kr.co.takeaseat.app.ui.components.StatusBadge
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes

private val CardInk = Color(0xFF0F172A) // 웹 카드 시간·가격 #0f172a
private val CardShape = RoundedCornerShape(8.dp)

/** 웹 getReservationState — 카드 오른쪽 상태 배지. */
private fun cardState(r: Reservation): Pair<String, String> = when {
    r.status == ReservationStatus.CANCELLED -> "cancelled" to "취소"
    r.status == ReservationStatus.NOSHOW -> "noshow" to "노쇼"
    r.status == ReservationStatus.REQUESTED -> "requested" to "신청"
    hasCompletedPayment(r) -> "paid" to "결제완료"
    else -> "booked" to "예약"
}

/**
 * 웹 `ui/ReservationInfoCard`(고객 명단 변형: 날짜·상태·가격 표시, 시간 두 줄, 담당자 색 왼쪽 막대) —
 * 왼쪽 시작·종료·날짜, 가운데 서비스 칩 + 담당자, 오른쪽 상태 배지 위 가격. 취소·노쇼는 회색 빗금.
 * [displayPrice] 는 상세와 같은 값(가격이 없으면 서비스 가격 합) — 웹 카드는 0원으로 보여 상세와 달랐다.
 */
@Composable
internal fun ReservationInfoCard(
    reservation: Reservation,
    serviceColorMap: Map<String, String>,
    assigneeName: String,
    assigneeColor: Color,
    displayPrice: Int,
    onClick: () -> Unit,
) {
    val inactive = reservation.status == ReservationStatus.CANCELLED || reservation.status == ReservationStatus.NOSHOW
    val (stateType, stateLabel) = cardState(reservation)
    val border = if (inactive) TasColors.Gray else assigneeColor.copy(alpha = 0x44 / 255f)
    val bar = if (inactive) TasColors.Gray else assigneeColor
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .then(
                if (inactive) Modifier.background(TasColors.Gray2).inactiveHatch(reservation.status == ReservationStatus.NOSHOW)
                else Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.96f), assigneeColor.copy(alpha = 0x12 / 255f)))),
            )
            .drawBehind {
                val thin = 1.dp.toPx()
                drawRoundRect(border, topLeft = Offset(thin / 2, thin / 2), size = Size(size.width - thin, size.height - thin), cornerRadius = CornerRadius(8.dp.toPx()), style = Stroke(thin))
                drawRect(bar, size = Size(4.dp.toPx(), size.height)) // 왼쪽 4 막대(웹 border-left 4px)
            }
            .clickable(role = Role.Button, onClick = onClick)
            // 카드 한 장을 한 번에 읽는다
            .clearAndSetSemantics {
                contentDescription = "${reservation.date} ${reservation.startTime}~${reservation.endTime} ${reservation.service} $assigneeName $stateLabel ${formatPrice(displayPrice)}"
                onClick { onClick(); true }
            }
            .padding(start = 9.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column {
            Text(reservation.startTime, color = CardInk, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.Bold, lineHeight = (12 * 1.25).sp)
            Text(reservation.endTime, color = CardInk, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.Bold, lineHeight = (12 * 1.25).sp)
            Text(reservation.date, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ServiceChipList(reservation.service, serviceColorMap, muted = inactive)
            AssigneeLabel(assigneeName, assigneeColor, modifier = Modifier.padding(top = 2.dp))
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusBadge(stateType, stateLabel)
            Text(formatPrice(displayPrice), color = CardInk, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.Bold)
        }
    }
}
