package kr.co.takeaseat.app.ui.revenue

import kr.co.takeaseat.app.ui.components.ButtonLine
import kr.co.takeaseat.app.ui.components.lineBox
import android.app.DatePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.REVENUE_MAX_RANGE_DAYS
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.RevenueDay
import kr.co.takeaseat.app.domain.RevenueFilterMode
import kr.co.takeaseat.app.domain.RevenueQuickRange
import kr.co.takeaseat.app.domain.RevenueRange
import kr.co.takeaseat.app.domain.RevenueSummary
import kr.co.takeaseat.app.domain.StoreSale
import kr.co.takeaseat.app.domain.UNASSIGNED_COLOR
import kr.co.takeaseat.app.domain.buildAssigneeColorMap
import kr.co.takeaseat.app.domain.buildServiceColorMap
import kr.co.takeaseat.app.domain.clampRevenueRange
import kr.co.takeaseat.app.domain.defaultRevenueRange
import kr.co.takeaseat.app.domain.firstVisitDates
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.isNewCustomerVisit
import kr.co.takeaseat.app.domain.moveRevenueRange
import kr.co.takeaseat.app.domain.quickRangeOf
import kr.co.takeaseat.app.domain.quickRevenueRange
import kr.co.takeaseat.app.domain.revenueSummary
import kr.co.takeaseat.app.domain.servicePriceTotal
import kr.co.takeaseat.app.domain.sortAssignees
import kr.co.takeaseat.app.domain.storeLabels
import kr.co.takeaseat.app.ui.calendar.ReservationDetailSheet
import kr.co.takeaseat.app.ui.calendar.ReservationInfoCard
import kr.co.takeaseat.app.ui.calendar.arrowLeft
import kr.co.takeaseat.app.ui.calendar.arrowRight
import kr.co.takeaseat.app.ui.components.PageHero
import kr.co.takeaseat.app.ui.components.ScreenHeader
import kr.co.takeaseat.app.ui.components.SystemDialogEffect
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.ui.components.svgLineIcon
import kr.co.takeaseat.app.ui.customers.CustomerDetailSheet
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes

/** 웹 RevenueViewTab(모바일) — 그래프 탭은 웹 모바일도 숨긴다. */
private enum class RevenueView { All, List }

/**
 * 웹 `/settings/revenue` 모바일 — 헤더 · 소개(REVENUE) · 필터(빠른 기간 · 기간 · 담당자 · 보기 · 집계) · 요약 카드 · 일별 목록.
 * 그래프는 웹 모바일(≤640)도 숨겨 없다. 일별 목록을 펼친 예약 카드 → 예약 상세, 충전 줄 고객명 → 고객 상세.
 * 요약 카드·날짜를 눌러 여는 상세 창은 11-2단계, 엑셀 내보내기는 다음.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RevenueScreen(store: GuestStore, modifier: Modifier = Modifier) {
    val snapshot by store.snapshot.collectAsState()
    val today = LocalDate.now()
    val initial = remember { defaultRevenueRange(today) }
    // LocalDate 는 저장 못 해 날짜 글자로 — 회전해도 기간이 남는다
    var startKey by rememberSaveable { mutableStateOf(initial.start.toString()) }
    var endKey by rememberSaveable { mutableStateOf(initial.end.toString()) }
    val range = RevenueRange(LocalDate.parse(startKey), LocalDate.parse(endKey))
    fun setRange(next: RevenueRange) = clampRevenueRange(next).let { startKey = it.start.toString(); endKey = it.end.toString() }
    var assigneeId by rememberSaveable { mutableStateOf<Int?>(null) }
    var mode by rememberSaveable { mutableStateOf(RevenueFilterMode.Booked) }
    var view by rememberSaveable { mutableStateOf(RevenueView.All) }
    var showHint by rememberSaveable { mutableStateOf(false) }
    var openDates by rememberSaveable { mutableStateOf(listOf<String>()) }
    var detailId by rememberSaveable { mutableStateOf<Int?>(null) }
    var customerDetailId by rememberSaveable { mutableStateOf<Int?>(null) }

    val zone = remember { ZoneId.systemDefault() }
    val summary = remember(snapshot, range, assigneeId, mode) { revenueSummary(snapshot, range, assigneeId, mode, zone) }
    val assignees = remember(snapshot.assignees) { sortAssignees(snapshot.assignees) }
    val assigneeNames = remember(snapshot.assignees) { snapshot.assignees.associate { it.id to it.name } }
    val assigneeColors = remember(snapshot.assignees) { buildAssigneeColorMap(snapshot.assignees) }
    val serviceColorMap = remember(snapshot.services, snapshot.categoryBaseColors) { buildServiceColorMap(snapshot.services, snapshot.categoryBaseColors) }
    val customerNames = remember(snapshot.customers) { snapshot.customers.associate { it.id to it.name } }
    val labels = remember(snapshot.shopType) { storeLabels(snapshot.shopType) }
    val firstVisits = remember(snapshot.reservations) { firstVisitDates(snapshot.reservations) }

    Column(modifier.fillMaxSize()) {
        ScreenHeader("매출")
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item {
                // 웹: 헤더 아래 8, 소개 아래 18
                PageHero("REVENUE", "기간별 매출 현황과 ${labels.assignee}별 실적을 확인합니다.", PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 18.dp))
            }
            stickyHeader {
                RevenueFilters(
                    range = range,
                    quick = quickRangeOf(range, today),
                    onQuick = { setRange(quickRevenueRange(it, today)) },
                    onMove = { forward -> setRange(moveRevenueRange(range, forward, today)) },
                    onStart = { setRange(range.copy(start = it)) },
                    onEnd = { setRange(range.copy(end = it)) },
                    assignees = assignees.map { it.id to it.name },
                    assigneeId = assigneeId,
                    onAssignee = { assigneeId = it },
                    view = view,
                    onView = { view = it },
                    mode = mode,
                    onMode = { mode = it },
                    showHint = showHint,
                    onToggleHint = { showHint = !showHint },
                )
            }
            if (view == RevenueView.All) {
                item { KpiGrid(summary) }
            }
            item {
                DailyCard(
                    summary = summary,
                    openDates = openDates,
                    onToggle = { key -> openDates = if (key in openDates) openDates - key else openDates + key },
                ) { day ->
                    day.reservations.forEach { r ->
                        val id = r.assigneeId?.takeIf { it != 0 }
                        ReservationInfoCard(
                            reservation = r,
                            serviceColorMap = serviceColorMap,
                            assigneeName = id?.let { assigneeNames[it] } ?: "미지정",
                            assigneeColor = hexColor(id?.let { assigneeColors[it] } ?: UNASSIGNED_COLOR),
                            displayPrice = r.price ?: servicePriceTotal(r.service, snapshot.services),
                            onClick = { detailId = r.id },
                            customerName = customerNames[r.customerId] ?: "고객 미지정",
                            isNewCustomer = isNewCustomerVisit(firstVisits[r.customerId], r.date),
                            showDate = false, // 날짜 줄 아래라 카드엔 또 적지 않는다(웹과 같다)
                            onCustomerClick = { customerDetailId = r.customerId },
                        )
                    }
                    day.sales.forEach { sale -> SaleItem(sale, customerNames[sale.customerId]) { customerDetailId = sale.customerId } }
                }
            }
            item { Box(Modifier.height(16.dp)) }
        }
    }

    // 고객 상세가 예약 상세보다 먼저 — 창은 그린 순서대로 쌓인다(고객 상세에서 연 예약이 위에)
    customerDetailId?.let { id -> CustomerDetailSheet(store, id, onReservationClick = { detailId = it.id }, onDismiss = { customerDetailId = null }) }
    detailId?.let { id ->
        snapshot.reservations.firstOrNull { it.id == id }?.let { r: Reservation ->
            ReservationDetailSheet(
                store = store,
                reservation = r,
                isNewCustomer = isNewCustomerVisit(firstVisits[r.customerId], r.date),
                serviceColorMap = serviceColorMap,
                labels = labels,
                onDismiss = { detailId = null },
            )
        }
    }
}

/** 웹 RevenueFilters(모바일) — 위에 붙어 있다. 줄 사이 6(빠른 기간 ~ 기간은 줄바꿈 8), 아래 1px 선. */
@Composable
private fun RevenueFilters(
    range: RevenueRange,
    quick: RevenueQuickRange?,
    onQuick: (RevenueQuickRange) -> Unit,
    onMove: (forward: Boolean) -> Unit,
    onStart: (LocalDate) -> Unit,
    onEnd: (LocalDate) -> Unit,
    assignees: List<Pair<Int, String>>,
    assigneeId: Int?,
    onAssignee: (Int?) -> Unit,
    view: RevenueView,
    onView: (RevenueView) -> Unit,
    mode: RevenueFilterMode,
    onMode: (RevenueFilterMode) -> Unit,
    showHint: Boolean,
    onToggleHint: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(TasColors.White).padding(start = 10.dp, end = 10.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(RevenueQuickRange.Today to "오늘", RevenueQuickRange.Week to "7일", RevenueQuickRange.Month to "30일").forEach { (q, text) ->
                FilterChip(text, quick == q, ChipStyle.Quick) { onQuick(q) }
            }
        }
        Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RangeArrow(arrowLeft, "이전 기간") { onMove(false) }
            // 웹 날짜 입력은 서로 ±365일 안으로만 고른다(min/max)
            DateField("시작일", range.start, bounds = range.end, onPick = onStart, modifier = Modifier.weight(1f))
            DateField("종료일", range.end, bounds = range.start, onPick = onEnd, modifier = Modifier.weight(1f))
            RangeArrow(arrowRight, "다음 기간") { onMove(true) }
        }
        // 담당자 탭 — 가로 스크롤(웹 padding 2 0)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip("전체", assigneeId == null, ChipStyle.Assignee) { onAssignee(null) }
            assignees.forEach { (id, name) -> FilterChip(name, assigneeId == id, ChipStyle.Assignee) { onAssignee(id) } }
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip("전체", view == RevenueView.All, ChipStyle.View) { onView(RevenueView.All) }
                FilterChip("매출 일별목록", view == RevenueView.List, ChipStyle.View) { onView(RevenueView.List) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip("예약완료매출", mode == RevenueFilterMode.Completed, ChipStyle.Mode) { onMode(RevenueFilterMode.Completed) }
                    FilterChip("예약매출", mode == RevenueFilterMode.Booked, ChipStyle.Mode) { onMode(RevenueFilterMode.Booked) }
                }
                InfoButton(showHint, onToggleHint)
            }
        }
        if (showHint) {
            Text(
                if (mode == RevenueFilterMode.Completed) "예약완료 매출은 예약상태가 예약완료인 건만 집계합니다. 결제완료 금액은 결제수단과 금액이 입력된 내역만 합산됩니다."
                else "예약매출은 취소, 노쇼를 제외한 전체 예약 기준으로 집계합니다. 결제완료 금액은 예약완료 기준으로만 집계됩니다.",
                color = TasColors.DarkGray2,
                fontSize = TasSizes.XSmallFont,
                lineHeight = TasSizes.XSmallFont * 1.45f,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(HintBg).padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(TasColors.LightGray))
    }
}

/** 웹 칩 네 가지 — 빠른 기간(13 둥글게, 파랑 채움) · 담당자(10, 파랑 채움) · 보기(6, 검정 채움) · 집계(10, 옅은 파랑 + 파랑 테두리·굵게). */
private enum class ChipStyle(val radius: Int, val horizontal: Int) { Quick(13, 11), Assignee(10, 11), View(6, 8), Mode(10, 11) }

/** 하나만 고르는 칩 — 낭독기엔 라디오(고름/안 고름)로 읽힌다. 30 높이(웹과 같다, WCAG 24 이상). */
@Composable
private fun FilterChip(text: String, selected: Boolean, style: ChipStyle, onClick: () -> Unit) {
    val shape = RoundedCornerShape(style.radius.dp)
    val (bg, border, color) = when {
        !selected -> Triple(TasColors.White, TasColors.LightGray, TasColors.DarkGray)
        style == ChipStyle.View -> Triple(TasColors.Black, TasColors.Black, TasColors.White)
        style == ChipStyle.Mode -> Triple(TasColors.InfoBg, TasColors.Blue, TasColors.Blue)
        else -> Triple(TasColors.Blue, TasColors.Blue, TasColors.White)
    }
    Box(
        Modifier
            .heightIn(min = 30.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = style.horizontal.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = TasSizes.SmallFont, maxLines = 1,
            fontWeight = if (selected && style == ChipStyle.Mode) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.lineBox(ButtonLine, TasSizes.SmallFont))
    }
}

/** 웹 StyledInfoButton — 20 동그라미 'i'. 누르는 자리는 24(WCAG 2.5.8). */
@Composable
private fun InfoButton(expanded: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(24.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "집계기준 설명"; stateDescription = if (expanded) "펼침" else "접힘" },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(TasColors.White).border(1.dp, TasColors.LightGray, CircleShape), contentAlignment = Alignment.Center) {
            Text("i", color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont, fontWeight = FontWeight.Bold, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

/** 웹 StyledRangeNavButton — 테두리 없는 화살표(DirectionIcon). 누르는 자리 32. */
@Composable
private fun RangeArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = TasColors.Icon, modifier = Modifier.size(24.dp)) }
}

/** 웹 `<input type="date">`(모바일) — 32 높이, 6 둥글게, 12 글자. 누르면 시스템 날짜 선택(상대 날짜 ±365일 안). */
@Composable
private fun DateField(label: String, date: LocalDate, bounds: LocalDate, onPick: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var picking by rememberSaveable { mutableStateOf(false) }
    if (picking) {
        SystemDialogEffect(onDismissed = { picking = false }) { context ->
            DatePickerDialog(context, { _, y, m, d -> onPick(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).apply {
                val zone = ZoneId.systemDefault()
                datePicker.minDate = bounds.minusDays(REVENUE_MAX_RANGE_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
                datePicker.maxDate = bounds.plusDays(REVENUE_MAX_RANGE_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
            }
        }
    }
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier
            .height(32.dp)
            .dropShadow(shape, Shadow(radius = 2.dp, color = FieldShadow, offset = DpOffset(0.dp, 1.dp)))
            .clip(shape)
            .background(TasColors.White)
            .border(1.dp, TasColors.LightGray, shape)
            .semantics { contentDescription = "$label, ${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일" }
            .clickable(role = Role.Button, onClickLabel = "날짜 선택") { picking = true }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("%d. %02d. %02d.".format(date.year, date.monthValue, date.dayOfMonth), color = TasColors.DarkGray, fontSize = TasSizes.SmallFont,
            maxLines = 1, modifier = Modifier.weight(1f).clearAndSetSemantics {})
        Icon(CalendarIcon, contentDescription = null, tint = TasColors.DarkGray, modifier = Modifier.size(14.dp))
    }
}

/** 웹 RevenueKpiGrid(모바일 2열) — 테두리 1 옅은 회색, 10 둥글게, 흰 → #f9fbff, 그림자 0 8 20 5%. */
@Composable
private fun KpiGrid(summary: RevenueSummary) {
    val cards = listOf(
        Triple("총 매출", formatPrice(summary.total), summary.salesTotal.takeIf { it > 0 }?.let { "회원권·충전 ${formatPrice(it)} 포함" }),
        Triple("예약 건수", "${summary.count}건", null),
        Triple("신규 고객 수", "${summary.newCustomerCount}명", null),
        Triple("재방문 고객 수", "${summary.returningCustomerCount}명", null),
        Triple("결제완료", formatPrice(summary.paidTotal), null),
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cards.chunked(2).forEach { row ->
            // 웹 grid — 한 줄의 카드는 같은 높이(설명 붙은 '총 매출' 옆 카드도 늘어난다)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { (label, value, sub) -> KpiCard(label, value, sub) }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RowScope.KpiCard(label: String, value: String, sub: String?) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .card(shape, CardShadowKpi)
            .background(Brush.verticalGradient(listOf(TasColors.White, KpiGradientEnd)))
            .border(1.dp, TasColors.LightGray, shape)
            // 낭독은 '총 매출, 18,000원' 한 덩어리로
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
        Text(value, color = TasColors.Black, fontSize = BigFont, fontWeight = FontWeight.Bold, maxLines = 1)
        if (sub != null) Text(sub, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
    }
}

/** 웹 StyledDailyCard + RevenueDailyList — 날짜 줄(누르면 펼침) · 펼치면 그날 예약 카드·충전 줄 · 아래 기간 합계. */
@Composable
private fun DailyCard(
    summary: RevenueSummary,
    openDates: List<String>,
    onToggle: (String) -> Unit,
    expanded: @Composable (RevenueDay) -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
            .card(shape, CardShadowDaily)
            .background(TasColors.White)
            .border(1.dp, TasColors.LightGray, shape)
            .padding(10.dp),
    ) {
        if (summary.days.isEmpty()) {
            Text("등록된 데이터가 없습니다", color = TasColors.DarkGray2, fontSize = TasSizes.MediumFont, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 54.dp))
            return@Column
        }
        summary.days.forEach { day ->
            val key = day.date.toString()
            val open = key in openDates
            Column(Modifier.fillMaxWidth().drawBehind {
                drawRect(TasColors.Black10, topLeft = Offset(0f, size.height - 1.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
            }) {
                DayHeader(day, open) { onToggle(key) }
                if (open) {
                    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { expanded(day) }
                }
            }
        }
        // 웹 StyledRevenueSummary — n건 · 합계(16 굵게 파랑). 웹은 화면 아래에 붙어 있다(여기선 목록 끝).
        Row(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .border(1.dp, TasColors.LightGray, RoundedCornerShape(10.dp))
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${summary.count}건", color = TasColors.DarkGray2, fontSize = TasSizes.MediumFont, modifier = Modifier.weight(1f))
            Text(formatPrice(summary.total), color = TasColors.Blue, fontSize = TasSizes.LargeFont, fontWeight = FontWeight.Bold)
        }
    }
}

private val WEEKDAYS = listOf("월", "화", "수", "목", "금", "토", "일")

/** 웹 StyledDayHeader — 날짜(10월 20일 / 2026년 (화), 폭 96) · n건 · 합계 · 펼침 표시. */
@Composable
private fun DayHeader(day: RevenueDay, open: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { stateDescription = if (open) "펼침" else "접힘" }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.width(96.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${day.date.monthValue}월 ${day.date.dayOfMonth}일", color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.Medium)
            Text("${day.date.year}년 (${WEEKDAYS[day.date.dayOfWeek.value - 1]})", color = TasColors.DarkGray2, fontSize = TasSizes.TinyFont)
        }
        Box(Modifier.weight(1f))
        Text("${day.count}건", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont)
        Text(formatPrice(day.total), color = TasColors.Black, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.Medium, maxLines = 1)
        // 웹 StyledExpandIcon — 8×8 꺾쇠(2px), 펼치면 위로
        Box(
            Modifier
                .size(14.dp)
                .rotate(if (open) 180f else 0f)
                .drawBehind {
                    val s = 5.dp.toPx()
                    val c = Offset(size.width / 2, size.height / 2 - s / 3)
                    val w = 2.dp.toPx()
                    drawLine(TasColors.DarkGray2, Offset(c.x - s, c.y - s / 2), Offset(c.x, c.y + s / 2), strokeWidth = w)
                    drawLine(TasColors.DarkGray2, Offset(c.x, c.y + s / 2), Offset(c.x + s, c.y - s / 2), strokeWidth = w)
                },
        )
    }
}

/** 웹 StyledSaleItem — 적립금 충전 · 고객명(누르면 고객 상세) 결제수단 / 금액. */
@Composable
private fun SaleItem(sale: StoreSale, customerName: String?, onCustomerClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TasColors.BgSubtle98)
            .border(1.dp, SaleBorder, shape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(sale.label, color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(customerName ?: "고객 미지정", color = TasColors.Blue, fontSize = TasSizes.XSmallFont,
                    modifier = Modifier.heightIn(min = 24.dp).wrapContentHeight(Alignment.CenterVertically).clickable(role = Role.Button, onClickLabel = "고객 정보 보기", onClick = onCustomerClick))
                Text(sale.method ?: "결제수단 없음", color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
            }
        }
        Text(formatPrice(sale.amount), color = TasColors.Black, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.SemiBold)
    }
}

private fun Modifier.card(shape: Shape, shadow: Shadow) = dropShadow(shape, shadow).clip(shape)

private val BigFont = TasSizes.LargeFont * 1.125f                                       // --big-font 18
private val HintBg = Color(0xF6, 0xF8, 0xFC)                                            // #f6f8fc
private val KpiGradientEnd = Color(0xF9, 0xFB, 0xFF)                                    // #f9fbff
private val FieldShadow = Color(15, 23, 42).copy(alpha = 0.04f)                         // 날짜 칸 0 1 2
private val SaleBorder = Color(148, 163, 184).copy(alpha = 0.18f)
private val CardShadowKpi = Shadow(radius = 20.dp, color = Color(15, 23, 42).copy(alpha = 0.05f), offset = DpOffset(0.dp, 8.dp))
private val CardShadowDaily = Shadow(radius = 24.dp, color = Color(15, 23, 42).copy(alpha = 0.05f), offset = DpOffset(0.dp, 10.dp))
private val CalendarIcon = svgLineIcon(24f, listOf("M4 6h16v14H4z", "M4 10h16", "M8 3v4", "M16 3v4"), strokeWidth = 2f, name = "calendar")
