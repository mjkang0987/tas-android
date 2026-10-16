package kr.co.takeaseat.app.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.LocalDate
import kotlinx.coroutines.delay
import kr.co.takeaseat.app.data.GuestStore
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
import kr.co.takeaseat.app.ui.components.hexColor

/** 캘린더 탭 — 지금은 일 뷰만(웹 `/day/yyyy/mm/dd`). */
@Composable
fun CalendarScreen(store: GuestStore, modifier: Modifier = Modifier) {
    val snapshot by store.snapshot.collectAsState()
    var dateKey by rememberSaveable { mutableStateOf(LocalDate.now().toDateKey()) }
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
    val items = remember(snapshot.reservations, dateKey, customers, firstVisit, assigneeColors) {
        placeReservations(snapshot.reservations.filter { it.date == dateKey }).map { placed ->
            val r = placed.reservation
            TimelineCardItem(
                placed = placed,
                color = hexColor(r.assigneeId?.let { assigneeColors[it] } ?: UNASSIGNED_COLOR),
                customerName = customers[r.customerId]?.name,
                isNewCustomer = isNewCustomerVisit(firstVisit[r.customerId], r.date),
            )
        }
    }

    val scrollState = rememberScrollState()
    Column(modifier.fillMaxSize()) {
        CalendarHeader(date = date, onDateChange = { dateKey = it.toDateKey() })
        DayTimeline(
            hours = timelineRange(snapshot.storeSettings.businessHours),
            scale = scale,
            items = items,
            serviceColorMap = serviceColorMap,
            isToday = date == today,
            closedKind = storeClosedKind(snapshot.storeSettings, dateKey),
            scrollState = scrollState,
            modifier = Modifier.weight(1f),
        )
    }
}
