package kr.co.takeaseat.app.ui.calendar

import kr.co.takeaseat.app.ui.nav.LocalAsideToggle
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
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.CalendarView
import kr.co.takeaseat.app.domain.UNASSIGNED_COLOR
import kr.co.takeaseat.app.domain.bellIndicator
import kr.co.takeaseat.app.domain.buildAssigneeColorMap
import kr.co.takeaseat.app.domain.upcomingReservations
import kr.co.takeaseat.app.domain.buildServiceColorMap
import kr.co.takeaseat.app.domain.firstVisitDates
import kr.co.takeaseat.app.domain.isNewCustomerVisit
import kr.co.takeaseat.app.domain.listDateWithin
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

/** 캘린더 탭 — 일·3일·주·월·년 보기(웹 `/day|three|week/yyyy/mm/dd`, `/month/yyyy/mm`, `/year/yyyy`). */
@Composable
fun CalendarScreen(
    store: GuestStore,
    modifier: Modifier = Modifier,
    openReservationId: Int? = null,
    onReservationOpened: () -> Unit = {},
    command: CalendarCommand? = null,
    onCommandHandled: () -> Unit = {},
    onViewShown: (CalendarView) -> Unit = {},
) {
    val snapshot by store.snapshot.collectAsState()
    var dateKey by rememberSaveable { mutableStateOf(LocalDate.now().toDateKey()) }
    var view by rememberSaveable { mutableStateOf(CalendarView.Day) }
    // 월 보기 아래 목록의 날짜(웹 reservationListFilter) — 월에 들어오면 오늘, 달을 옮기면 따라간다.
    var listDateKey by rememberSaveable { mutableStateOf(LocalDate.now().toDateKey()) }
    // 연 보기 아래 목록의 달(1~12) — 해를 옮겨도 달은 그대로(웹 filterWithinTarget 'month').
    var listMonth by rememberSaveable { mutableStateOf(LocalDate.now().monthValue) }
    val date = LocalDate.parse(dateKey)

    // 자정을 넘기면 '오늘'이 바뀐다 · 알림 종 '곧 시작'도 다시 센다 — 분이 바뀔 때마다
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val today = now.toLocalDate()
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            now = LocalDateTime.now()
        }
    }
    val upcoming = remember(snapshot.reservations, now) { upcomingReservations(snapshot.reservations, now) }
    var notifying by rememberSaveable { mutableStateOf(false) }

    val serviceColorMap = remember(snapshot.services, snapshot.categoryBaseColors) {
        buildServiceColorMap(snapshot.services, snapshot.categoryBaseColors)
    }
    val assigneeColors = remember(snapshot.assignees) { buildAssigneeColorMap(snapshot.assignees) }
    val firstVisit = remember(snapshot.reservations) { firstVisitDates(snapshot.reservations) }
    val customers = remember(snapshot.customers) { snapshot.customers.associateBy { it.id } }
    val customerNames = remember(customers) { customers.mapValues { it.value.name } }
    val scale = remember(snapshot.services) { timelineScaleFor(snapshot.services) }

    // 취소·노쇼도 그대로 그린다(웹과 같이 빗금으로 구분).
    val reservationsByDate = remember(snapshot.reservations) { snapshot.reservations.groupBy { it.date } }
    val columns = remember(reservationsByDate, snapshot.storeSettings, dateKey, view, today, customers, firstVisit, assigneeColors) {
        if (view == CalendarView.Month || view == CalendarView.Year) return@remember emptyList()
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
    // 예약 알림을 눌러 들어왔으면 그 예약 상세(지워진 예약이면 상세 창이 알아서 닫힌다)
    LaunchedEffect(openReservationId) {
        if (openReservationId != null) {
            details.openReservation(openReservationId)
            onReservationOpened()
        }
    }
    // 예약 추가 창 — null 이면 닫힘, "" 이면 헤더 ＋예약(오늘·지금), 날짜면 월 목록 ＋(그날 10:00).
    var creating by rememberSaveable { mutableStateOf<String?>(null) }
    // 웹 MobileViewTabs·Aside — 일·3일·월·년은 오늘로, 주는 오늘이 든 주(그 주 일요일)로. 월·년은 아래 목록도 오늘(이번 달).
    fun selectView(it: CalendarView) {
        view = it
        val now = LocalDate.now()
        dateKey = (if (it == CalendarView.Week) weekStart(now) else now).toDateKey()
        listDateKey = now.toDateKey()
        listMonth = now.monthValue
    }
    // 넓은 화면 왼쪽 메뉴에서 온 요청(보기 바꾸기·예약추가) — 알림 예약 열기와 같은 방식
    LaunchedEffect(command) {
        when (command) {
            is CalendarCommand.SelectView -> selectView(command.view)
            CalendarCommand.Create -> creating = ""
            null -> return@LaunchedEffect
        }
        onCommandHandled()
    }
    LaunchedEffect(view) { onViewShown(view) }

    Column(modifier.fillMaxSize()) {
        CalendarHeader(view = view, date = date, onDateChange = {
            dateKey = it.toDateKey()
            listDateKey = listDateWithin(LocalDate.parse(listDateKey), it).toDateKey()
        }, onCreate = { creating = "" }, bell = bellIndicator(todo = 0, soon = upcoming.soon.size), onBell = { notifying = true })
        // 넓은 화면은 왼쪽 메뉴가 보기를 고른다(웹 MobileViewTabs 는 640px 이하만)
        if (LocalAsideToggle.current == null) CalendarViewTabs(selected = view, onSelect = ::selectView)
        if (view == CalendarView.Year) {
            YearView(
                year = date.year,
                today = today,
                selectedMonth = listMonth,
                snapshot = snapshot,
                customerNames = customerNames,
                firstVisit = firstVisit,
                assigneeColors = assigneeColors,
                serviceColorMap = serviceColorMap,
                onSelectMonth = { listMonth = it },
                onOpenMonth = { month ->
                    // 웹 '자세히' — 그 달 월 보기. 목록은 오늘이 그 달이면 오늘, 아니면 1일(웹도 결과가 같다).
                    val first = LocalDate.of(date.year, month, 1)
                    dateKey = first.toDateKey()
                    listDateKey = listDateWithin(LocalDate.now(), first).toDateKey()
                    view = CalendarView.Month
                },
                onReservationClick = { details.openReservation(it) },
                onCustomerClick = { details.openCustomer(it) },
                modifier = Modifier.weight(1f),
            )
        } else if (view == CalendarView.Month) {
            MonthView(
                date = date,
                today = today,
                selected = LocalDate.parse(listDateKey),
                snapshot = snapshot,
                reservationsByDate = reservationsByDate,
                customerNames = customerNames,
                firstVisit = firstVisit,
                assigneeColors = assigneeColors,
                serviceColorMap = serviceColorMap,
                onSelect = { listDateKey = it.toDateKey() },
                onReservationClick = { details.openReservation(it) },
                onCustomerClick = { details.openCustomer(it) },
                onCreate = { creating = it.toDateKey() },
                modifier = Modifier.weight(1f),
            )
        } else key(view) {
            // 뷰마다 스크롤을 새로 잡는다(웹도 뷰 종류마다 새 노드 — 주에서 민 가로 위치가 남지 않게).
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

    if (notifying) {
        NotificationCenterSheet(
            upcoming = upcoming,
            customerNames = customerNames,
            assignees = remember(snapshot.assignees) { snapshot.assignees.associateBy { it.id } },
            assigneeColors = assigneeColors,
            onOpenReservation = { details.openReservation(it) },
            onDismiss = { notifying = false },
        )
    }

    creating?.let { day ->
        ReservationCreateSheet(
            store,
            onDismiss = { creating = null },
            initialDate = day.ifEmpty { null },
            initialStart = if (day.isEmpty()) null else "10:00",
        )
    }
}

/** 넓은 화면 왼쪽 메뉴가 캘린더에 보내는 요청. */
sealed interface CalendarCommand {
    data class SelectView(val view: CalendarView) : CalendarCommand
    data object Create : CalendarCommand
}
