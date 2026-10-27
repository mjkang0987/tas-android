package kr.co.takeaseat.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.buildServiceColorMap
import kr.co.takeaseat.app.domain.firstVisitDates
import kr.co.takeaseat.app.domain.isNewCustomerVisit
import kr.co.takeaseat.app.domain.storeLabels
import kr.co.takeaseat.app.ui.calendar.ReservationDetailSheet
import kr.co.takeaseat.app.ui.customers.CustomerDetailSheet

/**
 * 연 상세 창(예약 "r12" · 고객 "c3") 목록에 하나 더 연다. 이미 열려 있으면 그 위 창들을 닫아 그 창으로 돌아간다 —
 * 같은 창이 두 장 뜨지 않게, 그리고 창은 처음 그린 순서대로 쌓여 목록 순서만 바꿔서는 위로 올라오지 않기 때문이다.
 */
internal fun pushDetail(stack: List<String>, key: String): List<String> {
    val at = stack.indexOf(key)
    return if (at >= 0) stack.take(at + 1) else stack + key
}

/** 화면 하나의 상세 창 목록 — 회전해도 남게 글자로 둔다. 예약 상세에서 고객, 고객 상세에서 예약을 여는 길이 서로 얽혀 연 순서대로 쌓는다. */
class DetailStack internal constructor(private val state: MutableState<List<String>>) {
    val entries: List<String> get() = state.value
    fun openReservation(id: Int) { state.value = pushDetail(state.value, "r$id") }
    fun openCustomer(id: Int) { state.value = pushDetail(state.value, "c$id") }
    fun close(key: String) { state.value = state.value - key }
}

@Composable
fun rememberDetailStack(): DetailStack {
    val state = rememberSaveable { mutableStateOf(listOf<String>()) }
    return remember(state) { DetailStack(state) }
}

/** [stack] 을 연 순서대로 그린다(창은 그린 순서대로 위에 뜬다). 지운 예약·고객은 목록에서 뺀다. */
@Composable
fun DetailSheets(store: GuestStore, stack: DetailStack) {
    val snapshot by store.snapshot.collectAsState()
    val serviceColorMap = remember(snapshot.services, snapshot.categoryBaseColors) { buildServiceColorMap(snapshot.services, snapshot.categoryBaseColors) }
    val labels = remember(snapshot.shopType) { storeLabels(snapshot.shopType) }
    val firstVisits = remember(snapshot.reservations) { firstVisitDates(snapshot.reservations) }
    stack.entries.forEach { entry ->
        key(entry) {
            val id = entry.drop(1).toInt()
            if (entry.startsWith("c")) {
                CustomerDetailSheet(store, id, onReservationClick = { stack.openReservation(it.id) }, onDismiss = { stack.close(entry) })
            } else {
                val r = snapshot.reservations.firstOrNull { it.id == id }
                if (r == null) {
                    LaunchedEffect(entry) { stack.close(entry) }
                } else {
                    ReservationDetailSheet(
                        store = store,
                        reservation = r,
                        isNewCustomer = isNewCustomerVisit(firstVisits[r.customerId], r.date),
                        serviceColorMap = serviceColorMap,
                        labels = labels,
                        onCustomerClick = { stack.openCustomer(r.customerId) },
                        onDismiss = { stack.close(entry) },
                    )
                }
            }
        }
    }
}
