package kr.co.takeaseat.app.ui.calendar

import androidx.compose.foundation.layout.Column
import kr.co.takeaseat.app.domain.DetailAction
import kr.co.takeaseat.app.domain.ReservationStatus
import kr.co.takeaseat.app.domain.changeReservationStatus
import kr.co.takeaseat.app.domain.detailActions
import kr.co.takeaseat.app.domain.restoreReservation
import kr.co.takeaseat.app.ui.components.ActionButton
import kr.co.takeaseat.app.ui.components.ActionTone
import kr.co.takeaseat.app.ui.theme.TasColors
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
import kr.co.takeaseat.app.domain.assigneeColor
import kr.co.takeaseat.app.domain.servicePriceTotal
import kr.co.takeaseat.app.domain.storeLabels
import kr.co.takeaseat.app.ui.components.DetailSheet
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
    // 열린 예약 상세 — 회전해도 유지
    var detailId by rememberSaveable { mutableStateOf<Int?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val labels = remember(snapshot.shopType) { storeLabels(snapshot.shopType) }

    Column(modifier.fillMaxSize()) {
        CalendarHeader(date = date, onDateChange = { dateKey = it.toDateKey() }, onCreate = { creating = true })
        DayTimeline(
            hours = timelineRange(snapshot.storeSettings.businessHours),
            scale = scale,
            items = items,
            serviceColorMap = serviceColorMap,
            isToday = date == today,
            closedKind = storeClosedKind(snapshot.storeSettings, dateKey),
            scrollState = scrollState,
            onReservationClick = { detailId = it },
            modifier = Modifier.weight(1f),
        )
    }

    val detail = detailId?.let { id -> snapshot.reservations.firstOrNull { it.id == id } }
    if (detail != null) {
        val customer = customers[detail.customerId]
        // 웹은 담당자 번호 0 을 미지정으로 본다
        val assignee = detail.assigneeId?.takeIf { it != 0 }?.let { id -> snapshot.assignees.firstOrNull { it.id == id } }
        // 웹 ReservationDetailMode 중 지금 있는 것: 보기 · 취소 확인 · 노쇼 확인. 예약 전환은 따로 띄우는 확인창.
        var mode by rememberSaveable(detail.id) { mutableStateOf(DetailMode.View) }
        var restoring by rememberSaveable(detail.id) { mutableStateOf(false) }
        val customerName = customer?.name ?: "-"
        val actions = detailActions(detail)
        DetailSheet(
            title = customer?.name ?: "예약 상세",
            onDismiss = { detailId = null },
            footer = if (mode == DetailMode.View && actions.isEmpty()) null else {
                {
                    when (mode) {
                        DetailMode.View -> actions.forEach { action ->
                            when (action) {
                                DetailAction.Restore -> ActionButton("예약전환", tone = ActionTone.Primary, onClick = { restoring = true })
                                DetailAction.Cancel -> ActionButton("취소", tone = ActionTone.DangerOutline, onClick = { mode = DetailMode.Cancelling })
                                DetailAction.Noshow -> ActionButton("노쇼", tone = ActionTone.DangerOutline, onClick = { mode = DetailMode.Noshow })
                            }
                        }
                        DetailMode.Cancelling, DetailMode.Noshow -> {
                            ActionButton("취소", onClick = { mode = DetailMode.View })
                            val status = if (mode == DetailMode.Cancelling) ReservationStatus.CANCELLED else ReservationStatus.NOSHOW
                            ActionButton(if (mode == DetailMode.Cancelling) "예약 취소" else "노쇼 처리", tone = ActionTone.Danger, onClick = {
                                store.update { changeReservationStatus(it, detail.id, status) }
                                detailId = null // 웹: 취소·노쇼하면 상세가 닫힌다
                            })
                        }
                    }
                }
            },
        ) {
            when (mode) {
                DetailMode.View -> ReservationDetailView(
                    reservation = detail,
                    customer = customer,
                    isNewCustomer = isNewCustomerVisit(firstVisit[detail.customerId], detail.date),
                    displayPrice = detail.price ?: servicePriceTotal(detail.service, snapshot.services),
                    assigneeName = assignee?.name ?: "미지정",
                    assigneeColor = hexColor(assignee?.let { assigneeColor(it) } ?: UNASSIGNED_COLOR),
                    serviceColorMap = serviceColorMap,
                    labels = labels,
                )
                DetailMode.Cancelling -> ReservationConfirmSummary("이 예약을 취소하시겠습니까?", TasColors.Danger, detail, customerName, labels)
                DetailMode.Noshow -> ReservationConfirmSummary("이 예약을 노쇼 처리하시겠습니까?", TasColors.Warning, detail, customerName, labels)
            }
        }
        if (restoring) {
            DetailSheet(
                title = "예약 전환",
                onDismiss = { restoring = false },
                centered = true,
                footer = {
                    ActionButton("취소", onClick = { restoring = false })
                    ActionButton("예약전환", tone = ActionTone.Primary, onClick = {
                        restoring = false
                        store.update { restoreReservation(it, detail.id) } // 웹: 되돌린 뒤 상세는 열어 둔다
                    })
                },
            ) {
                ReservationConfirmSummary("취소된 예약을 되돌리시겠습니까?", TasColors.Blue, detail, customerName, labels, boxed = false)
            }
        }
    }

    if (creating) ReservationCreateSheet(store, onDismiss = { creating = false })
}

private enum class DetailMode { View, Cancelling, Noshow }
