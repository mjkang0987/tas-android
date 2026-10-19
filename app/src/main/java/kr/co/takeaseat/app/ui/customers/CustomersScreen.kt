package kr.co.takeaseat.app.ui.customers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import kr.co.takeaseat.app.domain.MEMO_TAG_COLORS
import kr.co.takeaseat.app.domain.addMemoTag
import kr.co.takeaseat.app.domain.canAddMemoTag
import kr.co.takeaseat.app.domain.removeMemoTag
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.Customer
import kr.co.takeaseat.app.domain.CustomerMemoTag
import kr.co.takeaseat.app.domain.CustomerReservationGroup
import kr.co.takeaseat.app.domain.CustomerStats
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.ServiceItem
import kr.co.takeaseat.app.domain.servicePriceTotal
import kr.co.takeaseat.app.domain.UNASSIGNED_COLOR
import kr.co.takeaseat.app.domain.buildAssigneeColorMap
import kr.co.takeaseat.app.domain.buildServiceColorMap
import kr.co.takeaseat.app.domain.customerListOrder
import kr.co.takeaseat.app.domain.customerStats
import kr.co.takeaseat.app.domain.firstVisitDates
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.formatTel
import kr.co.takeaseat.app.domain.groupedCustomerReservations
import kr.co.takeaseat.app.domain.isNewCustomerVisit
import kr.co.takeaseat.app.domain.searchCustomers
import kr.co.takeaseat.app.domain.storeLabels
import kr.co.takeaseat.app.domain.toDateKey
import kr.co.takeaseat.app.ui.calendar.ReservationDetailSheet
import kr.co.takeaseat.app.ui.calendar.ReservationInfoCard
import kr.co.takeaseat.app.ui.components.ServiceChipList
import kr.co.takeaseat.app.ui.components.StatusBadge
import kr.co.takeaseat.app.ui.components.TasTextField
import kr.co.takeaseat.app.ui.components.backgroundForWhiteText
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 `/address`(고객 명단) 모바일 — 헤더 · 소개(CUSTOMER) · 검색(이름·초성·연락처·메모) · 고객 행(펼치면 메모 태그 + 예약 묶음 카드).
 * 예약 카드를 누르면 캘린더와 같은 예약 상세 시트.
 * 펼친 행의 메모 태그는 추가·삭제(웹 AddressCustomerTags — 한 번에 한 고객).
 * shortcut: 고객 추가·병합(선택 체크), 적립금 충전, 고객명 → 고객 상세는 다음 조각(10-2~).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomersScreen(store: GuestStore, modifier: Modifier = Modifier) {
    val snapshot by store.snapshot.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var openIds by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var detailId by rememberSaveable { mutableStateOf<Int?>(null) }
    // 웹 address.tsx — 메모 태그는 한 번에 한 고객만 편집, 입력·고른 색은 화면에 하나
    var tagEditingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var tagInput by rememberSaveable { mutableStateOf("") }
    var tagColor by rememberSaveable { mutableStateOf(MEMO_TAG_COLORS[0].first) }
    val today = LocalDate.now().toDateKey()

    val ordered = remember(snapshot.customers) { customerListOrder(snapshot.customers) }
    val matches = remember(ordered, query) { searchCustomers(ordered, query) }
    val byCustomer = remember(snapshot.reservations) { snapshot.reservations.groupBy { it.customerId } }
    // 고객별 통계는 예약이 바뀔 때만 — 검색어를 칠 때마다 행마다 다시 세지 않게
    val statsById = remember(byCustomer, today) { byCustomer.mapValues { (_, list) -> customerStats(list, today) } }
    val assigneeNames = remember(snapshot.assignees) { snapshot.assignees.associate { it.id to it.name } }
    val serviceColorMap = remember(snapshot.services, snapshot.categoryBaseColors) { buildServiceColorMap(snapshot.services, snapshot.categoryBaseColors) }
    val assigneeColors = remember(snapshot.assignees) { buildAssigneeColorMap(snapshot.assignees) }
    val labels = remember(snapshot.shopType) { storeLabels(snapshot.shopType) }

    Column(modifier.fillMaxSize()) {
        // 웹 Header(모바일) — 높이 48, 제목 18 굵게 #525252, 아래 1px
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .drawBehind { drawRect(TasColors.LightGray, topLeft = Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx())) }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text("고객 명단", color = TasColors.DarkGray, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() })
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item {
                // 웹 PageHero — 영문 eyebrow + 설명, 아래 18
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CUSTOMER", color = TasColors.Blue, fontSize = TasSizes.XSmallFont, fontWeight = FontWeight.Bold, letterSpacing = 0.18.em)
                    Text("고객 정보, 예약 이력, 메모 태그를 관리합니다.", color = TasColors.DarkGray2, fontSize = TasSizes.LargeFont)
                }
            }
            stickyHeader {
                // 웹 검색(위로 붙어 있다) — 32 높이 칸
                Box(Modifier.fillMaxWidth().background(TasColors.White).padding(horizontal = 20.dp, vertical = 4.dp)) {
                    TasTextField(
                        label = "고객 검색",
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "고객명, 연락처, 메모 검색",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        showLabel = false,
                        compact = true,
                    )
                }
            }
            if (matches.isEmpty()) {
                item {
                    Text("등록된 데이터가 없습니다", color = TasColors.DarkGray2, fontSize = TasSizes.MediumFont,
                        modifier = Modifier.fillMaxWidth().padding(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            items(matches, key = { it.customer.id }) { match ->
                val customer = match.customer
                val reservations = byCustomer[customer.id].orEmpty()
                val open = customer.id in openIds
                CustomerRow(
                    customer = customer,
                    stats = statsById[customer.id] ?: customerStats(emptyList(), today),
                    matchedTags = match.matchedTags,
                    open = open,
                    onToggle = { openIds = if (open) openIds - customer.id else openIds + customer.id },
                    serviceColorMap = serviceColorMap,
                ) {
                    CustomerExpanded(
                        reservations, today, serviceColorMap, assigneeColors, assigneeNames, snapshot.services,
                        memoSection = {
                            MemoTagSection(
                                tags = customer.memoTags.orEmpty(),
                                editing = tagEditingId == customer.id,
                                input = tagInput,
                                color = tagColor,
                                onInput = { tagInput = it },
                                onColor = { tagColor = it },
                                onAdd = {
                                    // 못 더하면(빈 글자·같은 태그) 입력도 그대로 — 웹과 같다
                                    if (customer.canAddMemoTag(tagInput)) {
                                        val text = tagInput
                                        store.update { addMemoTag(it, customer.id, text, tagColor) }
                                        tagInput = ""
                                    }
                                },
                                onRemove = { text -> store.update { removeMemoTag(it, customer.id, text) } },
                                onStart = { tagEditingId = customer.id; tagInput = "" },
                                onFinish = { tagEditingId = null; tagInput = "" },
                            )
                        },
                    ) { detailId = it.id }
                }
            }
            item { Box(Modifier.height(16.dp)) }
        }
    }

    // 예약 카드 → 캘린더와 같은 상세 시트
    detailId?.let { id ->
        snapshot.reservations.firstOrNull { it.id == id }?.let { r ->
            ReservationDetailSheet(
                store = store,
                reservation = r,
                isNewCustomer = isNewCustomerVisit(firstVisitDates(snapshot.reservations)[r.customerId], r.date),
                serviceColorMap = serviceColorMap,
                labels = labels,
                onDismiss = { detailId = null },
            )
        }
    }
}

/** 웹 AddressCustomerSummary(모바일 3줄) — 이름·연락처 / 최근 서비스 / 적립금 + 상태별 건수. 행 전체를 누르면 펼친다. */
@Composable
private fun CustomerRow(
    customer: Customer,
    stats: CustomerStats,
    matchedTags: List<CustomerMemoTag>,
    open: Boolean,
    onToggle: () -> Unit,
    serviceColorMap: Map<String, String>,
    expanded: @Composable () -> Unit,
) {
    val context = LocalContext.current
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .drawBehind {
                // 웹: 접힘 1px 연회색, 펼침 2px 검정
                val h = (if (open) 2 else 1).dp.toPx()
                drawRect(if (open) TasColors.Black else TasColors.LightGray, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
            },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = if (open) "펼침" else "접힘" }
                .padding(top = 10.dp, bottom = 10.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(customer.name, color = TasColors.Black, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                // 웹 tel: 링크 — 누르면 전화 앱(번호만 채움)
                if (customer.tel.isNotBlank()) {
                    Text(
                        formatTel(customer.tel),
                        color = TasColors.DarkGray,
                        fontSize = 14.sp,
                        modifier = Modifier
                            // 누르는 영역 높이 24 이상(WCAG 2.5.8) — 글자 줄(약 19)보다 조금 키운다
                            .heightIn(min = 24.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .clickable(role = Role.Button, onClickLabel = "전화 걸기") {
                                runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.tel}"))) }
                            },
                    )
                }
              }
                // 웹 StyledArrow — 오른쪽 작은 삼각형(펼치면 아래로)
                Box(
                    Modifier
                        .offset(x = 16.dp) // 웹은 행 오른쪽 여백(16) 바깥 끝에 붙는다
                        .size(10.dp)
                        .rotate(if (open) 90f else 0f)
                        .clearAndSetSemantics {}
                        .drawBehind {
                            val p = Path().apply { moveTo(size.width * 0.25f, 0f); lineTo(size.width * 0.75f, size.height / 2); lineTo(size.width * 0.25f, size.height); close() }
                            drawPath(p, TasColors.DarkGray)
                        },
                )
            }
            if (matchedTags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    matchedTags.forEach { MemoChip(it, small = true) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("최근 서비스", color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.Medium)
                if (stats.recentService != null) ServiceChipList(stats.recentService, serviceColorMap) else Text("-", fontSize = TasSizes.SmallFont, color = TasColors.Black)
            }
            // 자리가 모자라면(좁은 폰·큰 글자) 상태 배지가 아랫줄로 내려간다(웹 flex-wrap)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("적립금 ", color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.Medium)
                    Text(formatPrice(customer.points ?: 0), color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge("booked", "예약(${stats.count(CustomerReservationGroup.Booked)})")
                    StatusBadge("cancelled", "취소(${stats.count(CustomerReservationGroup.Cancelled)})")
                    StatusBadge("completed", "완료(${stats.count(CustomerReservationGroup.Completed)})")
                    StatusBadge("noshow", "노쇼(${stats.count(CustomerReservationGroup.Noshow)})")
                }
            }
        }
        if (open) Box(Modifier.padding(bottom = 12.dp)) { expanded() }
    }
}

/** 펼친 행 — 메모 태그(없으면 '메모 없음'), 예약 묶음(예약·완료·취소·노쇼, 카드는 미래 → 과거). */
@Composable
private fun CustomerExpanded(
    reservations: List<Reservation>,
    today: String,
    serviceColorMap: Map<String, String>,
    assigneeColors: Map<Int, String>,
    assigneeNames: Map<Int, String>,
    catalog: List<ServiceItem>,
    memoSection: @Composable () -> Unit,
    onReservationClick: (Reservation) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        memoSection()
        val groups = groupedCustomerReservations(reservations, today)
        if (groups.isEmpty()) {
            Text("등록된 데이터가 없습니다", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        groups.forEach { (group, list) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${group.label} (${list.size})", color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
                list.forEach { r ->
                    val id = r.assigneeId?.takeIf { it != 0 }
                    ReservationInfoCard(
                        reservation = r,
                        serviceColorMap = serviceColorMap,
                        assigneeName = id?.let { assigneeNames[it] } ?: "미지정",
                        assigneeColor = hexColor(id?.let { assigneeColors[it] } ?: UNASSIGNED_COLOR),
                        displayPrice = r.price ?: servicePriceTotal(r.service, catalog),
                        onClick = { onReservationClick(r) },
                    )
                }
            }
        }
    }
}

/**
 * 웹 AddressCustomerTags — 보기: 태그(없으면 '메모 없음') + '추가'/'수정'.
 * 편집: 태그마다 ✕ → 색 8개 → 입력(자동 초점, 엔터 = 추가) + '추가' + '완료'.
 */
@Composable
private fun MemoTagSection(
    tags: List<CustomerMemoTag>,
    editing: Boolean,
    input: String,
    color: String,
    onInput: (String) -> Unit,
    onColor: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
) {
    if (!editing) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tags.isEmpty()) {
                Text("메모 없음", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont, modifier = Modifier.weight(1f))
            } else {
                FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { MemoChip(it) }
                }
            }
            MemoButton(if (tags.isEmpty()) "추가" else "수정", onStart, description = if (tags.isEmpty()) "메모 태그 추가" else "메모 태그 수정")
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { tag -> MemoChip(tag, onRemove = { onRemove(tag.text) }) }
            }
        }
        // 웹 18 동그라미(고른 색은 2px 진회색 테두리) — 누르는 영역은 24
        Row(Modifier.selectableGroup()) {
            MEMO_TAG_COLORS.forEach { (hex, name) ->
                val selected = color == hex
                Box(
                    Modifier
                        .size(24.dp)
                        .selectable(selected, role = Role.RadioButton) { onColor(hex) }
                        .semantics { contentDescription = "태그 색 $name" },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .background(hexColor(hex), CircleShape)
                            .border(2.dp, if (selected) TasColors.DarkGray else Color.Transparent, CircleShape),
                    )
                }
            }
        }
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() } // 웹 autoFocus
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TasTextField(
                label = "메모 태그 입력",
                value = input,
                onValueChange = onInput,
                placeholder = "태그 입력",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                // 엔터 = 추가, 키보드는 그대로(이어서 칠 수 있게 — 웹과 같다)
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
                showLabel = false,
                compact = true,
                modifier = Modifier.weight(1f, fill = false).widthIn(max = 200.dp).focusRequester(focus),
            )
            MemoButton("추가", onAdd)
            MemoButton("완료", onFinish)
        }
    }
}

/** 웹 StyledMemoButton — 28 높이, 1px 옅은 회색 테두리, 4 둥글게, 흰 바탕, 10 진회색. */
@Composable
private fun MemoButton(text: String, onClick: () -> Unit, description: String = text) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .heightIn(min = 28.dp)
            .clip(shape)
            .background(TasColors.White)
            .border(1.dp, TasColors.LightGray, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = TasColors.DarkGray, fontSize = TasSizes.TinyFont, modifier = Modifier.clearAndSetSemantics {}) }
}

/**
 * 메모 태그 — 웹 ColorTag(soft, 4 둥글게). 검색에 걸린 태그는 작게(10, 위아래 1).
 * [onRemove] 가 있으면 편집 중 — 글자 뒤 ✕(누르는 영역 24, 웹처럼 흐리게 하지 않아 흰 글자 대비 유지).
 */
@Composable
private fun MemoChip(tag: CustomerMemoTag, small: Boolean = false, onRemove: (() -> Unit)? = null) {
    if (onRemove != null) {
        Row(Modifier.background(backgroundForWhiteText(hexColor(tag.color)), TasShapes.RadiusSm).padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tag.text, color = TasColors.White, fontSize = TasSizes.XSmallFont, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Box(
                Modifier
                    .size(24.dp)
                    .clickable(role = Role.Button, onClick = onRemove)
                    .semantics { contentDescription = "${tag.text} 태그 삭제" },
                contentAlignment = Alignment.Center,
            ) { Text("✕", color = TasColors.White, fontSize = TasSizes.TinyFont, modifier = Modifier.clearAndSetSemantics {}) }
        }
        return
    }
    Text(
        tag.text,
        color = TasColors.White,
        fontSize = if (small) TasSizes.TinyFont else TasSizes.XSmallFont,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .background(backgroundForWhiteText(hexColor(tag.color)), TasShapes.RadiusSm)
            .padding(horizontal = if (small) 6.dp else 8.dp, vertical = if (small) 1.dp else 2.dp),
    )
}
