package kr.co.takeaseat.app.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.CreateError
import kr.co.takeaseat.app.domain.CreateField
import kr.co.takeaseat.app.domain.CreateForm
import kr.co.takeaseat.app.domain.RESERVATION_CHANNELS
import kr.co.takeaseat.app.domain.activeAssignees
import kr.co.takeaseat.app.domain.applyCreate
import kr.co.takeaseat.app.domain.assigneeStatus
import kr.co.takeaseat.app.domain.buildCatalogMap
import kr.co.takeaseat.app.domain.buildServiceColorMap
import kr.co.takeaseat.app.domain.isSelectableCustomer
import kr.co.takeaseat.app.domain.roundedStartTime
import kr.co.takeaseat.app.domain.sortCustomersByName
import kr.co.takeaseat.app.domain.storeLabels
import kr.co.takeaseat.app.domain.sumDurationMinutes
import kr.co.takeaseat.app.domain.sumPrice
import kr.co.takeaseat.app.domain.toDateKey
import kr.co.takeaseat.app.domain.validateCreate
import kr.co.takeaseat.app.ui.components.ActionButton
import kr.co.takeaseat.app.ui.components.ActionTone
import kr.co.takeaseat.app.ui.components.CustomerAutocomplete
import kr.co.takeaseat.app.ui.components.DetailSheet
import kr.co.takeaseat.app.ui.components.FieldError
import kr.co.takeaseat.app.ui.components.ServiceFields
import kr.co.takeaseat.app.ui.components.SystemDialogEffect
import kr.co.takeaseat.app.ui.components.TasSelect
import kr.co.takeaseat.app.ui.components.TasTextField
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes

private val formSaver = Saver<CreateForm, String>(
    save = { GuestStore.json.encodeToString(CreateForm.serializer(), it) },
    restore = { GuestStore.json.decodeFromString(CreateForm.serializer(), it) },
)

/**
 * 웹 `overlays/ReservationCreate.tsx` + `useReservationCreateForm` — 예약 추가 시트.
 * 규칙(바꾸기·검사·저장)은 `domain/ReservationCreate.kt`, 여기는 칸을 그리고 부르기만 한다.
 * 웹 모바일 '+ 예약'처럼 오늘·지금(30분 반올림)으로 연다.
 */
@Composable
fun ReservationCreateSheet(store: GuestStore, onDismiss: () -> Unit) {
    val snapshot by store.snapshot.collectAsState()
    val labels = remember(snapshot.shopType) { storeLabels(snapshot.shopType) }
    val catalogMap = remember(snapshot.services) { buildCatalogMap(snapshot.services) }
    val serviceColorMap = remember(snapshot.services, snapshot.categoryBaseColors) { buildServiceColorMap(snapshot.services, snapshot.categoryBaseColors) }
    val active = remember(snapshot.assignees) { activeAssignees(snapshot.assignees) }
    val customers = remember(snapshot.customers) { sortCustomersByName(snapshot.customers.filter(::isSelectableCustomer)) }

    var form by rememberSaveable(stateSaver = formSaver) {
        mutableStateOf(CreateForm.initial(LocalDate.now().toDateKey(), roundedStartTime(LocalDateTime.now()), snapshot.assignees))
    }
    var errorField by rememberSaveable { mutableStateOf<CreateField?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var errorSeq by rememberSaveable { mutableIntStateOf(0) } // 저장 실패마다 +1 — 같은 오류도 다시 보이는 곳으로 스크롤
    var showSuggestions by rememberSaveable { mutableStateOf(false) }
    fun edit(next: CreateForm) {
        form = next
        errorField = null
    }
    fun errorFor(field: CreateField) = errorMessage.takeIf { errorField == field }

    DetailSheet(
        title = "예약 추가",
        onDismiss = onDismiss,
        footer = {
            ActionButton("취소", onClick = onDismiss)
            ActionButton("저장", tone = ActionTone.Primary, onClick = {
                val error: CreateError? = validateCreate(form, store.snapshot.value, labels)
                if (error != null) {
                    errorField = error.field
                    errorMessage = error.message
                    errorSeq++
                } else {
                    store.update { applyCreate(form, it) }
                    onDismiss()
                }
            })
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 고객(웹 ReservationCreateCustomerFields — 간격 8)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val query = form.customerQuery
                CustomerAutocomplete(
                    label = "고객명",
                    placeholder = "고객명 검색 또는 신규 입력",
                    query = query,
                    customers = remember(customers, query) {
                        if (query.isBlank()) customers else customers.filter { it.name.contains(query) || it.tel.contains(query) }
                    },
                    selectedId = form.customerId,
                    showSuggestions = showSuggestions,
                    onQueryChange = {
                        edit(form.changeCustomerQuery(it))
                        showSuggestions = true
                    },
                    onFocusChange = { showSuggestions = it },
                    onSelect = {
                        edit(form.selectCustomer(it))
                        showSuggestions = false
                    },
                )
                TasTextField(
                    label = "연락처",
                    value = form.customerTel,
                    onValueChange = { edit(form.copy(customerTel = it)) },
                    placeholder = "01012345678",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    error = errorFor(CreateField.Customer),
                    errorKey = errorSeq,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(labels.service, color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
                ServiceFields(
                    catalog = snapshot.services,
                    catalogMap = catalogMap,
                    serviceColorMap = serviceColorMap,
                    selected = form.selectedServices,
                    onToggle = { edit(form.toggleService(it, catalogMap)) },
                    totalDuration = sumDurationMinutes(form.selectedServices, catalogMap),
                    totalPrice = sumPrice(form.selectedServices, catalogMap),
                )
                errorFor(CreateField.Service)?.let { FieldError(it, scrollKey = errorSeq) }
            }

            TasTextField(
                label = "가격",
                value = if (form.price == 0) "" else form.price.toString(),
                onValueChange = { edit(form.changePrice(it)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                suffix = "원",
            )

            if (active.isNotEmpty()) {
                // 0 = 미지정. 고를 수 있는 건 재직 담당자뿐.
                val ids = listOf(0) + active.map { it.id }
                Column {
                    TasSelect(
                        label = labels.assignee,
                        options = listOf("미지정") + active.map { it.name },
                        selectedIndex = ids.indexOf(form.assigneeId).coerceAtLeast(0),
                        onSelect = { edit(form.copy(assigneeId = ids[it])) },
                        error = errorFor(CreateField.Assignee),
                        errorKey = errorSeq,
                    )
                    if (snapshot.assignees.any { assigneeStatus(it) != "재직" }) {
                        Text(
                            "예약 화면에서는 재직 담당자만 새로 선택할 수 있습니다.",
                            color = TasColors.Caution,
                            fontSize = TasSizes.XSmallFont,
                            lineHeight = TasSizes.XSmallFont * 1.5f,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }

            DateField(form.date, errorFor(CreateField.Date), errorSeq) { edit(form.copy(date = it)) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeField("시작", form.startTime, Modifier.weight(1f)) { edit(form.changeStartTime(it, catalogMap)) }
                    TimeField("종료", form.endTime, Modifier.weight(1f)) { edit(form.changeEndTime(it)) }
                }
                errorFor(CreateField.Time)?.let { FieldError(it, scrollKey = errorSeq) }
            }

            TasSelect(
                label = "예약경로",
                options = RESERVATION_CHANNELS,
                selectedIndex = RESERVATION_CHANNELS.indexOf(form.channel).coerceAtLeast(0),
                onSelect = { edit(form.copy(channel = RESERVATION_CHANNELS[it])) },
            )
            TasTextField(
                label = "요청사항",
                value = form.memo,
                onValueChange = { edit(form.copy(memo = it)) },
                placeholder = "예약 관련 요청사항",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }
    }
}

/** 웹 `<input type="date">` 자리 — 누르면 시스템 날짜 선택. */
@Composable
private fun DateField(value: String, error: String?, errorKey: Int, onChange: (String) -> Unit) {
    val date = try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        LocalDate.now()
    }
    var picking by rememberSaveable { mutableStateOf(false) }
    if (picking) {
        SystemDialogEffect(onDismissed = { picking = false }) {
            DatePickerDialog(it, { _, y, m, d -> onChange(LocalDate.of(y, m + 1, d).toDateKey()) }, date.year, date.monthValue - 1, date.dayOfMonth)
        }
    }
    TasSelect(
        label = "날짜",
        valueText = "%d. %02d. %02d.".format(date.year, date.monthValue, date.dayOfMonth),
        valueDescription = "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일",
        onClick = { picking = true },
        error = error,
        errorKey = errorKey,
        centered = true,
    )
}

/** 웹 `<input type="time">` 자리 — 누르면 시스템 시간 선택(24시간). */
@Composable
private fun TimeField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    val (h, m) = value.split(':').map { it.toIntOrNull() ?: 0 }.let { it.getOrElse(0) { 0 } to it.getOrElse(1) { 0 } }
    var picking by rememberSaveable { mutableStateOf(false) }
    if (picking) {
        SystemDialogEffect(onDismissed = { picking = false }) {
            TimePickerDialog(it, { _, hour, minute -> onChange("%02d:%02d".format(hour, minute)) }, h, m, true)
        }
    }
    TasSelect(
        label = label,
        valueText = value,
        valueDescription = "${h}시 ${m}분",
        onClick = { picking = true },
        modifier = modifier,
        centered = true,
    )
}
