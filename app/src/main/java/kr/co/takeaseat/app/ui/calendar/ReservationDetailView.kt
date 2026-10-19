package kr.co.takeaseat.app.ui.calendar

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import kr.co.takeaseat.app.domain.Customer
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.ReservationStatus
import kr.co.takeaseat.app.domain.StoreLabels
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.formatTel
import kr.co.takeaseat.app.domain.hasCompletedPayment
import kr.co.takeaseat.app.domain.paymentEntriesOf
import kr.co.takeaseat.app.domain.showsNaverDepositLine
import kr.co.takeaseat.app.ui.components.AssigneeLabel
import kr.co.takeaseat.app.ui.components.LabelBadge
import kr.co.takeaseat.app.ui.components.LabelTone
import kr.co.takeaseat.app.ui.components.NewCustomerBadge
import kr.co.takeaseat.app.ui.components.ServiceChipList
import kr.co.takeaseat.app.ui.components.StatusBadge
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.ui.components.readableTextOn
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 `ReservationViewSection` 이식 — 예약 상세(보기). 행 차례·조건이 웹과 같다(웹 dl: 항목명 60 + 값, 간격 8/12).
 * 회원권·쿠폰 행은 로그인 전용이라 게스트에선 없다.
 */
@Composable
fun ReservationDetailView(
    reservation: Reservation,
    customer: Customer?,
    isNewCustomer: Boolean,
    displayPrice: Int,
    assigneeName: String,
    assigneeColor: Color,
    serviceColorMap: Map<String, String>,
    labels: StoreLabels,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (reservation.status) {
            ReservationStatus.CANCELLED -> DetailRow("상태") { StatusBadge("cancelled", "취소") }
            ReservationStatus.NOSHOW -> DetailRow("상태") { StatusBadge("noshow", "노쇼") }
            ReservationStatus.COMPLETED -> DetailRow("상태") { StatusBadge("completed", "완료") }
            ReservationStatus.REQUESTED -> DetailRow("상태") { StatusBadge("requested", "확정대기") }
        }
        DetailRow("날짜") { DetailText(reservation.date) }
        DetailRow("시간") { DetailText("${reservation.startTime} ~ ${reservation.endTime}") }
        DetailRow(labels.service) { ServiceChipList(reservation.service, serviceColorMap) }
        DetailRow("가격") { DetailText(formatPrice(displayPrice)) }
        DetailRow("결제") { PaymentValue(reservation) }
        DetailRow("고객명") {
            // shortcut: 웹은 누르면 고객 정보가 열리는 파란 링크 — 고객 화면이 생기면 같은 모양·동작으로 바꾼다.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isNewCustomer) NewCustomerBadge()
                DetailText(customer?.name ?: "-")
            }
        }
        DetailRow("연락처") {
            val tel = customer?.tel.orEmpty()
            if (tel.isEmpty()) {
                DetailText("-")
            } else {
                // 웹 tel: 링크 — 누르면 전화 앱(번호만 채운다, 바로 걸지 않는다)
                DetailText(
                    formatTel(tel),
                    Modifier.clickable(role = Role.Button, onClickLabel = "전화 걸기") {
                        // 전화 앱이 없는 기기(와이파이 태블릿·업무 프로필)에선 열 곳이 없다 — 앱이 죽지 않게 넘어간다.
                        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))) }
                    },
                )
            }
        }
        val memoTags = customer?.memoTags.orEmpty()
        if (memoTags.isNotEmpty()) {
            DetailRow("고객 메모") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    memoTags.forEach { tag ->
                        val bg = hexColor(tag.color)
                        Text(
                            tag.text,
                            color = readableTextOn(bg),
                            fontSize = TasSizes.XSmallFont,
                            fontWeight = FontWeight.SemiBold,
                            // 웹 태그는 최소 22 에 글이 길면 줄을 바꿔 커진다 — 한 줄 상자(lineBox)를 쓰지 않는다.
                            modifier = Modifier
                                .background(bg, TasShapes.Chip)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .heightIn(min = 18.dp)
                                .wrapContentHeight(Alignment.CenterVertically),
                        )
                    }
                }
            }
        }
        reservation.memo?.trim()?.takeIf { it.isNotEmpty() }?.let { memo -> DetailRow("요청사항") { DetailText(memo) } }
        DetailRow("적립금") { DetailText(formatPrice(customer?.points ?: 0)) }
        DetailRow(labels.assignee) { AssigneeLabel(assigneeName, assigneeColor) }
        DetailRow("예약경로") {
            val bookingId = reservation.naverBookingId?.takeIf { it.isNotEmpty() }
            if (bookingId != null) NaverBookingInfo(bookingId, reservation.naverBookingUrl)
            else LabelBadge(reservation.channel ?: "전화예약", LabelTone.Info, fontSize = TasSizes.TinyFont)
        }
    }
}

/** 결제 — 배지는 위 한 줄, 아래에 종류·금액 목록(웹 v0.64.1). */
@Composable
private fun PaymentValue(reservation: Reservation) {
    val entries = paymentEntriesOf(reservation)
    val paid = hasCompletedPayment(reservation)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusBadge(if (paid) "paid" else "unpaid", if (paid) "결제완료" else "미결제")
        if (paid && entries.isEmpty()) DetailText("미입력", color = TasColors.DarkGray2)
        if (entries.isNotEmpty()) {
            Column(Modifier.widthIn(max = 220.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                entries.forEach { entry ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        DetailText(entry.method, Modifier.weight(1f))
                        DetailText(formatPrice(entry.amount), style = LocalTextStyle.current.merge(TabularNums))
                    }
                }
            }
        }
    }
    // 웹은 결제 묶음(아래 2px) 다음 형제라 간격이 2 — 위 묶음의 6 간격에 넣지 않는다.
    if (showsNaverDepositLine(reservation, entries)) NaverDepositLine(reservation.naverDeposit ?: 0, Modifier.padding(top = 2.dp))
}

/** 웹 StyledNaverDepositLine — 연두 상자 + N 로고 + "네이버 예약 확정 예약금 N원". */
@Composable
private fun NaverDepositLine(deposit: Int, modifier: Modifier = Modifier) {
    val shape = TasShapes.RadiusMd
    Row(
        modifier
            .background(Color(0xFFF0FAF4), shape)
            .border(1.dp, Color(0xFFD4EDDA), shape)
            .padding(horizontal = 8.dp, vertical = 4.dp), // 웹 패딩 3/7 + 바깥 테두리 1
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(NaverLogo, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(14.dp))
        Text("네이버 예약 확정 예약금", color = Color(0xFF333333), fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
        Text(formatPrice(deposit), color = Color(0xFF333333), fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
    }
}

private val NaverLogo: ImageVector = ImageVector.Builder(defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
    .addPath(PathParser().parsePathString("M4 0H16A4 4 0 0 1 20 4V16A4 4 0 0 1 16 20H4A4 4 0 0 1 0 16V4A4 4 0 0 1 4 0Z").toNodes(), fill = SolidColor(Color(0xFF03C75A)))
    .addPath(PathParser().parsePathString("M11.7 13.1L8.1 8.2V13.1H6V6h2.4l3.5 4.8V6H14v7.1h-2.3z").toNodes(), fill = SolidColor(Color.White))
    .build()

/** 웹 `NaverBookingInfo` + 안내 — 네이버예약 태그 · 예약번호 · 바로가기, 아래 초록 안내. */
@Composable
private fun NaverBookingInfo(bookingId: String, bookingUrl: String?) {
    val context = LocalContext.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
        LabelBadge("네이버예약", LabelTone.Brand, fontSize = TasSizes.TinyFont)
        Text(bookingId, color = TasColors.DarkGray, fontSize = TasSizes.XSmallFont)
        if (!bookingUrl.isNullOrEmpty()) {
            Text(
                "바로가기 ↗",
                color = Color(0xFF00803C), // 웹 바로가기 — 흰 바탕 대비 4.5 이상(브랜드 #03C75A 는 2.3)
                fontSize = TasSizes.XSmallFont,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(role = Role.Button, onClickLabel = "네이버 예약 열기") {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bookingUrl))) }
                },
            )
        }
    }
    Text(
        "네이버예약의 실제 변경/취소는 스마트플레이스 통해서 가능합니다.",
        color = Color(0xFF0F5132),
        fontSize = TasSizes.SmallFont,
        lineHeight = 1.45.em,
        // 웹 word-break: keep-all — 한글을 글자마다 끊지 않는다(안드로이드 13+, 그 아래는 기본 줄바꿈)
        style = LocalTextStyle.current.copy(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)),
        modifier = Modifier
            .padding(top = 8.dp)
            // 따로 읽는다 — 행에 합치면 바로가기보다 먼저 읽혀 보이는 차례와 어긋난다.
            .semantics(mergeDescendants = true) {}
            .background(Color(3, 199, 90).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
    )
}

/** 웹 dt(60, 13 500 #525252) + dd(13). 낭독기는 항목명·값을 한 번에 읽는다(누를 수 있는 값은 따로 남는다). */
@Composable
private fun DetailRow(term: String, gap: androidx.compose.ui.unit.Dp = 12.dp, value: @Composable ColumnScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(gap)) {
        Text(
            term,
            color = TasColors.DarkGray,
            fontSize = TasSizes.MediumFont,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(60.dp),
        )
        Column(Modifier.weight(1f), content = value)
    }
}

@Composable
private fun DetailText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
) {
    Text(text, color = color, fontSize = TasSizes.MediumFont, style = style, modifier = modifier)
}

/**
 * 웹 `ReservationStaticDiffSection`(취소·노쇼 확인) / 예약 전환 확인창 본문 — 색 문구 + 서비스·날짜·시간·고객명.
 * [boxed] = 취소·노쇼(옅은 회색 상자, 간격 4/10). 예약 전환은 상자 없이 간격 8/12, 문구 아래 16.
 */
@Composable
internal fun ReservationConfirmSummary(
    message: String,
    color: Color,
    reservation: Reservation,
    customerName: String,
    labels: StoreLabels,
    boxed: Boolean = true,
) {
    Text(
        message,
        color = color,
        fontSize = 14.sp, // --modal-message-font
        fontWeight = FontWeight.SemiBold,
        lineHeight = (14 * 1.55).sp,
        // 버튼을 누르면 본문이 확인 문구로 바뀐다 — 낭독기가 바뀐 걸 알게 읽어 준다.
        modifier = Modifier.padding(bottom = if (boxed) 10.dp else 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
    )
    Column(
        (if (boxed) Modifier.fillMaxWidth().background(TasColors.Black10, TasShapes.RadiusMd).padding(12.dp) else Modifier.fillMaxWidth()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val gap = if (boxed) 10.dp else 12.dp
        DetailRow(labels.service, gap) { DetailText(reservation.service) }
        DetailRow("날짜", gap) { DetailText(reservation.date) }
        DetailRow("시간", gap) { DetailText("${reservation.startTime} ~ ${reservation.endTime}") }
        DetailRow("고객명", gap) { DetailText(customerName) }
    }
}
