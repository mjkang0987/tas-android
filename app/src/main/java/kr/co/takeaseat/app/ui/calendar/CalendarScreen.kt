package kr.co.takeaseat.app.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.LocalDate
import kotlinx.coroutines.delay
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.CalendarView
import kr.co.takeaseat.app.domain.UNASSIGNED_COLOR
import kr.co.takeaseat.app.domain.buildAssigneeColorMap
import kr.co.takeaseat.app.domain.buildServiceColorMap
import kr.co.takeaseat.app.domain.firstVisitDates
import kr.co.takeaseat.app.domain.isNewCustomerVisit
import kr.co.takeaseat.app.domain.placeReservations
import kr.co.takeaseat.app.domain.storeClosedKind
import kr.co.takeaseat.app.domain.timelineRange
import kr.co.takeaseat.app.domain.timelineScaleFor
import kr.co.takeaseat.app.domain.toDateKey
import kr.co.takeaseat.app.domain.viewDates
import kr.co.takeaseat.app.domain.weekStart
import kr.co.takeaseat.app.ui.DetailSheets
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.ui.rememberDetailStack

/** 캘린더 탭 — 일·3일·주 보기(웹 `/day|three|week/yyyy/mm/dd`). */
@Composable
fun CalendarScreen(store: GuestStore, modifier: Modifier = Modifier) {
    val snapshot by store.snapshot.collectAsState()
    var dateKey by rememberSaveable { mutableStateOf(LocalDate.now().toDateKey()) }
    var view by rememberSaveable { mutableStateOf(CalendarView.Day) }
    val date = LocalDate.parse(dateKey)

    // 자정을 넘기면 '오늘'이 바뀐다 — 1분마다 확인
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            today = LocalDate.now()
        }
    }

    val serviceColorMap = remember(snapshot.services, snapshot.categoryBaseColors) {
        buildServiceColorMap(snapshot.services, snapshot.categoryBaseColors)
    }
    val assigneeColors = remember(snapshot.assignees) { buildAssigneeColorMap(snapshot.assignees) }
    val firstVisit = remember(snapshot.reservations) { firstVisitDates(snapshot.reservations) }
    val customers = remember(snapshot.customers) { snapshot.customers.associateBy { it.id } }
    val scale = remember(snapshot.services) { timelineScaleFor(snapshot.services) }

    // 취소·노쇼도 그대로 그린다(웹과 같이 빗금으로 구분).
    val reservationsByDate = remember(snapshot.reservations) { snapshot.reservations.groupBy { it.date } }
    val columns = remember(reservationsByDate, snapshot.storeSettings, dateKey, view, today, customers, firstVisit, assigneeColors) {
        viewDates(date, view).map { day ->
            val key = day.toDateKey()
            TimelineColumn(
                date = day,
                items = placeReservations(reservationsByDate[key].orEmpty()).map { placed ->
                    val r = placed.reservation
                    TimelineCardItem(
                        placed = placed,
                        color = hexColor(r.assigneeId?.let { assigneeColors[it] } ?: UNASSIGNED_COLOR),
                        customerName = customers[r.customerId]?.name,
                        isNewCustomer = isNewCustomerVisit(firstVisit[r.customerId], r.date),
                    )
                },
                closedKind = storeClosedKind(snapshot.storeSettings, key),
                isToday = day == today,
                faded = day.monthValue != date.monthValue,
            )
        }
    }

    // 열린 상세 창(예약 → 고객 → …) — 회전해도 유지
    val details = rememberDetailStack()
    var creating by rememberSaveable { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        CalendarHeader(view = view, date = date, onDateChange = { dateKey = it.toDateKey() }, onCreate = { creating = true })
        // 웹 MobileViewTabs — 일·3일은 오늘로, 주는 오늘이 든 주(그 주 일요일)로.
        CalendarViewTabs(selected = view, onSelect = {
            view = it
            dateKey = (if (it == CalendarView.Week) weekStart(LocalDate.now()) else LocalDate.now()).toDateKey()
        })
        // 뷰마다 스크롤을 새로 잡는다(웹도 뷰 종류마다 새 노드 — 주에서 민 가로 위치가 남지 않게).
        key(view) {
            CalendarTimeline(
                view = view,
                hours = timelineRange(snapshot.storeSettings.businessHours),
                scale = scale,
                columns = columns,
                serviceColorMap = serviceColorMap,
                onReservationClick = { details.openReservation(it) },
                onDateClick = {
                    dateKey = it.toDateKey()
                    view = CalendarView.Day
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    DetailSheets(store, details)

    if (creating) ReservationCreateSheet(store, onDismiss = { creating = false })
}

