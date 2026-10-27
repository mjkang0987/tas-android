package kr.co.takeaseat.app.ui.revenue

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.takeaseat.app.domain.MetricCustomer
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.RevenueMetric
import kr.co.takeaseat.app.domain.RevenueMetricDetail
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.formatTel
import kr.co.takeaseat.app.domain.formatVisitGap
import kr.co.takeaseat.app.ui.components.ActionButton
import kr.co.takeaseat.app.ui.components.DetailSheet
import kr.co.takeaseat.app.ui.components.NewCustomerBadge
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 `RevenueMetricModal`(모바일 바텀시트) — 요약 카드를 누르면. 총 매출·예약 건수·결제완료는 예약 카드 목록(날짜가 위),
 * 신규·재방문은 고객 카드 목록. 아래 줄 = 요약 + 닫기. 예약·고객을 누르면 그 상세가 위로 뜬다(부르는 쪽이 이 창 뒤에 그린다).
 */
@Composable
internal fun RevenueMetricSheet(
    metric: RevenueMetric,
    subtitle: String?,
    detail: RevenueMetricDetail,
    onCustomerClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    reservationCard: @Composable (Reservation) -> Unit,
) {
    DetailSheet(
        title = metric.title,
        subtitle = subtitle,
        onDismiss = onDismiss,
        footer = {
            Text(detail.summary, color = TasColors.Black, fontSize = TasSizes.LargeFont, modifier = Modifier.align(Alignment.CenterVertically))
            ActionButton("닫기", onClick = onDismiss)
        },
    ) {
        val customerMetric = metric == RevenueMetric.New || metric == RevenueMetric.Returning
        val empty = if (customerMetric) detail.customers.isEmpty() else detail.reservations.isEmpty()
        if (empty) {
            Text("등록된 데이터가 없습니다", color = TasColors.DarkGray2, fontSize = TasSizes.MediumFont, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 54.dp))
            return@DetailSheet
        }
        // 웹 StyledMetricBodyInner — 위 10, 고객 카드 사이 12 · 예약 카드 사이 8
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(if (customerMetric) 12.dp else 8.dp)) {
            if (customerMetric) {
                detail.customers.forEach { MetricCustomerCard(it, isNew = metric == RevenueMetric.New) { onCustomerClick(it.customer.id) } }
            } else {
                detail.reservations.forEach { reservationCard(it) }
            }
        }
    }
}

/** 웹 StyledCustomerRow — 신규 배지·이름(재방문은 간격 배지) / 방문일 / 연락처·적립금·최근 방문일(·이전 방문). 누르면 고객 상세. */
@Composable
private fun MetricCustomerCard(item: MetricCustomer, isNew: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(10.dp)
    val c = item.customer
    Column(
        Modifier
            .fillMaxWidth()
            .dropShadow(shape, Shadow(radius = 18.dp, color = Color(15, 23, 42).copy(alpha = 0.05f), offset = DpOffset(0.dp, 8.dp)))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(TasColors.Surface.copy(alpha = 0.96f), TasColors.BgSubtle98)))
            .border(1.dp, CardBorder, shape)
            .clickable(role = Role.Button, onClickLabel = "고객 정보 보기", onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 모바일은 이름 줄과 방문일이 위아래로
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isNew) NewCustomerBadge()
                Text(c.name, color = TasColors.CardInk, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.Bold, lineHeight = (13 * 1.35).sp, modifier = Modifier.weight(1f, fill = false))
                if (!isNew && item.prevVisitDate != null) {
                    Text(
                        formatVisitGap(item.visitDate, item.prevVisitDate),
                        color = TasColors.Blue,
                        fontSize = TasSizes.XSmallFont,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(percent = 50)).background(TasColors.InfoBg).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(item.visitDate, color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
        }
        // 웹 StyledCustomerInfoGrid — 줄 바꿈 칸, 굵은 이름표 + 값
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            InfoItem("연락처") {
                if (c.tel.isNotBlank()) {
                    Text(formatTel(c.tel), color = TasColors.DarkGray, fontSize = 14.sp,
                        modifier = Modifier.heightIn(min = 24.dp).wrapContentHeight(Alignment.CenterVertically).clickable(role = Role.Button, onClickLabel = "전화 걸기") {
                            runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.tel}"))) }
                        })
                } else {
                    Text("-", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont)
                }
            }
            InfoItem("적립금") { Text(formatPrice(c.points ?: 0), color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont) }
            InfoItem("최근 방문일") { Text(item.visitDate, color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont) }
            if (!isNew && item.prevVisitDate != null) InfoItem("이전 방문") { Text(item.prevVisitDate, color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont) }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
        value()
    }
}

private val CardBorder = Color(148, 163, 184).copy(alpha = 0.18f)
