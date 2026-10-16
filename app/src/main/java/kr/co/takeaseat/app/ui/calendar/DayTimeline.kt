package kr.co.takeaseat.app.ui.calendar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
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
import kr.co.takeaseat.app.ui.theme.TasSizes

// 웹 `Timeline.tsx`(일 뷰) 의 좌표계. 정시선·카드·현재시각 바가 같은 오프셋을 쓴다.
private val BLOCK_OFFSET = 55.dp // 웹 blockOffsetOf(Day)
private val AXIS_TOP = 30.dp // 웹 StyledTimes margin-top
private val AXIS_BOTTOM = 60.dp // 웹 StyledTimes margin-bottom
private val CARD_INSET_LEFT = 3.dp
private val LANE_GAP = 3.dp
private val NOW_SCROLL_MARGIN = 72.dp // 웹 StyledBar scroll-margin-top

/** 한 칸에 그릴 것 — 화면 쪽에서 고객·색을 풀어 둔다. */
data class TimelineCardItem(
    val placed: PlacedReservation,
    val color: Color,
    val customerName: String?,
    val isNewCustomer: Boolean,
)

/**
 * 웹 일 뷰: 왼쪽 시간축(44) + 본문. 둘이 한 스크롤로 같이 움직인다.
 * 시간축 높이 = 30 + (끝-시작+1)시간 + 60 — 웹은 끝 시각 라벨까지 그린다.
 */
@Composable
fun DayTimeline(
    hours: IntRange,
    scale: TimelineScale,
    items: List<TimelineCardItem>,
    serviceColorMap: Map<String, String>,
    isToday: Boolean,
    closedKind: StoreClosedKind?,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val hourHeight = scale.hourHeight.dp
    val labelCount = hours.last - hours.first + 2 // 웹 `end - start + 1` (IntRange.last = end - 1)
    val totalHeight = AXIS_TOP + hourHeight * labelCount + AXIS_BOTTOM

    // 현재시각 — 30초마다. shortcut: 화면 복귀 즉시 갱신은 없다(웹은 focus 때 바로). 최대 30초 늦다.
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(isToday) {
        while (isToday) {
            now = LocalTime.now()
            delay(30_000)
        }
    }
    val nowTop = BLOCK_OFFSET + hourHeight * (now.hour - hours.first) +
        (scale.minuteHeight * now.minute + scale.minuteHeight / 60 * now.second).dp

    // 오늘이면 열자마자 현재시각이 위쪽(여백 72)에 오게. 다른 시간대를 보는 중에 끌려오지 않게 isToday 가 바뀔 때만.
    val density = LocalDensity.current
    LaunchedEffect(isToday) {
        if (isToday) scrollState.scrollTo(with(density) { (nowTop - NOW_SCROLL_MARGIN).roundToPx() }.coerceAtLeast(0))
    }

    Box(modifier.fillMaxSize().verticalScroll(scrollState)) {
        Row(Modifier.fillMaxWidth().height(totalHeight)) {
            TimeAxis(hours, labelCount, scale)
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(closedKind?.let { Modifier.background(if (it == StoreClosedKind.Date) TasColors.ClosedDateBg else TasColors.ClosedWeekdayBg) } ?: Modifier)
                    .semantics { if (closedKind != null) contentDescription = closedKind.label }
                    .drawBehind {
                        // 정시선은 본문이 그린다(웹과 같이 카드 아래에 깔리게). 카드와 같은 오프셋.
                        var y = BLOCK_OFFSET.toPx()
                        val step = hourHeight.toPx()
                        while (y < size.height) {
                            drawRect(TasColors.CalendarGridLine, topLeft = Offset(0f, y), size = Size(size.width, 1.dp.toPx()))
                            y += step
                        }
                    },
            ) {
                val usable = maxWidth - CARD_INSET_LEFT * 2
                items.forEach { item ->
                    val r = item.placed.reservation
                    val lane = item.placed.lane
                    val startMinutes = toMinutes(r.startTime)
                    val top = BLOCK_OFFSET + hourHeight * (startMinutes / 60 - hours.first) + (startMinutes % 60 * scale.minuteHeight).dp
                    val height = cardHeightFor(toMinutes(r.endTime) - startMinutes, scale.minuteHeight).dp
                    val width = (usable - LANE_GAP * (lane.count - 1)) / lane.count
                    ReservationCard(
                        reservation = r,
                        height = height,
                        color = item.color,
                        customerName = item.customerName,
                        isNewCustomer = item.isNewCustomer,
                        serviceColorMap = serviceColorMap,
                        modifier = Modifier
                            .offset(x = CARD_INSET_LEFT + (width + LANE_GAP) * lane.index, y = top)
                            .size(width, height),
                    )
                }
                if (isToday) NowBar(nowTop)
            }
        }
    }
}

/** 웹 `TimelineTitle` — 정시만 두 자리 숫자, 30분은 짧은 눈금. */
@Composable
private fun TimeAxis(hours: IntRange, labelCount: Int, scale: TimelineScale) {
    val hourHeight = scale.hourHeight.dp
    // 웹은 30분 칸이 있을 때만(30 이 단위로 나눠질 때) 눈금을 단다.
    val halfTick = 30 % scale.unit == 0
    val labelHeight = 14.dp
    Box(
        Modifier
            .width(TasSizes.TimelineCol)
            .fillMaxHeight()
            .drawBehind {
                drawRect(TasColors.CalendarGridLine, topLeft = Offset(size.width - 1.dp.toPx(), 0f), size = Size(1.dp.toPx(), size.height))
                if (halfTick) {
                    for (i in 0 until labelCount) {
                        val y = (BLOCK_OFFSET + hourHeight * i + hourHeight / 2).toPx()
                        drawRect(TasColors.DarkGray2.copy(alpha = 0.35f), topLeft = Offset(size.width - 6.dp.toPx(), y), size = Size(6.dp.toPx(), 1.dp.toPx()))
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
                    .offset(y = BLOCK_OFFSET + hourHeight * i - labelHeight / 2)
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
