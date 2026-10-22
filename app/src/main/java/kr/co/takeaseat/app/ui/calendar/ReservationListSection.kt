package kr.co.takeaseat.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.takeaseat.app.domain.LocalDbSnapshot
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.UNASSIGNED_COLOR
import kr.co.takeaseat.app.domain.bookedRevenueTotal
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.isNewCustomerVisit
import kr.co.takeaseat.app.domain.servicePriceTotal
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.ui.components.lineBox
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

private val GroupTitleBorder = Color(148, 163, 184).copy(alpha = 0.18f) // 웹 StyledDateTitle 테두리
private val GroupTitleInk = Color(0xFF111111)

/**
 * 웹 `ReservationListSection` — 월·연 보기에서 고른 날·달의 예약 목록. 레이어가 아니라 격자 아래 자리다.
 * 회색 머리줄([title] · N건 · 합계(취소·노쇼·신청 뺌) · [onCreate] 가 있으면 ＋) + 예약 카드.
 * [grouped](연 보기의 달 목록)면 날짜마다 "2026-10-20 (1)" 알약 제목을 단다 — 날짜 목록은 머리줄이 이미 날짜라 달지 않는다.
 * [listKey] 가 바뀌면 맨 위부터 — 앞 목록에서 내린 자리가 남으면 이른 예약을 놓친다.
 */
@Composable
internal fun ReservationListSection(
    title: String,
    listKey: Any,
    reservations: List<Reservation>,
    grouped: Boolean,
    lazy: Boolean,
    snapshot: LocalDbSnapshot,
    customerNames: Map<Int, String>,
    firstVisit: Map<Int, String>,
    assigneeColors: Map<Int, String>,
    serviceColorMap: Map<String, String>,
    onReservationClick: (Int) -> Unit,
    onCustomerClick: (Int) -> Unit,
    onCreate: (() -> Unit)?,
    modifier: Modifier,
) {
    val assigneeNames = remember(snapshot.assignees) { snapshot.assignees.associate { it.id to it.name } }
    val total = remember(reservations, snapshot.services) { bookedRevenueTotal(reservations, snapshot.services) }
    val groups = remember(reservations, grouped) {
        if (grouped) reservations.groupBy { it.date }.toList() else listOf("" to reservations)
    }
    Column(
        modifier
            .fillMaxWidth()
            .drawBehind { drawRect(TasColors.LightGray, size = Size(size.width, 1.dp.toPx())) } // 웹 border-top
            .padding(top = 1.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().background(TasColors.Gray2).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = TasColors.Black, fontSize = TasSizes.TinyFont, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            Text(
                "${reservations.size}건 · ${formatPrice(total)}",
                color = TasColors.DarkGray2,
                fontSize = TasSizes.SmallFont,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
            if (onCreate != null) {
                // 웹 StyledHeaderAdd — 글리프 18, 누르는 자리 24. 헤더 ＋예약 은 늘 오늘로 열려 고른 날엔 이걸 쓴다.
                Box(
                    Modifier
                        .padding(start = 2.dp)
                        .size(24.dp)
                        .clickable(role = Role.Button, onClick = onCreate)
                        .clearAndSetSemantics {
                            contentDescription = "$title 예약 추가"
                            role = Role.Button
                            onClick { onCreate(); true }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = TasColors.Brand, fontSize = 18.sp, modifier = Modifier.lineBox(18.sp, 18.sp))
                }
            }
        }
        if (reservations.isEmpty()) {
            Text(
                "$title 예약이 없습니다.",
                color = TasColors.DarkGray2,
                fontSize = TasSizes.MediumFont,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
            return@Column
        }
        val card: @Composable (Reservation) -> Unit = { r ->
            val id = r.assigneeId?.takeIf { it != 0 }
            val name = customerNames[r.customerId]
            ReservationInfoCard(
                reservation = r,
                serviceColorMap = serviceColorMap,
                assigneeName = id?.let { assigneeNames[it] } ?: "미지정",
                assigneeColor = hexColor(id?.let { assigneeColors[it] } ?: UNASSIGNED_COLOR),
                displayPrice = r.price ?: servicePriceTotal(r.service, snapshot.services),
                onClick = { onReservationClick(r.id) },
                customerName = name ?: "-",
                isNewCustomer = isNewCustomerVisit(firstVisit[r.customerId], r.date),
                showDate = false,
                onCustomerClick = name?.let { { onCustomerClick(r.customerId) } },
            )
        }
        // 웹 StyledDateGroup(모바일) — 그룹 안 4, 그룹 사이 8. 맨 아래 8 + 10.
        // 묶음 목록은 위 6 을 첫 알약이 흰 바탕째로 갖는다 — 목록 여백으로 두면 붙은 알약 위로 카드가 비친다.
        val padding = PaddingValues(start = 8.dp, end = 8.dp, top = if (grouped) 0.dp else 6.dp, bottom = 18.dp)
        val gap = if (grouped) 4.dp else 2.dp
        if (lazy) {
            key(listKey) {
                LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(gap)) {
                    groups.forEachIndexed { index, (date, items) ->
                        if (grouped) {
                            // 웹은 sticky 알약 — 내려도 어느 날짜인지 보이게 위에 붙는다
                            stickyHeader(key = "d$date") { GroupTitle(date, items.size, first = index == 0) }
                        }
                        items(items, key = { it.id }) { card(it) }
                    }
                }
            }
        } else {
            Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(gap)) {
                groups.forEachIndexed { index, (date, items) ->
                    if (grouped) GroupTitle(date, items.size, first = index == 0)
                    items.forEach { card(it) }
                }
            }
        }
    }
}

/** 웹 StyledDateTitle — "2026-10-20 (1)" 알약(높이 30·좌우 12·옅은 테두리). */
@Composable
private fun GroupTitle(date: String, count: Int, first: Boolean) {
    // 위에 붙었을 때 둥근 모서리·위 여백 틈으로 지나가는 카드가 비치지 않게 흰 띠째로 붙는다.
    // 웹은 반투명 흰 + 흐림(backdrop-filter)인데 흐림이 없으니 가리는 쪽을 택한다.
    Box(Modifier.fillMaxWidth().background(TasColors.White).padding(top = if (first) 6.dp else 4.dp)) { // 그룹 사이 8 = 간격 4 + 4
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 30.dp)
                .border(1.dp, GroupTitleBorder, TasShapes.Chip)
                .padding(horizontal = 12.dp)
                // 제목은 안쪽 글자 노드 — 합쳐야 머리글 이동에서 날짜가 읽힌다
                .semantics(mergeDescendants = true) { heading() },
            contentAlignment = Alignment.CenterStart,
        ) {
            Text("$date ($count)", color = GroupTitleInk, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.Bold)
        }
    }
}
