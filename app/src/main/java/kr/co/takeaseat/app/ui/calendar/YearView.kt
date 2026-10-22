package kr.co.takeaseat.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kr.co.takeaseat.app.domain.LocalDbSnapshot
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.bookedRevenueTotal
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.reservationsByMonth
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

private val MonthShape = RoundedCornerShape(5.dp) // 웹 StyledMonth border-radius 5px

/**
 * 웹 연 보기(모바일) — 4 × 3 달 칸 + 아래 고른 달의 예약 목록(날짜별 묶음).
 * 칸: 달 숫자(누르면 목록만 그 달로) · 건수/매출(같은 동작) · '자세히'(그 달 월 보기로). 매출은 취소·노쇼·신청 뺀 예약 기준.
 */
@Composable
fun YearView(
    year: Int,
    today: LocalDate,
    selectedMonth: Int,
    snapshot: LocalDbSnapshot,
    customerNames: Map<Int, String>,
    firstVisit: Map<Int, String>,
    assigneeColors: Map<Int, String>,
    serviceColorMap: Map<String, String>,
    onSelectMonth: (Int) -> Unit,
    onOpenMonth: (Int) -> Unit,
    onReservationClick: (Int) -> Unit,
    onCustomerClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val byMonth = remember(snapshot.reservations, year) { reservationsByMonth(snapshot.reservations, year) }
    val revenue = remember(byMonth, snapshot.services) { byMonth.map { bookedRevenueTotal(it, snapshot.services) } }
    CalendarGridWithList(
        modifier = modifier,
        grid = { gridModifier ->
            // 웹 StyledYear — 한 줄에 4달, 간격·안쪽 여백 5
            Column(gridModifier.fillMaxWidth().padding(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                (1..12).chunked(4).forEach { row ->
                    // 같은 줄 칸 높이를 맞춘다(웹 grid align-self: stretch) — '자세히'가 줄마다 같은 자리에
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { month ->
                            MonthCell(
                                month = month,
                                isCurrent = year == today.year && month == today.monthValue,
                                isSelected = month == selectedMonth,
                                reservations = byMonth[month - 1],
                                revenue = revenue[month - 1],
                                onSelect = { onSelectMonth(month) },
                                onOpen = { onOpenMonth(month) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        },
        list = { lazy, listModifier ->
            ReservationListSection(
                title = "${year}년 ${selectedMonth}월",
                listKey = year to selectedMonth,
                reservations = byMonth[selectedMonth - 1],
                grouped = true,
                lazy = lazy,
                snapshot = snapshot,
                customerNames = customerNames,
                firstVisit = firstVisit,
                assigneeColors = assigneeColors,
                serviceColorMap = serviceColorMap,
                onReservationClick = onReservationClick,
                onCustomerClick = onCustomerClick,
                onCreate = null, // 웹: 달 목록엔 ＋ 가 없다(어느 날에 넣을지 모른다)
                modifier = listModifier,
            )
        },
    )
}

/** 웹 StyledMonth — 옅은 회색 칸(최소 56). */
@Composable
private fun MonthCell(
    month: Int,
    isCurrent: Boolean,
    isSelected: Boolean,
    reservations: List<Reservation>,
    revenue: Int,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .heightIn(min = 56.dp)
            .clip(MonthShape)
            .background(TasColors.Black10)
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DateCircle(
            text = month.toString(),
            // 웹 Num 은 숫자만 읽힌다("10") — 무엇을 하는지·이번 달인지 같이 읽는다.
            label = listOfNotNull("${month}월 예약 목록 보기", "이번 달".takeIf { isCurrent }).joinToString(", "),
            isToday = isCurrent,
            isSelected = isSelected,
            faded = false,
            onClick = onSelect,
        )
        if (reservations.isNotEmpty()) {
            // 웹 StyledMonthSummary — 건수(브랜드) 위, 매출(회색) 아래
            Column(
                Modifier
                    .clickable(role = Role.Button, onClick = onSelect)
                    .clearAndSetSemantics {
                        contentDescription = "${month}월 예약 ${reservations.size}건, ${formatPrice(revenue)} 보기"
                        role = Role.Button
                        onClick { onSelect(); true }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text("${reservations.size}건", color = TasColors.Brand, fontSize = TasSizes.TinyFont, fontWeight = FontWeight.SemiBold)
                Text(formatPrice(revenue), color = TasColors.DarkGray2, fontSize = TasSizes.TinyFont)
            }
        }
        Spacer(Modifier.weight(1f)) // 웹 margin-top: auto — '자세히'는 칸 맨 아래
        // 웹 StyledMonthDetail — 누르는 높이 24(WCAG 2.5.8)
        Text(
            "자세히",
            color = TasColors.DarkGray2,
            fontSize = TasSizes.TinyFont,
            modifier = Modifier
                .clip(TasShapes.Chip)
                .clickable(role = Role.Button, onClick = onOpen)
                .clearAndSetSemantics {
                    contentDescription = "${month}월 달력 보기"
                    role = Role.Button
                    onClick { onOpen(); true }
                }
                .heightIn(min = 24.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .wrapContentHeight(Alignment.CenterVertically),
        )
    }
}
