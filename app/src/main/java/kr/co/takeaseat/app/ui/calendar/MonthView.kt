package kr.co.takeaseat.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.hideFromAccessibility
import kr.co.takeaseat.app.domain.WEEKDAYS
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kr.co.takeaseat.app.domain.LocalDbSnapshot
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.ReservationStatus
import kr.co.takeaseat.app.domain.StoreClosedKind
import kr.co.takeaseat.app.domain.UNASSIGNED_COLOR
import kr.co.takeaseat.app.domain.isNewCustomerVisit
import kr.co.takeaseat.app.domain.monthGrid
import kr.co.takeaseat.app.domain.storeClosedKind
import kr.co.takeaseat.app.domain.toDateKey
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.ui.components.lineBox
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

private val MIN_LIST_HEIGHT = 160.dp // 격자 밑에 목록이 이만큼도 안 남으면 화면 전체를 한 스크롤로
private val GridPadding = PaddingValues(horizontal = 12.dp) // 웹 MonthWrap 모바일 padding 0 12
private val CellGap = 3.dp // 웹 gap 3px

/**
 * 웹 월 보기(모바일) — 요일 줄(`Days`) + 날짜 격자(`MonthWrap`·`Month`) + 아래 고른 날의 예약 목록(`ReservationListSection`).
 * 칸에는 예약이 하나면 이름, 둘 이상이면 건수만(웹 MOBILE_MAX_NAMES = 1). 날짜를 누르면 아래 목록이 그날로 바뀐다.
 * 목록은 레이어가 아니라 격자 아래 자리 — 남은 높이를 채우고 제 안에서 스크롤한다.
 */
@Composable
fun MonthView(
    date: LocalDate,
    today: LocalDate,
    selected: LocalDate,
    snapshot: LocalDbSnapshot,
    reservationsByDate: Map<String, List<Reservation>>,
    customerNames: Map<Int, String>,
    firstVisit: Map<Int, String>,
    assigneeColors: Map<Int, String>,
    serviceColorMap: Map<String, String>,
    onSelect: (LocalDate) -> Unit,
    onReservationClick: (Int) -> Unit,
    onCustomerClick: (Int) -> Unit,
    onCreate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedReservations = remember(reservationsByDate, selected) {
        reservationsByDate[selected.toDateKey()].orEmpty().sortedBy { it.startTime }
    }
    CalendarGridWithList(
        modifier = modifier,
        grid = { gridModifier ->
            MonthGrid(
                date = date,
                today = today,
                selected = selected,
                snapshot = snapshot,
                reservationsByDate = reservationsByDate,
                customerNames = customerNames,
                firstVisit = firstVisit,
                assigneeColors = assigneeColors,
                onSelect = onSelect,
                onReservationClick = onReservationClick,
                modifier = gridModifier,
            )
        },
        list = { lazy, listModifier ->
            ReservationListSection(
                title = "${selected.monthValue}월 ${selected.dayOfMonth}일",
                listKey = selected,
                reservations = selectedReservations,
                grouped = false,
                lazy = lazy,
                snapshot = snapshot,
                customerNames = customerNames,
                firstVisit = firstVisit,
                assigneeColors = assigneeColors,
                serviceColorMap = serviceColorMap,
                onReservationClick = onReservationClick,
                onCustomerClick = onCustomerClick,
                onCreate = { onCreate(selected) },
                modifier = listModifier,
            )
        },
    )
}

/**
 * 웹은 격자(월·연) 높이를 고정하고 남는 자리를 아래 목록이 제 안에서 스크롤한다. 가로 화면처럼 낮으면 격자가 잘리고
 * 목록이 0 이 되므로, 격자 밑에 [MIN_LIST_HEIGHT] 가 안 남을 때만 격자·목록을 한 스크롤로 잇는다(목록은 lazy 아님).
 */
@Composable
internal fun CalendarGridWithList(
    modifier: Modifier,
    grid: @Composable (Modifier) -> Unit,
    list: @Composable (lazy: Boolean, Modifier) -> Unit,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        var gridHeight by remember { mutableStateOf(0.dp) }
        val compact = gridHeight > maxHeight - MIN_LIST_HEIGHT
        Column(Modifier.fillMaxSize().then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
            grid(Modifier.onSizeChanged { gridHeight = with(density) { it.height.toDp() } })
            list(!compact, if (compact) Modifier else Modifier.weight(1f))
        }
    }
}

/** 웹 Days(월) + MonthWrap — 요일 줄과 날짜 격자. */
@Composable
private fun MonthGrid(
    date: LocalDate,
    today: LocalDate,
    selected: LocalDate,
    snapshot: LocalDbSnapshot,
    reservationsByDate: Map<String, List<Reservation>>,
    customerNames: Map<Int, String>,
    firstVisit: Map<Int, String>,
    assigneeColors: Map<Int, String>,
    onSelect: (LocalDate) -> Unit,
    onReservationClick: (Int) -> Unit,
    modifier: Modifier,
) {
    val closedWeekdays = snapshot.storeSettings.closedWeekdays
    Column(modifier) {
        // 웹 Days(월) — 요일 줄도 격자와 같은 여백·간격이라야 양끝 칸이 어긋나지 않는다.
        Row(Modifier.fillMaxWidth().padding(GridPadding), horizontalArrangement = Arrangement.spacedBy(CellGap)) {
            for (index in 0..6) {
                // 이 줄은 일요일부터, WEEKDAYS·closedWeekdays 는 월요일=0
                val weekday = (index + 6) % 7
                val label = WEEKDAYS[weekday]
                val closed = weekday in closedWeekdays
                Text(
                    label,
                    color = TasColors.Black,
                    fontSize = TasSizes.SmallFont,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (closed) Modifier.background(TasColors.ClosedWeekdayBg) else Modifier)
                        .padding(top = 4.dp, bottom = 2.dp)
                        .semantics { if (closed) contentDescription = "$label, ${StoreClosedKind.Weekday.label}" },
                )
            }
        }
        Column(Modifier.fillMaxWidth().padding(GridPadding), verticalArrangement = Arrangement.spacedBy(CellGap)) {
            monthGrid(date).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    week.forEach { day ->
                        val key = day.toDateKey()
                        DayCell(
                            day = day,
                            adjacent = day.month != date.month,
                            isToday = day == today,
                            isSelected = day == selected,
                            closedKind = storeClosedKind(snapshot.storeSettings, key),
                            reservations = reservationsByDate[key].orEmpty(),
                            customerNames = customerNames,
                            firstVisit = firstVisit,
                            assigneeColors = assigneeColors,
                            onSelect = { onSelect(day) },
                            onReservationClick = onReservationClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** 웹 Month 칸(StyledDate) — 날짜 원(Num) + 이름 한 줄 또는 건수. 휴무는 바탕 틴트. */
@Composable
private fun DayCell(
    day: LocalDate,
    adjacent: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    closedKind: StoreClosedKind?,
    reservations: List<Reservation>,
    customerNames: Map<Int, String>,
    firstVisit: Map<Int, String>,
    assigneeColors: Map<Int, String>,
    onSelect: () -> Unit,
    onReservationClick: (Int) -> Unit,
    modifier: Modifier,
) {
    // 웹 aria-label "10월 22일 예약 N건 보기" — 오늘·휴무는 색으로만 보여 소리로 덧붙인다.
    val label = listOfNotNull(
        "${day.monthValue}월 ${day.dayOfMonth}일 예약 ${reservations.size}건 보기",
        "오늘".takeIf { isToday },
        closedKind?.label,
    ).joinToString(", ")
    Column(
        modifier
            .heightIn(min = 40.dp) // 웹 grid-auto-rows: minmax(40px, auto)
            .then(closedKind?.let { Modifier.background(if (it == StoreClosedKind.Date) TasColors.ClosedDateBg else TasColors.ClosedWeekdayBg) } ?: Modifier)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DateCircle(
            text = if (adjacent) "${day.monthValue}/${day.dayOfMonth}" else day.dayOfMonth.toString(),
            label = label,
            isToday = isToday,
            isSelected = isSelected,
            faded = adjacent,
            onClick = onSelect,
        )
        when {
            reservations.isEmpty() -> Unit
            // 웹 StyledCountButton — 건수만, 브랜드색 평문. 누르면 아래 목록이 그날로.
            reservations.size > 1 -> Text(
                reservations.size.toString(),
                color = TasColors.Brand,
                fontSize = TasSizes.TinyFont,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    // 바로 위 날짜 원과 같은 동작·같은 이름 — 낭독기에선 한 번만 읽히게 숨긴다(누르는 자리는 그대로).
                    .semantics { hideFromAccessibility() }
                    .clickable(role = Role.Button, onClick = onSelect)
                    .padding(horizontal = 6.dp),
            )
            else -> {
                val r = reservations.first()
                val name = customerNames[r.customerId].orEmpty()
                MonthNameTag(
                    reservation = r,
                    name = name,
                    color = hexColor(r.assigneeId?.let { assigneeColors[it] } ?: UNASSIGNED_COLOR),
                    // 웹 aria-label — 보이는 글자(이름)가 이름에 다 들어가야 음성 조작으로 부를 수 있다(WCAG 2.5.3)
                    description = "${r.startTime} ${if (isNewCustomerVisit(firstVisit[r.customerId], r.date)) "신규 " else ""}$name",
                    onClick = { onReservationClick(r.id) },
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/**
 * 웹 `Num` — 30 원. 오늘은 옅게, 고른 날은 꽉 차게(iOS monthDayCell). [faded](앞뒤 달 "9/27")는 회색 글자에 폭이 늘어난다.
 * 월 칸의 날짜, 연 칸의 달 숫자가 같이 쓴다.
 */
@Composable
internal fun DateCircle(text: String, label: String, isToday: Boolean, isSelected: Boolean, faded: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(30.dp)
            .widthIn(min = 30.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> TasColors.Brand
                    isToday -> TasColors.BrandBorder
                    else -> Color.Transparent
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                selected = isSelected
                onClick { onClick(); true }
            }
            // 웹은 0 8px 이지만 360dp 칸(안쪽 약 41)엔 "12/31"(약 31) + 16 이 안 들어가 4 로 줄인다.
            .padding(horizontal = if (faded) 4.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = when {
                isSelected -> TasColors.White
                faded -> TasColors.DarkGray2
                else -> TasColors.Black
            },
            fontSize = TasSizes.SmallFont,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            // 더 좁은 화면에서도 잘리지 않게 그때만 줄인다
            autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = TasSizes.SmallFont),
        )
    }
}

/**
 * 웹 ReservationList 줄(모바일) — 담당자 색 테두리 + 왼쪽 3 막대 + 이름만(시간·신규 배지는 접는다).
 * 취소·노쇼는 회색 빗금 + 취소선.
 */
@Composable
private fun MonthNameTag(reservation: Reservation, name: String, color: Color, description: String, onClick: () -> Unit, modifier: Modifier) {
    val inactive = reservation.status == ReservationStatus.CANCELLED || reservation.status == ReservationStatus.NOSHOW
    val line = if (inactive) TasColors.Gray else color
    Box(
        modifier
            .fillMaxWidth()
            .clip(TasShapes.RadiusSm)
            .then(
                if (inactive) Modifier.background(TasColors.Gray2).inactiveHatch(reservation.status == ReservationStatus.NOSHOW)
                else Modifier.background(color.copy(alpha = 0x12 / 255f)),
            )
            .border(1.dp, line, TasShapes.RadiusSm)
            .drawBehind { drawRect(line, size = Size(3.dp.toPx(), size.height)) }
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description + if (inactive) ", ${if (reservation.status == ReservationStatus.NOSHOW) "노쇼" else "취소"}" else ""
                role = Role.Button
                onClick { onClick(); true }
            }
            .padding(start = 3.dp + 2.dp, end = 3.dp, top = 2.dp, bottom = 2.dp),
    ) {
        Text(
            name,
            color = if (inactive) TasColors.DarkGray2 else TasColors.DarkGray,
            fontSize = TasSizes.TinyFont,
            textDecoration = if (inactive) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.lineBox(12.sp, TasSizes.TinyFont),
        )
    }
}
