package kr.co.takeaseat.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt
import kr.co.takeaseat.app.domain.CardDetail
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.ReservationStatus
import kr.co.takeaseat.app.domain.cardDetailForHeight
import kr.co.takeaseat.app.ui.components.NewCustomerBadge
import kr.co.takeaseat.app.ui.components.ServiceChipList
import kr.co.takeaseat.app.ui.components.WarningPillBadge
import kr.co.takeaseat.app.ui.components.lineBox
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 `TimelineReservationCard` + `ui/Buttons.tsx` `StyledReserveButton` 이식.
 * 상태는 글자로 알리지 않는다 — 취소·노쇼는 빗금+회색+취소선(노쇼는 붉은 빗금), 확정대기는 딤+배지, 결제완료는 표시 없음.
 */
@Composable
fun ReservationCard(
    reservation: Reservation,
    height: Dp,
    color: Color,
    customerName: String?,
    isNewCustomer: Boolean,
    serviceColorMap: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    val status = reservation.status
    val cancelled = status == ReservationStatus.CANCELLED || status == ReservationStatus.NOSHOW
    val noshow = status == ReservationStatus.NOSHOW
    val detail = cardDetailForHeight(height.value)
    val textColor = if (cancelled) TasColors.DarkGray2 else Color.Black
    val decoration = if (cancelled) TextDecoration.LineThrough else TextDecoration.None
    val borderColor = if (cancelled) TasColors.Gray else color

    Box(
        modifier
            .clip(TasShapes.RadiusSm)
            .then(if (cancelled) Modifier.background(TasColors.Gray2).inactiveHatch(noshow) else Modifier.background(color.copy(alpha = 0x12 / 255f)))
            .cardBorder(borderColor)
            // 한 문장으로만 읽는다 — 안의 시각·이름·칩까지 합치면 같은 내용을 두 번 읽는다.
            .clearAndSetSemantics { contentDescription = reservationLabel(reservation, customerName, isNewCustomer) },
    ) {
        // 웹 카드는 <button> 이라 내용이 세로 가운데에 놓인다. 왼쪽 4px 막대 + 상하좌우 2px(모바일).
        Column(
            Modifier
                .matchParentSize()
                .padding(start = 4.dp + 2.dp, end = 2.dp + 1.dp, top = 2.dp + 1.dp, bottom = 2.dp + 1.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (detail == CardDetail.Full) {
                Text(
                    reservation.startTime,
                    color = textColor,
                    fontSize = TasSizes.SmallFont,
                    fontWeight = FontWeight.SemiBold,
                    style = LocalTextStyle.current.merge(TabularNums),
                    maxLines = 1,
                    modifier = Modifier.lineBox(14.5.sp), // 웹 실측
                )
                if (!customerName.isNullOrEmpty()) {
                    // 웹 .detail 줄 = 신규 배지 높이 14
                    Row(Modifier.height(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isNewCustomer) NewCustomerBadge()
                        Text(
                            customerName,
                            color = textColor,
                            fontSize = TasSizes.TinyFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.lineBox(11.5.sp),
                        )
                    }
                }
                ServiceChipList(reservation.service, serviceColorMap, muted = cancelled)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (isNewCustomer) NewCustomerBadge()
                    Text(
                        text = buildAnnotatedString {
                            // compact 까지만 시작시간 — name 단계는 이름 한 줄도 빠듯하다(웹 계약)
                            if (detail == CardDetail.Compact) {
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")) {
                                    append(reservation.startTime)
                                }
                                append(" ")
                            }
                            append(customerName?.takeIf { it.isNotEmpty() } ?: "고객")
                            if (detail == CardDetail.Compact && reservation.service.isNotEmpty()) append(" · ${reservation.service}")
                        },
                        color = textColor,
                        fontSize = TasSizes.TinyFont,
                        lineHeight = 1.2.em,
                        textDecoration = decoration,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().lineBox(12.sp), // 10 × 1.2
                    )
                }
            }
        }
        if (status == ReservationStatus.REQUESTED) {
            // 아직 수락 전인 신청 — 있지만 정해지지 않았다
            Box(Modifier.matchParentSize().background(Color.White.copy(alpha = 0.72f)))
            if (height >= 34.dp) WarningPillBadge("확정대기", Modifier.align(Alignment.Center))
        }
    }
}

/** 숫자 폭 고정 — 시각이 흔들리지 않게(웹 font-variant-numeric: tabular-nums). */
internal val TabularNums = TextStyle(fontFeatureSettings = "tnum")

private fun reservationLabel(r: Reservation, customerName: String?, isNewCustomer: Boolean): String {
    val state = when (r.status) {
        ReservationStatus.CANCELLED -> ", 취소"
        ReservationStatus.NOSHOW -> ", 노쇼"
        ReservationStatus.REQUESTED -> ", 확정대기"
        else -> ""
    }
    val newBadge = if (isNewCustomer) "신규 " else ""
    return "${r.startTime}~${r.endTime} $newBadge${customerName ?: "고객"} ${r.service}$state"
}

/** 1px 테두리 + 왼쪽 4px 막대(웹 border 1px · border-left-width 4px). */
private fun Modifier.cardBorder(color: Color) = drawBehind {
    val thin = 1.dp.toPx()
    val radius = 4.dp.toPx()
    drawRoundRect(color, topLeft = Offset(thin / 2, thin / 2), size = Size(size.width - thin, size.height - thin),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius), style = Stroke(thin))
    drawRect(color, size = Size(4.dp.toPx(), size.height))
}

/**
 * 웹 `inactiveHatch` — repeating-linear-gradient(-45deg, 줄 0 6px, 투명 6px 12px).
 * 취소는 무채색, 노쇼는 붉은 기운(매출이 빈 자리라 눈에 걸려야 한다).
 */
private fun Modifier.inactiveHatch(noshow: Boolean) = drawBehind {
    val stripe = if (noshow) Color(201, 58, 48).copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.05f)
    val step = 12.dp.toPx() / sqrt(2f)
    drawRect(
        Brush.linearGradient(
            0f to stripe, 0.5f to stripe, 0.5f to Color.Transparent, 1f to Color.Transparent,
            start = Offset.Zero,
            end = Offset(-step, -step), // -45deg = 왼쪽 위 방향
            tileMode = TileMode.Repeated,
        ),
    )
}
