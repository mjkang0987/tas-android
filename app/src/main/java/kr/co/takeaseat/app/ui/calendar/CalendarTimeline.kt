package kr.co.takeaseat.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.zIndex
import java.time.LocalDate
import kr.co.takeaseat.app.domain.CalendarView
import kr.co.takeaseat.app.domain.WEEKDAYS
import kr.co.takeaseat.app.domain.maxLanes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import kotlinx.coroutines.delay
import kr.co.takeaseat.app.domain.PlacedReservation
import kr.co.takeaseat.app.domain.StoreClosedKind
import kr.co.takeaseat.app.domain.TimelineScale
import kr.co.takeaseat.app.domain.cardHeightFor
import kr.co.takeaseat.app.domain.toMinutes
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.drawBehindThemed
import kr.co.takeaseat.app.ui.theme.TasSizes

// 웹 `Timeline.tsx` 의 좌표계. 정시선·카드·현재시각 바가 같은 오프셋을 쓴다.
private val DAY_BLOCK_OFFSET = 55.dp // 웹 blockOffsetOf(Day)
private val COLUMNS_BLOCK_OFFSET = 25.dp // 웹 blockOffsetOf(3일·주) — 날짜 머리줄 아래서 잰다
private val AXIS_MARGIN = 90.dp // 웹 StyledTimes 위+아래 여백 합
private val WEEK_HEADING = 24.dp // 웹 TIMELINE_WEEK_HEADING
private val WEEK_COL = 64.dp // 웹 모바일 --week-col
private val NARROW_MAX = 640.dp // 웹 모바일 분기 (max-width: 640px)
private val CARD_INSET_LEFT = 3.dp
private val LANE_GAP = 3.dp
private val NOW_SCROLL_MARGIN = 72.dp // 웹 StyledBar scroll-margin-top
private val HeadingShadow = Color(15, 23, 42).copy(alpha = 0.06f) // 웹 0 2px 4px rgba(15,23,42,.06)

/** 한 칸에 그릴 것 — 화면 쪽에서 고객·색을 풀어 둔다. */
data class TimelineCardItem(
    val placed: PlacedReservation,
    val color: Color,
    val customerName: String?,
    val isNewCustomer: Boolean,
)

/** 하루 칸. [lanes] = 그날 가장 많이 겹친 수 — 주 보기는 칸 폭이 그만큼 넓어진다(웹 weekColumns). */
data class TimelineColumn(
    val date: LocalDate,
    val items: List<TimelineCardItem>,
    val closedKind: StoreClosedKind?,
    val isToday: Boolean,
    val faded: Boolean,
) {
    val lanes: Int = maxLanes(items.map { it.placed })
}

/**
 * 웹 캘린더 본문: 왼쪽 시간축(44) + 날짜 칸들. 축과 칸이 한 세로 스크롤로 같이 움직인다.
 * 일 = 한 칸. 3일·주 = 칸마다 "10.22 목" 머리줄(위에 고정) — 3일은 폭을 셋으로 나누고,
 * 주는 칸을 64 × 겹침 수로 못박아 넘치면 가로로 민다(시간축은 고정). 좁은 칸의 카드는 이름만.
 * 640dp 를 넘는 폭(가로 화면·태블릿)은 웹 데스크톱처럼 주도 겹침 수 비율로 폭을 나누고 카드를 다 보인다.
 * 축 높이 = 90 + (끝-시작+1)시간 — 웹은 끝 시각 라벨까지 그린다. 3일·주는 그 90 안에 머리줄(24)이 든다.
 * 뷰를 바꿀 때 스크롤을 새로 잡도록 호출부가 `key(view)` 로 감싼다(웹도 뷰마다 새 노드).
 */
@Composable
fun CalendarTimeline(
    view: CalendarView,
    hours: IntRange,
    scale: TimelineScale,
    columns: List<TimelineColumn>,
    serviceColorMap: Map<String, String>,
    onReservationClick: (Int) -> Unit,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val multi = view != CalendarView.Day
    val blockOffset = if (multi) COLUMNS_BLOCK_OFFSET else DAY_BLOCK_OFFSET
    val hourHeight = scale.hourHeight.dp
    val labelCount = hours.last - hours.first + 2 // 웹 `end - start + 1` (IntRange.last = end - 1)
    val totalHeight = (if (multi) AXIS_MARGIN - WEEK_HEADING else AXIS_MARGIN) + hourHeight * labelCount
    val todayInView = columns.any { it.isToday }

    // 현재시각 — 30초마다. shortcut: 화면 복귀 즉시 갱신은 없다(웹은 focus 때 바로). 최대 30초 늦다.
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(todayInView) {
        while (todayInView) {
            now = LocalTime.now()
            delay(30_000)
        }
    }
    val nowTop = blockOffset + hourHeight * (now.hour - hours.first) +
        (scale.minuteHeight * now.minute + scale.minuteHeight / 60 * now.second).dp

    // 오늘이 보이면 열자마자 현재시각이 위쪽(여백 72)에 오게. 다른 시간대를 보는 중에 끌려오지 않게 오늘이 들고 날 때만.
    // 3일·주는 머리줄이 스크롤 밖에 있어 그만큼 여백을 덜 준다(웹은 머리줄이 스크롤 안에 떠 있다).
    val density = LocalDensity.current
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    LaunchedEffect(todayInView) {
        val margin = if (multi) NOW_SCROLL_MARGIN - WEEK_HEADING else NOW_SCROLL_MARGIN
        if (todayInView) vScroll.scrollTo(with(density) { (nowTop - margin).roundToPx() }.coerceAtLeast(0))
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val narrow = maxWidth <= NARROW_MAX
        val sideways = view == CalendarView.Week && narrow
        // 칸 폭: 가로로 미는 주는 64 × 겹침 수, 넓은 화면의 주는 겹침 수 비율, 3일은 같은 몫.
        fun RowScope.columnWidth(column: TimelineColumn) = when {
            sideways -> Modifier.width(WEEK_COL * column.lanes)
            view == CalendarView.Week -> Modifier.weight(column.lanes.toFloat())
            else -> Modifier.weight(1f)
        }
        // 주 보기는 열 때 한 번 오늘 칸을 가운데로(웹 Week scrollIntoView inline:center). 주를 넘겨도 다시 끌지 않는다.
        // 넓은 화면에서 열고 좁게 돌렸을 때도 가로 스크롤이 생기는 순간 한 번 더 맞춘다.
        val viewport = maxWidth - TasSizes.TimelineCol
        LaunchedEffect(sideways) {
            val i = columns.indexOfFirst { it.isToday }
            if (!sideways || i < 0) return@LaunchedEffect
            val center = WEEK_COL * columns.take(i).sumOf { it.lanes } + WEEK_COL * columns[i].lanes / 2
            hScroll.scrollTo(with(density) { (center - viewport / 2).roundToPx() }.coerceAtLeast(0))
        }
        Column(Modifier.fillMaxSize()) {
            if (multi) {
                // 웹 StyledNumWrap — 흰 바탕 + 옅은 그림자로 밑으로 지나가는 카드와 경계를 준다.
                Row(Modifier.fillMaxWidth().zIndex(1f).background(TasColors.Surface)) {
                    Spacer(Modifier.width(TasSizes.TimelineCol).height(WEEK_HEADING))
                    Row(
                        Modifier
                            .weight(1f)
                            .dropShadow(RectangleShape, Shadow(radius = 4.dp, color = HeadingShadow, offset = DpOffset(0.dp, 2.dp)))
                            .background(TasColors.Surface)
                            .then(if (sideways) Modifier.horizontalScroll(hScroll) else Modifier),
                    ) {
                        columns.forEachIndexed { i, column ->
                            DayHeading(
                                column = column,
                                divider = i != 6,
                                onOpen = { onDateClick(column.date) },
                                modifier = columnWidth(column),
                            )
                        }
                    }
                }
            }
            Box(Modifier.weight(1f).verticalScroll(vScroll)) {
                Row(Modifier.fillMaxWidth().height(totalHeight)) {
                    TimeAxis(hours, labelCount, scale, blockOffset)
                    Row(Modifier.weight(1f).fillMaxHeight().then(if (sideways) Modifier.horizontalScroll(hScroll) else Modifier)) {
                        columns.forEachIndexed { i, column ->
                            DayColumn(
                                column = column,
                                hours = hours,
                                scale = scale,
                                blockOffset = blockOffset,
                                nowTop = nowTop,
                                nameOnly = multi && narrow,
                                // 3일·주는 머리줄이 "…일 보기, 정기휴무" 로 읽는다 — 칸에서 날짜 없이 또 읽지 않게.
                                announceClosed = !multi,
                                divider = multi && i != 6,
                                serviceColorMap = serviceColorMap,
                                onReservationClick = onReservationClick,
                                modifier = columnWidth(column).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 웹 Week StyledDayHeading — "10.22 목", 오늘은 브랜드색 굵게, 다른 달은 회색. 누르면 그날 일 보기. */
@Composable
private fun DayHeading(column: TimelineColumn, divider: Boolean, onOpen: () -> Unit, modifier: Modifier) {
    val d = column.date
    Text(
        "${d.monthValue}.${d.dayOfMonth} ${WEEKDAYS[d.dayOfWeek.value - 1]}",
        color = when {
            column.isToday -> TasColors.Brand
            // 웹은 --gray-color(대비 1.4:1)라 글자로 못 읽는다 — 보조 글자색(4.8:1)으로.
            column.faded -> TasColors.DarkGray2
            else -> TasColors.Black
        },
        fontSize = TasSizes.SmallFont,
        fontWeight = if (column.isToday) FontWeight.Bold else FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .height(WEEK_HEADING)
            .then(if (divider) Modifier.rightLine() else Modifier)
            .clickable(role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                // 웹 aria-label "10월 22일 보기". 오늘·휴무는 색으로만 보여 소리로 따로 알린다.
                contentDescription = listOfNotNull("${d.monthValue}월 ${d.dayOfMonth}일 보기", "오늘".takeIf { column.isToday }, column.closedKind?.label)
                    .joinToString(", ")
                role = Role.Button
                onClick { onOpen(); true }
            }
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(horizontal = 2.dp),
    )
}

/** 하루 칸 — 휴무 바탕, 정시선, 카드, (오늘이면) 현재시각 바. */
@Composable
private fun DayColumn(
    column: TimelineColumn,
    hours: IntRange,
    scale: TimelineScale,
    blockOffset: Dp,
    nowTop: Dp,
    nameOnly: Boolean,
    announceClosed: Boolean,
    divider: Boolean,
    serviceColorMap: Map<String, String>,
    onReservationClick: (Int) -> Unit,
    modifier: Modifier,
) {
    val hourHeight = scale.hourHeight.dp
    val closedKind = column.closedKind
    BoxWithConstraints(
        modifier
            .then(closedKind?.let { Modifier.background(if (it == StoreClosedKind.Date) TasColors.ClosedDateBg else TasColors.ClosedWeekdayBg) } ?: Modifier)
            .semantics { if (closedKind != null && announceClosed) contentDescription = closedKind.label }
            .drawBehindThemed { colors ->
                // 정시선은 본문이 그린다(웹과 같이 카드 아래에 깔리게). 카드와 같은 오프셋.
                var y = blockOffset.toPx()
                val step = hourHeight.toPx()
                while (y < size.height) {
                    drawRect(colors.CalendarGridLine, topLeft = Offset(0f, y), size = Size(size.width, 1.dp.toPx()))
                    y += step
                }
            }
            .then(if (divider) Modifier.rightLine() else Modifier),
    ) {
        val usable = maxWidth - CARD_INSET_LEFT * 2
        column.items.forEach { item ->
            val r = item.placed.reservation
            val lane = item.placed.lane
            val startMinutes = toMinutes(r.startTime)
            val top = blockOffset + hourHeight * (startMinutes / 60 - hours.first) + (startMinutes % 60 * scale.minuteHeight).dp
            val height = cardHeightFor(toMinutes(r.endTime) - startMinutes, scale.minuteHeight).dp
            val width = (usable - LANE_GAP * (lane.count - 1)) / lane.count
            ReservationCard(
                reservation = r,
                height = height,
                color = item.color,
                customerName = item.customerName,
                isNewCustomer = item.isNewCustomer,
                serviceColorMap = serviceColorMap,
                nameOnly = nameOnly,
                onClick = { onReservationClick(r.id) },
                modifier = Modifier
                    .offset(x = CARD_INSET_LEFT + (width + LANE_GAP) * lane.index, y = top)
                    .size(width, height),
            )
        }
        if (column.isToday) NowBar(nowTop)
    }
}

/** 웹 StyledWeek:after — 칸 오른쪽 1px 세로선(주의 마지막 칸은 없음). */
@Composable
private fun Modifier.rightLine() = drawBehindThemed { colors ->
    drawRect(colors.CalendarGridLine, topLeft = Offset(size.width - 1.dp.toPx(), 0f), size = Size(1.dp.toPx(), size.height))
}

/** 웹 `TimelineTitle` — 정시만 두 자리 숫자, 30분은 짧은 눈금. */
@Composable
private fun TimeAxis(hours: IntRange, labelCount: Int, scale: TimelineScale, blockOffset: Dp) {
    val hourHeight = scale.hourHeight.dp
    // 웹은 30분 칸이 있을 때만(30 이 단위로 나눠질 때) 눈금을 단다.
    val halfTick = 30 % scale.unit == 0
    val labelHeight = 14.dp
    Box(
        Modifier
            .width(TasSizes.TimelineCol)
            .fillMaxHeight()
            .drawBehindThemed { colors ->
                drawRect(colors.CalendarGridLine, topLeft = Offset(size.width - 1.dp.toPx(), 0f), size = Size(1.dp.toPx(), size.height))
                if (halfTick) {
                    for (i in 0 until labelCount) {
                        val y = (blockOffset + hourHeight * i + hourHeight / 2).toPx()
                        drawRect(colors.DarkGray2.copy(alpha = 0.35f), topLeft = Offset(size.width - 6.dp.toPx(), y), size = Size(6.dp.toPx(), 1.dp.toPx()))
                    }
                }
            },
    ) {
        for (i in 0 until labelCount) {
            val hour = hours.first + i
            // 라벨 가운데 = 정시선. 웹은 30분 단위 매장에서만 맞는다(30 + 칸 절반 = 55) — 앱은 단위와 무관하게 맞춘다.
            Text(
                text = hour.toString().padStart(2, '0'),
                color = TasColors.DarkGray,
                fontSize = TasSizes.TinyFont,
                lineHeight = TasSizes.TinyFont,
                style = LocalTextStyle.current.merge(TabularNums),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .offset(y = blockOffset + hourHeight * i - labelHeight / 2)
                    .width(TasSizes.TimelineCol)
                    .height(labelHeight)
                    .padding(end = 6.dp, top = 2.dp)
                    .clearAndSetSemantics { contentDescription = "${hour}시" },
            )
        }
    }
}

/** 웹 StyledBar — 2px 주황 선 + 왼쪽 10px 점. */
@Composable
private fun NowBar(top: Dp) {
    Box(Modifier.offset(y = top).fillMaxWidth().height(2.dp).background(TasColors.Orange))
    Box(
        Modifier
            .offset(y = top - 4.dp)
            .size(10.dp)
            .background(TasColors.Orange, CircleShape),
    )
}
