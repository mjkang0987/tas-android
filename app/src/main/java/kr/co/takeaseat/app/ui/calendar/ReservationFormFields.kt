package kr.co.takeaseat.app.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.Assignee
import kr.co.takeaseat.app.domain.FormField
import kr.co.takeaseat.app.domain.LocalDbSnapshot
import kr.co.takeaseat.app.domain.RESERVATION_CHANNELS
import kr.co.takeaseat.app.domain.ReservationForm
import kr.co.takeaseat.app.domain.ServiceItem
import kr.co.takeaseat.app.domain.StoreLabels
import kr.co.takeaseat.app.domain.activeAssignees
import kr.co.takeaseat.app.domain.assigneeStatus
import kr.co.takeaseat.app.domain.sumDurationMinutes
import kr.co.takeaseat.app.domain.sumPrice
import kr.co.takeaseat.app.domain.toDateKey
import kr.co.takeaseat.app.ui.components.FieldError
import kr.co.takeaseat.app.ui.components.ServiceFields
import kr.co.takeaseat.app.ui.components.SystemDialogEffect
import kr.co.takeaseat.app.ui.components.TasSelect
import kr.co.takeaseat.app.ui.components.TasTextField
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes

/** 폼을 회전에도 살린다(직렬화 한 덩어리). */
internal val reservationFormSaver = Saver<ReservationForm, String>(
    save = { GuestStore.json.encodeToString(ReservationForm.serializer(), it) },
    restore = { GuestStore.json.decodeFromString(ReservationForm.serializer(), it) },
)

/**
 * 웹 `ReservationDetailSections` `ReservationFormFields` — 예약 추가·변경이 같이 쓰는 칸(서비스·가격·담당자·날짜·시간·예약경로·요청사항).
 * [currentAssignee] 는 변경할 때 지금 담당자 — 휴직·퇴직이어도 선택지에 남겨 둔다(웹과 같다, 바꾸면 다시 못 고른다).
 */
@Composable
internal fun ReservationFormFields(
    form: ReservationForm,
    onChange: (ReservationForm) -> Unit,
    snapshot: LocalDbSnapshot,
    labels: StoreLabels,
    catalogMap: Map<String, ServiceItem>,
    serviceColorMap: Map<String, String>,
    currentAssignee: Assignee?,
    errorFor: (FormField) -> String?,
    errorSeq: Int,
) {
    val active = activeAssignees(snapshot.assignees)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(labels.service, color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
        ServiceFields(
            catalog = snapshot.services,
            catalogMap = catalogMap,
            serviceColorMap = serviceColorMap,
            selected = form.selectedServices,
            onToggle = { onChange(form.toggleService(it, catalogMap)) },
            totalDuration = sumDurationMinutes(form.selectedServices, catalogMap),
            totalPrice = sumPrice(form.selectedServices, catalogMap),
        )
        errorFor(FormField.Service)?.let { FieldError(it, scrollKey = errorSeq) }
    }

    TasTextField(
        label = "가격",
        value = if (form.price == 0) "" else form.price.toString(),
        onValueChange = { onChange(form.changePrice(it)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        suffix = "원",
    )

    val keepCurrent = currentAssignee?.takeIf { assigneeStatus(it) != "재직" }
    if (active.isNotEmpty() || currentAssignee != null) {
        // 0 = 미지정. 새로 고를 수 있는 건 재직 담당자뿐 — 지금 담당자가 휴직·퇴직이면 그 사람만 남겨 둔다.
        val choices = listOfNotNull(keepCurrent) + active
        val ids = listOf(0) + choices.map { it.id }
        val names = listOf("미지정") + choices.map { if (it === keepCurrent) "${it.name} (${assigneeStatus(it)} · 신규 선택 불가)" else it.name }
        Column {
            TasSelect(
                label = labels.assignee,
                options = names,
                selectedIndex = ids.indexOf(form.assigneeId).coerceAtLeast(0),
                onSelect = { onChange(form.copy(assigneeId = ids[it])) },
                error = errorFor(FormField.Assignee),
                errorKey = errorSeq,
            )
            if (snapshot.assignees.any { assigneeStatus(it) != "재직" }) {
                Text(
                    "예약 화면에서는 재직 담당자만 새로 선택할 수 있습니다." + (keepCurrent?.let {
                        " 현재 담당자는 ${assigneeStatus(it)} 상태라 유지 가능하지만, 변경 후에는 다시 선택할 수 없습니다."
                    } ?: ""),
                    color = TasColors.Caution,
                    fontSize = TasSizes.XSmallFont,
                    lineHeight = TasSizes.XSmallFont * 1.5f,
                    // 한글 단어 중간("퇴/직")에서 줄이 바뀌지 않게 어절 단위로
                    style = LocalTextStyle.current.copy(lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }

    DateField(form.date, errorFor(FormField.Date), errorSeq) { onChange(form.copy(date = it)) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimeField("시작", form.startTime, Modifier.weight(1f)) { onChange(form.changeStartTime(it, catalogMap)) }
            TimeField("종료", form.endTime, Modifier.weight(1f)) { onChange(form.changeEndTime(it)) }
        }
        errorFor(FormField.Time)?.let { FieldError(it, scrollKey = errorSeq) }
    }

    TasSelect(
        label = "예약경로",
        options = RESERVATION_CHANNELS,
        selectedIndex = RESERVATION_CHANNELS.indexOf(form.channel).coerceAtLeast(0),
        onSelect = { onChange(form.copy(channel = RESERVATION_CHANNELS[it])) },
    )
    TasTextField(
        label = "요청사항",
        value = form.memo,
        onValueChange = { onChange(form.copy(memo = it)) },
        placeholder = "예약 관련 요청사항",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
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
