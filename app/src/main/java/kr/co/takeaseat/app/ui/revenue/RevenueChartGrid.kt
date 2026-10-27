package kr.co.takeaseat.app.ui.revenue

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.takeaseat.app.domain.ChartDetailKey
import kr.co.takeaseat.app.domain.chartPercent
import kr.co.takeaseat.app.domain.Customer
import kr.co.takeaseat.app.domain.RevenueCharts
import kr.co.takeaseat.app.domain.RevenueRange
import kr.co.takeaseat.app.domain.UNASSIGNED_COLOR
import kr.co.takeaseat.app.domain.compareAssigneeName
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.toDateKey
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes
import kotlin.math.roundToInt

/**
 * 웹 `RevenueChartGrid` — 넓은 화면(웹 641px 이상)의 매출 분석 카드 6장. 휴대폰 폭은 웹도 숨긴다.
 * 웹 900px 이하는 1열, 넘으면 2열(추이·결제수단은 두 칸). 카드·막대·범례를 누르면 차트 상세([onDetail]),
 * 노쇼 카드의 고객 이름은 고객 상세. 웹 막대 툴팁은 마우스를 올릴 때만이라 옮기지 않았다 — 낭독은 막대마다 '날짜 금액'.
 */
@Composable
internal fun RevenueChartGrid(
    charts: RevenueCharts,
    range: RevenueRange,
    assigneeSelected: Boolean,
    assigneeLabel: String,
    assigneeNames: Map<Int, String>,
    rawAssigneeColors: Map<Int, String?>,
    customers: Map<Int, Customer>,
    onDetail: (ChartDetailKey) -> Unit,
    onCustomerClick: (Int) -> Unit,
) {
    // 웹 900px 기준은 창(뷰포트) 폭 — 본문 폭이 아니라
    val twoColumns = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } > 900.dp
    val trend = @Composable { TrendCard(charts, range, twoColumns, onDetail) }
    val payment = @Composable { PaymentCard(charts, twoColumns, onDetail) }
    val channel = @Composable { m: Modifier -> ChannelCard(charts, twoColumns, m, onDetail) }
    val assignee = @Composable { m: Modifier -> AssigneeCard(charts, assigneeSelected, assigneeLabel, twoColumns, m, onDetail) }
    val cancellation = @Composable { m: Modifier ->
        CancellationCard(charts, assigneeLabel, assigneeNames, rawAssigneeColors, twoColumns, m, onDetail)
    }
    val noshow = @Composable { m: Modifier -> NoshowCard(charts, customers, twoColumns, m, onDetail, onCustomerClick) }

    // 웹 StyledDashboard gap 12 — 요약 카드 아래 여백 8 에 4 를 더한다
    Column(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        trend()
        payment()
        if (twoColumns) {
            // 웹 grid — 한 줄의 두 카드는 같은 높이
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                channel(Modifier.weight(1f).fillMaxHeight())
                assignee(Modifier.weight(1f).fillMaxHeight())
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cancellation(Modifier.weight(1f).fillMaxHeight())
                noshow(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            channel(Modifier.fillMaxWidth())
            assignee(Modifier.fillMaxWidth())
            cancellation(Modifier.fillMaxWidth())
            noshow(Modifier.fillMaxWidth())
        }
    }
}

/** 웹 StyledChartCard — 둥글기 10·안 여백 10·칸 사이 12·연회색 테두리·그림자. 높이 하한은 2열일 때만(웹 900px 이하 auto). */
@Composable
private fun ChartCard(
    title: String,
    meta: String,
    minHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .heightIn(min = minHeight)
            .card(shape, CardShadowDaily)
            .background(TasColors.Surface)
            .border(1.dp, TasColors.LightGray, shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = TasColors.DarkGray, fontSize = TasSizes.LargeFont, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(meta, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont, textAlign = TextAlign.End)
        }
        content()
    }
}

/** 웹 StyledChartEmpty — 회색 바탕에 '등록된 데이터가 없습니다'(남은 높이를 채운다). */
@Composable
private fun ColumnScope.ChartEmpty(fill: Boolean) {
    Box(
        // 웹 StyledChartEmpty 는 StyledEmpty 여백을 0 으로 덮는다 — 1열은 글자 한 줄 띠, 2열은 남은 높이를 채운다
        (if (fill) Modifier.weight(1f) else Modifier).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(TasColors.Gray2),
        contentAlignment = Alignment.Center,
    ) { Text("등록된 데이터가 없습니다", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont, textAlign = TextAlign.Center) }
}

/** 웹 StyledColorSwatch — 10 원. */
@Composable
private fun Swatch(color: Color) = Box(Modifier.size(10.dp).clip(CircleShape).background(color))

// ── ① 기간별 매출 추이 ──

@Composable
private fun TrendCard(charts: RevenueCharts, range: RevenueRange, twoColumns: Boolean, onDetail: (ChartDetailKey) -> Unit) {
    val from = range.from.toDateKey()
    val to = range.to.toDateKey()
    ChartCard("기간별 매출 추이", "$from ~ $to", if (twoColumns) 320.dp else 0.dp) {
        if (!charts.hasTrend) { ChartEmpty(fill = twoColumns); return@ChartCard }
        val boxShape = RoundedCornerShape(18.dp)
        // 웹 StyledTrendChartBox — 연보라 위 → 바탕 아래 그라데이션, 오른쪽 위 은은한 보라 빛
        Column(
            Modifier.fillMaxWidth().heightIn(min = 236.dp).clip(boxShape)
                .background(Brush.verticalGradient(listOf(TasColors.BrandBg, TasColors.Surface)))
                // 웹 radial-gradient(circle at top right, …, transparent 28%) — 반지름 = 대각선의 28%
                .drawBehind {
                    val radius = kotlin.math.hypot(size.width, size.height) * 0.28f
                    drawRect(Brush.radialGradient(listOf(TasBrandGlow, Color.Transparent), center = Offset(size.width, 0f), radius = radius))
                }
                .border(1.dp, TasBrandHairline, boxShape)
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // 웹 StyledYAxis — 64 폭, 최대·절반·0 을 눈금선 높이에 맞춰
                Box(Modifier.width(64.dp).height(190.dp)) {
                    YLabel(formatPrice(charts.chartMax), Alignment.TopStart)
                    YLabel(formatPrice((charts.chartMax / 2.0).roundToInt()), Alignment.CenterStart)
                    YLabel(formatPrice(0), Alignment.BottomStart)
                }
                TrendBars(charts, from, to, onDetail)
            }
        }
    }
}

@Composable
private fun BoxScope.YLabel(text: String, alignment: Alignment) {
    // 웹은 글자 가운데를 눈금선에 맞춘다(translateY(-50%)) — 위·아래 끝은 글자 높이 반만큼 밖으로
    val sign = when (alignment) { Alignment.TopStart -> -1; Alignment.BottomStart -> 1; else -> 0 }
    Text(text, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont, lineHeight = TasSizes.XSmallFont, maxLines = 1,
        modifier = Modifier.align(alignment).layout { measurable, constraints ->
            val p = measurable.measure(constraints)
            layout(p.width, p.height) { p.place(0, sign * p.height / 2) }
        })
}

@Composable
private fun RowScope.TrendBars(charts: RevenueCharts, from: String, to: String, onDetail: (ChartDetailKey) -> Unit) {
    val count = charts.series.size
    // 웹: 간격은 밀도에 맞춰(45 초과 1 · 20 초과 2 · 그 밖 4), 막대 6~48, 넘치면 가로 스크롤
    val gap = when { count > 45 -> 1.dp; count > 20 -> 2.dp; else -> 4.dp }
    val guide = TasColors.Black.copy(alpha = 0.06f)
    val max = charts.chartMax.toFloat()
    BoxWithConstraints(Modifier.weight(1f)) {
        val minContent = MinBar * count + gap * (count - 1)
        val contentWidth = maxOf(maxWidth, minContent)
        val slot = (contentWidth - gap * (count - 1)) / count
        val bar = slot.coerceIn(MinBar, MaxBar)
        Column(Modifier.clip(RoundedCornerShape(10.dp)).horizontalScroll(rememberScrollState()).width(contentWidth), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.fillMaxWidth().height(190.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    listOf(0f, 0.5f, 1f).forEach { ratio ->
                        val y = (size.height * ratio).coerceIn(0.5f, size.height - 0.5f)
                        drawLine(guide, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                }
                // 웹 justify-content: space-around — 막대 중심이 각 1/n 칸 가운데
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) {
                    charts.series.forEach { point ->
                        val ratio = point.total / max
                        val label = "${point.date.toDateKey()} ${formatPrice(point.total)}"
                        Box(
                            Modifier.width(bar).fillMaxHeight()
                                .clickable(role = Role.Button, onClickLabel = "상세 보기") { onDetail(ChartDetailKey.Date(point.date)) }
                                .semantics { contentDescription = label },
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (ratio > 0f) {
                                // 매출이 있는 날은 아주 작아도 3 — 0원인 날과 구분
                                // heightIn 이 먼저 — fillMaxHeight 뒤에 두면 고정된 높이를 못 넘어 최소값이 묻힌다
                                Box(Modifier.fillMaxWidth().heightIn(min = 3.dp).fillMaxHeight(ratio)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(TasColors.BrandFill))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(from.substring(5), color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
                Text(to.substring(5), color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
            }
        }
    }
}

// ── ② 결제수단 비중 · ③ 예약비중 (도넛) ──

/** 웹 StyledDonutChart — 150 원, 두께 22, 가운데 값·이름. 합이 0이면 회색 한 바퀴(#E5E7EB). */
@Composable
private fun Donut(segments: List<Pair<Color, Int>>, value: String, label: String, twoColumns: Boolean) {
    val sum = segments.sumOf { it.second }
    Box(
        Modifier.padding(horizontal = if (twoColumns) 50.dp else 10.dp, vertical = if (twoColumns) 25.dp else 10.dp).size(150.dp)
            .clearAndSetSemantics { contentDescription = "$label $value" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 22.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            if (sum <= 0) {
                drawArc(DonutEmpty, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            } else {
                // 웹 conic-gradient 는 12시에서 시계방향
                var start = -90f
                segments.forEach { (color, amount) ->
                    val sweep = amount * 360f / sum
                    if (sweep > 0f) drawArc(color, start, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                    start += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, color = TasColors.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
        }
    }
}

/** 웹 StyledLegendInlineLabel — 알약(점 · 이름 · 굵은 값 · %), 누르면 상세. */
@Composable
private fun LegendPill(color: Color, name: String, value: String, percent: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        Modifier.clip(shape).border(1.dp, TasColors.LightGray, shape).background(TasColors.Surface)
            .clickable(role = Role.Button, onClickLabel = "상세 보기", onClick = onClick)
            .heightIn(min = 32.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Swatch(color)
        Text(name, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
        Text(value, color = TasColors.Black, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.Bold)
        Text("$percent%", color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
    }
}

@Composable
private fun PaymentCard(charts: RevenueCharts, twoColumns: Boolean, onDetail: (ChartDetailKey) -> Unit) {
    // 웹 $autoHeight — 높이 하한 없음. 받은 돈 7종은 늘 있어 빈 상태가 없다(웹과 같다).
    ChartCard("결제수단 비중", "결제완료 기준", 0.dp) {
        val donut = @Composable {
            Donut(charts.payments.map { hexColor(it.color) to it.total }, formatPrice(charts.paidTotal), "결제합계", twoColumns)
        }
        val legend = @Composable { m: Modifier ->
            FlowRow(m, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                charts.payments.forEach { item ->
                    LegendPill(hexColor(item.color), item.method, formatPrice(item.total), chartPercent(item.total, charts.paidTotal)) {
                        onDetail(ChartDetailKey.Payment(item.method))
                    }
                }
            }
        }
        if (twoColumns) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                donut()
                legend(Modifier.weight(1f))
            }
        } else {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                donut()
                legend(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ChannelCard(charts: RevenueCharts, twoColumns: Boolean, modifier: Modifier, onDetail: (ChartDetailKey) -> Unit) {
    ChartCard("예약비중", "전화 · 방문 · 네이버", if (twoColumns) 250.dp else 0.dp, modifier) {
        val total = charts.channelTotal
        if (total == 0) { ChartEmpty(fill = twoColumns); return@ChartCard }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Donut(charts.channels.map { hexColor(it.color) to it.count }, "${total}건", "전체예약", twoColumns)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                charts.channels.forEach { item ->
                    LegendPill(hexColor(item.color), item.channel, "${item.count}건", chartPercent(item.count, total)) {
                        onDetail(ChartDetailKey.Channel(item.channel))
                    }
                }
            }
        }
    }
}

// ── ④ 담당자별 매출 ──

@Composable
private fun AssigneeCard(
    charts: RevenueCharts,
    assigneeSelected: Boolean,
    assigneeLabel: String,
    twoColumns: Boolean,
    modifier: Modifier,
    onDetail: (ChartDetailKey) -> Unit,
) {
    ChartCard("${assigneeLabel}별 매출", if (assigneeSelected) "선택 담당자 기준" else "전체 기준", if (twoColumns) 250.dp else 0.dp, modifier) {
        if (charts.assignees.isEmpty()) { ChartEmpty(fill = twoColumns); return@ChartCard }
        val sum = charts.assignees.sumOf { it.total }
        val items = charts.assignees.sortedWith { a, b -> compareAssigneeName(a.name, b.name) }
        // 웹 StyledShareSection — '전체비율' · 범례(점 이름 굵은%) · 20 띠
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("전체비율", color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Swatch(hexColor(item.color))
                        Text(item.name, color = TasColors.DarkGray, fontSize = TasSizes.XSmallFont)
                        Text("${chartPercent(item.total, sum)}%", color = TasColors.Black, fontSize = TasSizes.XSmallFont, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Row(Modifier.padding(top = 4.dp).fillMaxWidth().height(20.dp).clip(RoundedCornerShape(percent = 50)).background(TasColors.Gray2)) {
                if (sum > 0) items.filter { it.total > 0 }.forEach { item ->
                    Box(Modifier.weight(item.total.toFloat()).fillMaxHeight().background(hexColor(item.color)))
                }
            }
        }
        // 웹 StyledBarChartList — 이름·금액 / 20 막대(합계 대비 폭), 누르면 그 담당자 매출 상세
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { item ->
                Column(
                    Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "상세 보기") { onDetail(ChartDetailKey.Assignee(item.assigneeId)) },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Swatch(hexColor(item.color))
                            Text(item.name, color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(formatPrice(item.total), color = TasColors.Black, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    Box(Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(percent = 50)).background(TasColors.Gray2)) {
                        val ratio = item.total.toFloat() / (if (sum > 0) sum else 1)
                        if (ratio > 0f) Box(Modifier.fillMaxWidth(ratio).fillMaxHeight().clip(RoundedCornerShape(percent = 50)).background(hexColor(item.color)))
                    }
                }
            }
        }
    }
}

// ── ⑤ 담당자별 취소율 · ⑥ 고객별 노쇼율 ──

/** 웹 StyledOperationSummary — 회색 바탕 '전체 취소 n건' · 큰 %. */
@Composable
private fun OperationSummary(label: String, rate: Int) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(TasColors.Gray2).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont, modifier = Modifier.weight(1f))
        Text("$rate%", color = TasColors.Black, fontSize = BigFont, fontWeight = FontWeight.Bold)
    }
}

/** 웹 StyledClickableOperationRow — 이름·'n건 중 m건' / 파랑 %. 행을 누르면 그 취소·노쇼 상세. */
@Composable
private fun OperationRow(label: String, sub: String, rate: Int, onClick: () -> Unit, name: @Composable () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).border(1.dp, TasColors.LightGray, shape).background(TasColors.Surface)
            .clickable(role = Role.Button, onClickLabel = "상세 보기", onClick = onClick)
            // 안의 이름 버튼(노쇼)은 따로 읽혀 행에서 빠진다 — 행은 누구의 몇 건인지 한 문장으로
            .semantics { contentDescription = "$label $sub $rate%" }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            name()
            Text(sub, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
        }
        Text("$rate%", color = TasColors.Blue, fontSize = TasSizes.LargeFont, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CancellationCard(
    charts: RevenueCharts,
    assigneeLabel: String,
    assigneeNames: Map<Int, String>,
    rawAssigneeColors: Map<Int, String?>,
    twoColumns: Boolean,
    modifier: Modifier,
    onDetail: (ChartDetailKey) -> Unit,
) {
    ChartCard("${assigneeLabel}별 취소율", "기간 내 전체 예약 기준", if (twoColumns) 250.dp else 0.dp, modifier) {
        OperationSummary("전체 취소 ${charts.totalCancelled}건", charts.totalCancelledRate)
        if (charts.cancellations.isEmpty()) { ChartEmpty(fill = twoColumns); return@ChartCard }
        // 웹: 이름이 없으면(미지정·지운 담당자) '미지정' 회색, 색은 담당자에 직접 고른 색만(없으면 회색)
        val rows = charts.cancellations.map { item ->
            val name = item.id?.let { assigneeNames[it] } ?: "미지정"
            Triple(item, name, item.id?.let { rawAssigneeColors[it] } ?: UNASSIGNED_COLOR)
        }.sortedWith { a, b -> compareAssigneeName(a.second, b.second) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { (item, name, color) ->
                OperationRow(name, "${item.total}건 중 ${item.hit}건", item.rate, { onDetail(ChartDetailKey.Cancellation(item.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Swatch(hexColor(color))
                        Text(name, color = TasColors.DarkGray, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoshowCard(
    charts: RevenueCharts,
    customers: Map<Int, Customer>,
    twoColumns: Boolean,
    modifier: Modifier,
    onDetail: (ChartDetailKey) -> Unit,
    onCustomerClick: (Int) -> Unit,
) {
    ChartCard("고객별 노쇼율", "기간 내 전체 예약 기준", if (twoColumns) 250.dp else 0.dp, modifier) {
        OperationSummary("전체 노쇼 ${charts.totalNoshow}건", charts.totalNoshowRate)
        if (charts.noshows.isEmpty()) { ChartEmpty(fill = twoColumns); return@ChartCard }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            charts.noshows.forEach { item ->
                val id = item.id!! // 노쇼는 고객별이라 늘 있다(Reservation.customerId 는 null 이 아니다)
                val customer = customers[id]
                OperationRow(customer?.name ?: "고객 미지정", "${item.total}건 중 ${item.hit}건", item.rate, { onDetail(ChartDetailKey.Noshow(id)) }) {
                    if (customer != null) {
                        // 웹 StyledOperationCustomerButton — 이름만 누르면 고객 상세(행은 노쇼 상세)
                        Text(customer.name, color = TasColors.DarkGray, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.widthIn(min = 24.dp).heightIn(min = 24.dp).wrapContentHeight(Alignment.CenterVertically)
                                .clickable(role = Role.Button, onClickLabel = "고객 정보 보기") { onCustomerClick(id) })
                    } else {
                        Text("고객 미지정", color = TasColors.DarkGray, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private val MinBar = 6.dp
private val MaxBar = 48.dp
private val DonutEmpty = Color(0xE5, 0xE7, 0xEB)
/** 웹 rgba(101, 38, 217, 0.1) — 추이 상자 오른쪽 위 빛 */
private val TasBrandGlow = Color(101, 38, 217).copy(alpha = 0.1f)
/** 웹 rgba(101, 38, 217, 0.08) — 추이 상자 테두리 */
private val TasBrandHairline = Color(101, 38, 217).copy(alpha = 0.08f)
