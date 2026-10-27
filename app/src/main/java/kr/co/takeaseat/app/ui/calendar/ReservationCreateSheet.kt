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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import kr.co.takeaseat.app.domain.CREATE_PAYMENT_METHODS
import kr.co.takeaseat.app.domain.PointSettings
import kr.co.takeaseat.app.domain.createPointPreview
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.ui.theme.drawBehindThemed
import kr.co.takeaseat.app.domain.FormError
import kr.co.takeaseat.app.domain.FormField
import kr.co.takeaseat.app.domain.ReservationForm
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


/**
 * 웹 `overlays/ReservationCreate.tsx` + `useReservationCreateForm` — 예약 추가 시트.
 * 규칙(바꾸기·검사·저장)은 `domain/ReservationCreate.kt`, 여기는 칸을 그리고 부르기만 한다.
 * 웹 모바일 '+ 예약'처럼 오늘·지금(30분 반올림)으로 연다. 월 목록의 ＋ 는 그날 10:00 으로([initialDate]·[initialStart]).
 */
@Composable
fun ReservationCreateSheet(store: GuestStore, onDismiss: () -> Unit, initialDate: String? = null, initialStart: String? = null) {
    val snapshot by store.snapshot.collectAsState()
    val labels = remember(snapshot.shopType) { storeLabels(snapshot.shopType) }
    val catalogMap = remember(snapshot.services) { buildCatalogMap(snapshot.services) }
    val serviceColorMap = remember(snapshot.services, snapshot.categoryBaseColors) { buildServiceColorMap(snapshot.services, snapshot.categoryBaseColors) }
    val active = remember(snapshot.assignees) { activeAssignees(snapshot.assignees) }
    val customers = remember(snapshot.customers) { sortCustomersByName(snapshot.customers.filter(::isSelectableCustomer)) }

    var form by rememberSaveable(stateSaver = reservationFormSaver) {
        mutableStateOf(ReservationForm.initial(initialDate ?: LocalDate.now().toDateKey(), initialStart ?: roundedStartTime(LocalDateTime.now()), snapshot.assignees))
    }
    var errorField by rememberSaveable { mutableStateOf<FormField?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var errorSeq by rememberSaveable { mutableIntStateOf(0) } // 저장 실패마다 +1 — 같은 오류도 다시 보이는 곳으로 스크롤
    var showSuggestions by rememberSaveable { mutableStateOf(false) }
    fun edit(next: ReservationForm) {
        form = next
        errorField = null
    }
    fun errorFor(field: FormField) = errorMessage.takeIf { errorField == field }

    DetailSheet(
        title = "예약 추가",
        onDismiss = onDismiss,
        footer = {
            ActionButton("취소", onClick = onDismiss)
            ActionButton("저장", tone = ActionTone.Primary, onClick = {
                val error: FormError? = validateCreate(form, store.snapshot.value, labels)
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
                    error = errorFor(FormField.Customer),
                    errorKey = errorSeq,
                )
            }

            ReservationFormFields(
                form = form,
                onChange = ::edit,
                snapshot = snapshot,
                labels = labels,
                catalogMap = catalogMap,
                serviceColorMap = serviceColorMap,
                currentAssignee = null,
                errorFor = ::errorFor,
                errorSeq = errorSeq,
            )
            CreatePaymentFields(form, ::edit, snapshot.storeSettings.pointSettings, errorFor(FormField.Payment), errorSeq)
        }
    }
}

/**
 * '결제완료로 등록' — 웹 추가 화면엔 없는 앱 기능(iOS 와 같다). 생김새는 웹 결제 칸('적립 적용' 체크 행·고르는 칸)을 따른다.
 * 켜면 결제수단(기본 카드) · 안내 한 줄. 저장은 상세 결제와 같은 규칙([createPaymentForm]).
 */
@Composable
private fun CreatePaymentFields(form: ReservationForm, onChange: (ReservationForm) -> Unit, settings: PointSettings, error: String?, errorSeq: Int) {
    Column(
        Modifier.fillMaxWidth().drawBehindThemed { colors -> drawRect(colors.LightGray, size = Size(size.width, 1.dp.toPx())) }.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.toggleable(form.markPaid, role = Role.Checkbox) { onChange(form.copy(markPaid = it)) }.heightIn(min = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = form.markPaid, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = TasColors.BrandFill), modifier = Modifier.size(20.dp))
            Text("결제완료로 등록", color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
        }
        if (form.markPaid) {
            TasSelect(
                label = "결제수단",
                options = CREATE_PAYMENT_METHODS,
                selectedIndex = CREATE_PAYMENT_METHODS.indexOf(form.payMethod),
                onSelect = { onChange(form.copy(payMethod = CREATE_PAYMENT_METHODS[it])) },
            )
            val earned = createPointPreview(form, settings)
            Text(
                if (earned != null) "결제 ${formatPrice(form.price)} · 적립률 ${settings.serviceRate.toInt()}% → ${formatPrice(earned)} 적립"
                else "결제 ${formatPrice(form.price)}으로 결제완료 처리됩니다." // 금액은 늘 "원"(받침)으로 끝나 "으로",
                color = TasColors.DarkGray2,
                fontSize = TasSizes.SmallFont,
            )
        }
        if (error != null) FieldError(error, scrollKey = errorSeq)
    }
}
