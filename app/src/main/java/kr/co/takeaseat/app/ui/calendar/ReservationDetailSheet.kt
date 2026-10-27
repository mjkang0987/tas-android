package kr.co.takeaseat.app.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import kr.co.takeaseat.app.domain.PaymentForm
import kr.co.takeaseat.app.domain.applyPayment
import kr.co.takeaseat.app.domain.completeReservation
import kr.co.takeaseat.app.domain.hasCompletedPayment
import kr.co.takeaseat.app.domain.open
import kr.co.takeaseat.app.domain.validatePayment
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.FormField
import kr.co.takeaseat.app.domain.DetailAction
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.ReservationForm
import kr.co.takeaseat.app.domain.ReservationStatus
import kr.co.takeaseat.app.domain.StoreLabels
import kr.co.takeaseat.app.domain.UNASSIGNED_COLOR
import kr.co.takeaseat.app.domain.applyEdit
import kr.co.takeaseat.app.domain.assigneeColor
import kr.co.takeaseat.app.domain.buildCatalogMap
import kr.co.takeaseat.app.domain.changeReservationStatus
import kr.co.takeaseat.app.domain.changedFields
import kr.co.takeaseat.app.domain.detailActions
import kr.co.takeaseat.app.domain.fromReservation
import kr.co.takeaseat.app.domain.isPastStart
import kr.co.takeaseat.app.domain.restoreReservation
import kr.co.takeaseat.app.domain.servicePriceTotal
import kr.co.takeaseat.app.domain.validateEdit
import kr.co.takeaseat.app.ui.components.ActionButton
import kr.co.takeaseat.app.ui.components.ActionTone
import kr.co.takeaseat.app.ui.components.DetailSheet
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.ui.theme.TasColors

// 웹 ReservationDetailMode 중 지금 있는 것. 예약 전환은 따로 띄우는 확인창(restoring).
private enum class DetailMode { View, Cancelling, Noshow, Editing, Confirming, PastConfirm, Payment, Completing }

private val paymentFormSaver = Saver<PaymentForm, String>(
    save = { GuestStore.json.encodeToString(PaymentForm.serializer(), it) },
    restore = { GuestStore.json.decodeFromString(PaymentForm.serializer(), it) },
)

/**
 * 웹 `overlays/ReservationDetail.tsx` — 예약 상세 시트. 보기 · 취소/노쇼 확인 · 변경(폼 → 바뀐 칸 확인) · 예약 전환 확인창.
 * 규칙은 `domain/ReservationStatusChange.kt`·`ReservationEdit.kt`, 여기는 모드를 바꾸고 그리기만 한다.
 */
@Composable
internal fun ReservationDetailSheet(
    store: GuestStore,
    reservation: Reservation,
    isNewCustomer: Boolean,
    serviceColorMap: Map<String, String>,
    labels: StoreLabels,
    onCustomerClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val snapshot by store.snapshot.collectAsState()
    val customer = snapshot.customers.firstOrNull { it.id == reservation.customerId }
    // 웹은 담당자 번호 0 을 미지정으로 본다
    val assignee = reservation.assigneeId?.takeIf { it != 0 }?.let { id -> snapshot.assignees.firstOrNull { it.id == id } }
    val catalogMap = remember(snapshot.services) { buildCatalogMap(snapshot.services) }
    val knownNames = serviceColorMap.keys

    var mode by rememberSaveable(reservation.id) { mutableStateOf(DetailMode.View) }
    var restoring by rememberSaveable(reservation.id) { mutableStateOf(false) }
    var historyOpen by rememberSaveable(reservation.id) { mutableStateOf(false) }
    val history = snapshot.history.filter { it.reservationId == reservation.id }
    var form by rememberSaveable(reservation.id, stateSaver = reservationFormSaver) { mutableStateOf(ReservationForm.fromReservation(reservation, knownNames, snapshot.services)) }
    var errorField by rememberSaveable(reservation.id) { mutableStateOf<FormField?>(null) }
    var errorMessage by rememberSaveable(reservation.id) { mutableStateOf("") }
    var errorSeq by rememberSaveable(reservation.id) { mutableIntStateOf(0) }
    val displayPrice = reservation.price ?: servicePriceTotal(reservation.service, snapshot.services)
    val pointSettings = snapshot.storeSettings.pointSettings
    var payment by rememberSaveable(reservation.id, stateSaver = paymentFormSaver) { mutableStateOf(PaymentForm.open(reservation, displayPrice, pointSettings)) }
    var paymentError by rememberSaveable(reservation.id) { mutableStateOf<String?>(null) }
    var paymentErrorSeq by rememberSaveable(reservation.id) { mutableIntStateOf(0) } // 같은 오류로 다시 눌러도 다시 보이게
    fun edit(next: ReservationForm) {
        form = next
        errorField = null
    }
    fun errorFor(field: FormField) = errorMessage.takeIf { errorField == field }
    fun startEditing() {
        form = ReservationForm.fromReservation(reservation, knownNames, snapshot.services) // 웹: 열 때마다 저장된 값으로 다시
        errorField = null
        mode = DetailMode.Editing
    }

    val customerName = customer?.name ?: "-"
    val actions = detailActions(reservation, customerDeleted = customer?.deleted == true)
    // 웹 dialogTitle — 보기·변경은 고객 이름, 나머지 모드는 모드 이름(MODE_LABELS)
    val title = when (mode) {
        DetailMode.View, DetailMode.Editing -> customer?.name ?: "예약 상세"
        DetailMode.Confirming, DetailMode.PastConfirm -> "변경 확인"
        DetailMode.Cancelling -> "예약 취소"
        DetailMode.Noshow -> "노쇼 처리"
        DetailMode.Payment -> "결제 처리"
        DetailMode.Completing -> "예약 완료"
    }
    DetailSheet(
        title = title,
        onDismiss = onDismiss,
        footer = if (mode == DetailMode.View && actions.isEmpty()) null else {
            {
                when (mode) {
                    DetailMode.View -> actions.forEach { action ->
                        when (action) {
                            DetailAction.Pay -> {
                                // 채운 색은 주 동작 하나만 — 미결제면 결제완료, 결제 후엔 예약완료(웹과 같다)
                                val paid = hasCompletedPayment(reservation)
                                ActionButton(if (paid) "결제수단 변경" else "결제완료", tone = if (paid) ActionTone.Default else ActionTone.Primary, onClick = {
                                    payment = PaymentForm.open(reservation, displayPrice, pointSettings)
                                    paymentError = null
                                    mode = DetailMode.Payment
                                })
                            }
                            DetailAction.Complete -> ActionButton("예약완료", tone = ActionTone.Primary, onClick = { mode = DetailMode.Completing })
                            DetailAction.Restore -> ActionButton("예약전환", tone = ActionTone.Primary, onClick = { restoring = true })
                            DetailAction.Cancel -> ActionButton("취소", tone = ActionTone.DangerOutline, onClick = { mode = DetailMode.Cancelling })
                            DetailAction.Noshow -> ActionButton("노쇼", tone = ActionTone.DangerOutline, onClick = { mode = DetailMode.Noshow })
                            DetailAction.Edit -> ActionButton("변경", onClick = ::startEditing)
                        }
                    }
                    DetailMode.Cancelling, DetailMode.Noshow -> {
                        ActionButton("취소", onClick = { mode = DetailMode.View })
                        val status = if (mode == DetailMode.Cancelling) ReservationStatus.CANCELLED else ReservationStatus.NOSHOW
                        ActionButton(if (mode == DetailMode.Cancelling) "예약 취소" else "노쇼 처리", tone = ActionTone.Danger, onClick = {
                            store.update { changeReservationStatus(it, reservation.id, status) }
                            onDismiss() // 웹: 취소·노쇼하면 상세가 닫힌다
                        })
                    }
                    DetailMode.Editing -> {
                        ActionButton("취소", onClick = { mode = DetailMode.View })
                        ActionButton("저장", tone = ActionTone.Primary, onClick = {
                            val error = validateEdit(reservation, form, store.snapshot.value, labels)
                            when {
                                error != null -> {
                                    errorField = error.field
                                    errorMessage = error.message
                                    errorSeq++
                                }
                                // 웹: 바뀐 게 없으면 확인 없이 보기로
                                changedFields(reservation, form, snapshot.assignees, labels, knownNames, snapshot.services).isEmpty() -> mode = DetailMode.View
                                isPastStart(form, LocalDateTime.now()) -> mode = DetailMode.PastConfirm
                                else -> mode = DetailMode.Confirming
                            }
                        })
                    }
                    DetailMode.Payment -> {
                        ActionButton("취소", onClick = { mode = DetailMode.View })
                        ActionButton("결제 저장", tone = ActionTone.Primary, onClick = {
                            val error = validatePayment(payment, reservation, displayPrice, customer?.points ?: 0, labels)
                            if (error != null) {
                                paymentError = error
                                paymentErrorSeq++
                            } else {
                                store.update { applyPayment(it, reservation.id, payment) }
                                paymentError = null
                                mode = DetailMode.View
                            }
                        })
                    }
                    DetailMode.Completing -> {
                        ActionButton("취소", onClick = { mode = DetailMode.View })
                        ActionButton("예약완료", tone = ActionTone.Primary, onClick = {
                            store.update { completeReservation(it, reservation.id) }
                            mode = DetailMode.View
                        })
                    }
                    DetailMode.Confirming, DetailMode.PastConfirm -> {
                        ActionButton("취소", onClick = { mode = DetailMode.Editing })
                        ActionButton("저장", tone = ActionTone.Primary, onClick = {
                            store.update { applyEdit(it, reservation, form, knownNames) }
                            mode = DetailMode.View // 웹: 저장하면 상세(보기)로 남는다
                        })
                    }
                }
            }
        },
    ) {
        when (mode) {
            DetailMode.View -> ReservationDetailView(
                reservation = reservation,
                customer = customer,
                isNewCustomer = isNewCustomer,
                displayPrice = displayPrice,
                assigneeName = assignee?.name ?: "미지정",
                assigneeColor = hexColor(assignee?.let { assigneeColor(it) } ?: UNASSIGNED_COLOR),
                serviceColorMap = serviceColorMap,
                labels = labels,
                historyCount = history.size,
                onOpenHistory = { historyOpen = true },
                onCustomerClick = onCustomerClick,
            )
            DetailMode.Cancelling -> ReservationConfirmSummary("이 예약을 취소하시겠습니까?", TasColors.Danger, reservation, customerName, labels)
            DetailMode.Noshow -> ReservationConfirmSummary("이 예약을 노쇼 처리하시겠습니까?", TasColors.Warning, reservation, customerName, labels)
            DetailMode.Editing -> {
                EditMemoSection(customer?.memoTags.orEmpty())
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReservationFormFields(
                        form = form,
                        onChange = ::edit,
                        snapshot = snapshot,
                        labels = labels,
                        catalogMap = catalogMap,
                        serviceColorMap = serviceColorMap,
                        currentAssignee = assignee,
                        errorFor = ::errorFor,
                        errorSeq = errorSeq,
                    )
                }
            }
            DetailMode.Payment -> ReservationPaymentSection(
                form = payment,
                onChange = {
                    payment = it
                    paymentError = null
                },
                settings = pointSettings,
                displayPrice = displayPrice,
                naverDeposit = reservation.naverDeposit ?: 0,
                customerPoints = customer?.points ?: 0,
                error = paymentError,
                errorKey = paymentErrorSeq,
            )
            DetailMode.Completing -> ReservationConfirmSummary("이 예약을 완료 처리하시겠습니까?", TasColors.Blue, reservation, customerName, labels)
            DetailMode.Confirming -> ReservationDiffSummary("수정하시겠습니까?", changedFields(reservation, form, snapshot.assignees, labels, knownNames, snapshot.services))
            DetailMode.PastConfirm -> ReservationDiffSummary(
                "현재 시간보다 과거입니다. 변경하시겠습니까?",
                changedFields(reservation, form, snapshot.assignees, labels, knownNames, snapshot.services),
                TasColors.Caution,
            )
        }
    }
    if (historyOpen) {
        ReservationHistorySheet(history, snapshot.assignees, labels, onDismiss = { historyOpen = false })
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
                    store.update { restoreReservation(it, reservation.id) } // 웹: 되돌린 뒤 상세는 열어 둔다
                })
            },
        ) {
            ReservationConfirmSummary("취소된 예약을 되돌리시겠습니까?", TasColors.Blue, reservation, customerName, labels, boxed = false)
        }
    }
}
