package kr.co.takeaseat.app.ui.settings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.CATEGORY_NAMES
import kr.co.takeaseat.app.domain.CLOSED_WEEKDAY_LABELS
import kr.co.takeaseat.app.domain.SHOP_INDUSTRIES
import kr.co.takeaseat.app.domain.StoreBusinessHours
import kr.co.takeaseat.app.domain.addClosedDate
import kr.co.takeaseat.app.domain.formatDateLabel
import kr.co.takeaseat.app.domain.primaryIndustry
import kr.co.takeaseat.app.domain.saveStoreInfo
import kr.co.takeaseat.app.ui.components.ActionButton
import kr.co.takeaseat.app.ui.components.ActionTone
import kr.co.takeaseat.app.ui.components.FieldError
import kr.co.takeaseat.app.ui.components.LocalToast
import kr.co.takeaseat.app.ui.components.PageHero
import kr.co.takeaseat.app.ui.components.ScreenHeader
import kr.co.takeaseat.app.ui.components.SystemDialogEffect
import kr.co.takeaseat.app.ui.components.TasSelect
import kr.co.takeaseat.app.ui.components.TasTextField
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 `/settings/store`(StoreManageSection, 게스트 모바일) — 매장 정보 · 영업시간 · 정기 휴무 · 휴업일 · 기능 사용 카드.
 * 카드마다 '수정' → '취소'·'저장'(기능 사용은 누르는 즉시). 규칙은 `domain/StoreManage.kt`.
 * 매장명 다국어 칸은 고객 예약 서비스를 켠 매장만 보여 게스트엔 없다.
 */
@Composable
fun StoreManageScreen(store: GuestStore, modifier: Modifier = Modifier) {
    val snapshot by store.snapshot.collectAsState()
    val toast = LocalToast.current
    val saved = snapshot.storeSettings
    Column(modifier.fillMaxSize()) {
        ScreenHeader("매장 관리")
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            PageHero("STORE", "영업시간과 휴업일을 설정합니다.", PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 18.dp))
            Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StoreInfoCard(snapshot.storeName, snapshot.shopType.orEmpty()) { name, type ->
                    var error: String? = null
                    store.update { s -> saveStoreInfo(s, name, type).also { error = it.second }.first }
                    if (error == null) toast.show("매장 정보가 저장되었습니다.")
                    error
                }
                BusinessHoursCard(saved.businessHours) { hours ->
                    store.update { it.copy(storeSettings = it.storeSettings.copy(businessHours = hours)) }
                    toast.show("영업시간이 저장되었습니다.")
                }
                ClosedWeekdaysCard(saved.closedWeekdays) { days ->
                    store.update { it.copy(storeSettings = it.storeSettings.copy(closedWeekdays = days)) }
                    toast.show("정기 휴무가 저장되었습니다.")
                }
                ClosedDatesCard(saved.closedDates) { dates ->
                    store.update { it.copy(storeSettings = it.storeSettings.copy(closedDates = dates)) }
                    toast.show("휴업일이 저장되었습니다.")
                }
                FeatureCard(snapshot.usePointSystem == true) { on ->
                    store.update { it.copy(usePointSystem = on) }
                    toast.show(if (on) "적립금 시스템을 켰습니다." else "적립금 시스템을 껐습니다.")
                }
            }
        }
    }
}

/** 웹 StyledStoreCard — 1px 옅은 회색, 8 둥글게, 그림자 sm, 안 10 8, 사이 10. 머리 = 제목(16 굵게) + 오른쪽 버튼. */
@Composable
private fun StoreCard(title: String, actions: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .dropShadow(shape, Shadow(radius = TasSizes.ShadowSmBlur, color = TasColors.ShadowSm, offset = DpOffset(0.dp, TasSizes.ShadowSmOffsetY)))
            .clip(shape)
            .background(TasColors.White)
            .border(1.dp, TasColors.LightGray, shape)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 30.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = TasColors.DarkGray, fontSize = TasSizes.LargeFont, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).semantics { heading() })
            actions()
        }
        content()
    }
}

/** 카드 머리 버튼 — 보기: '수정', 수정 중: '취소'·'저장'([saveEnabled] 아니면 흐리게, 웹 disabled). */
@Composable
private fun EditActions(editing: Boolean, saveEnabled: Boolean = true, onEdit: () -> Unit, onCancel: () -> Unit, onSave: () -> Unit) {
    if (!editing) {
        SmallButton("수정", onEdit)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton("취소", onClick = onCancel)
            ActionButton("저장", onClick = onSave, tone = ActionTone.Primary, enabled = saveEnabled)
        }
    }
}

@Composable
private fun StoreInfoCard(storeName: String?, shopType: String, onSave: (name: String, shopType: String) -> String?) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val current = primaryIndustry(shopType)
    StoreCard("매장 정보", actions = {
        EditActions(
            editing,
            onEdit = { name = storeName.orEmpty(); type = current?.value.orEmpty(); error = null; editing = true },
            onCancel = { error = null; editing = false },
            onSave = { error = onSave(name, type); if (error == null) editing = false },
        )
    }) {
        if (editing) {
            TasTextField(label = "매장 이름", value = name, onValueChange = { name = it; error = null }, placeholder = "매장 이름을 입력하세요", error = error)
            val chosen = SHOP_INDUSTRIES.firstOrNull { it.value == type }
            TasSelect(label = "업종", valueText = chosen?.let { "${it.emoji} ${it.label}" } ?: "선택 안 함", onClick = { picking = true }) {
                // 웹 <optgroup> — 분류 이름 아래 업종들
                DropdownMenu(expanded = picking, onDismissRequest = { picking = false }) {
                    DropdownMenuItem(text = { Text("선택 안 함", fontSize = TasSizes.MediumFont) }, onClick = { type = ""; picking = false })
                    CATEGORY_NAMES.forEach { (category, categoryName) ->
                        val items = SHOP_INDUSTRIES.filter { it.category == category }
                        if (items.isEmpty()) return@forEach
                        Text(categoryName, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).semantics { heading() })
                        items.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.emoji} ${item.label}", fontSize = TasSizes.MediumFont, fontWeight = if (item.value == type) FontWeight.SemiBold else null) },
                                onClick = { type = item.value; picking = false },
                            )
                        }
                    }
                }
            }
        } else {
            // 웹 StyledStoreInfoRow — 이름(16 굵게) · 업종 배지(13, 옅은 회색 알약)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (storeName.isNullOrBlank()) Text("매장 이름 없음", color = TasColors.DarkGray2, fontSize = TasSizes.MediumFont)
                else Text(storeName, color = TasColors.Black, fontSize = TasSizes.LargeFont, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                Text(
                    current?.let { "${it.emoji} ${it.label}" } ?: "업종 미설정",
                    color = if (current != null) TasColors.DarkGray else TasColors.DarkGray2,
                    fontSize = TasSizes.MediumFont,
                    modifier = Modifier.clip(RoundedCornerShape(percent = 50)).background(TasColors.Gray2).padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun BusinessHoursCard(saved: StoreBusinessHours, onSave: (StoreBusinessHours) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var start by rememberSaveable { mutableStateOf(saved.start) }
    var end by rememberSaveable { mutableStateOf(saved.end) }
    StoreCard("영업시간", actions = {
        EditActions(
            editing,
            saveEnabled = start != saved.start || end != saved.end,
            onEdit = { start = saved.start; end = saved.end; editing = true },
            onCancel = { editing = false },
            onSave = { onSave(StoreBusinessHours(start, end)); editing = false },
        )
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeField("오픈", if (editing) start else saved.start, editing, Modifier.weight(1f)) { start = it }
            TimeField("마감", if (editing) end else saved.end, editing, Modifier.weight(1f)) { end = it }
        }
    }
}

/** 웹 `<input type="time">`(수정 중에만) — 누르면 시스템 시간 고르기(24시간). */
@Composable
private fun TimeField(label: String, value: String, enabled: Boolean, modifier: Modifier, onPick: (String) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    if (picking) {
        SystemDialogEffect(onDismissed = { picking = false }) { context ->
            val (h, m) = value.split(':').map { it.toIntOrNull() ?: 0 }.let { it.getOrElse(0) { 0 } to it.getOrElse(1) { 0 } }
            TimePickerDialog(context, { _, hh, mm -> onPick("%02d:%02d".format(java.util.Locale.ROOT, hh, mm)) }, h, m, true)
        }
    }
    TasSelect(label = label, valueText = value, onClick = { picking = true }, enabled = enabled, modifier = modifier)
}

@Composable
private fun ClosedWeekdaysCard(saved: List<Int>, onSave: (List<Int>) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf(saved.sorted()) }
    val days = if (editing) draft else saved.sorted()
    StoreCard("정기 휴무", actions = {
        EditActions(
            editing,
            saveEnabled = draft != saved.sorted(),
            onEdit = { draft = saved.sorted(); editing = true },
            onCancel = { editing = false },
            onSave = { onSave(draft); editing = false },
        )
    }) {
        Text("매주 쉬는 요일을 선택하면 고객 예약 페이지에서 해당 요일은 예약할 수 없습니다.", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont, lineHeight = TasSizes.SmallFont * 1.5f)
        // 웹 StyledWeekdayRow — 7칸 같은 폭, 고르면 브랜드 색 채움(체크박스를 숨긴 라벨 = 낭독기엔 체크박스)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CLOSED_WEEKDAY_LABELS.forEachIndexed { index, label ->
                val on = index in days
                val shape = RoundedCornerShape(percent = 50)
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 30.dp)
                        .clip(shape)
                        .background(if (on) TasColors.Brand else TasColors.White)
                        .border(1.dp, if (on) TasColors.Brand else TasColors.LightGray, shape)
                        .toggleable(value = on, enabled = editing, role = Role.Checkbox) { checked ->
                            draft = if (checked) (draft + index).sorted() else draft - index
                        },
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = if (on) TasColors.White else TasColors.Black, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold) }
            }
        }
        if (days.isEmpty() && !editing) Empty("정기 휴무 없음")
    }
}

@Composable
private fun ClosedDatesCard(saved: List<String>, onSave: (List<String>) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf(saved) }
    val dates = if (editing) draft else saved
    var input by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    if (picking) {
        SystemDialogEffect(onDismissed = { picking = false }) { context ->
            val d = runCatching { LocalDate.parse(input) }.getOrElse { LocalDate.now() }
            DatePickerDialog(context, { _, y, m, day -> input = LocalDate.of(y, m + 1, day).toString(); error = null }, d.year, d.monthValue - 1, d.dayOfMonth)
        }
    }
    StoreCard("휴업일", actions = {
        EditActions(
            editing,
            saveEnabled = draft != saved,
            onEdit = { draft = saved; editing = true },
            onCancel = { input = ""; error = null; editing = false },
            onSave = { onSave(draft); editing = false },
        )
    }) {
        if (editing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TasSelect(
                    label = "휴업일", valueText = input.ifEmpty { "날짜 선택" },
                    valueDescription = if (input.isEmpty()) "날짜 선택" else formatDateLabel(input),
                    onClick = { picking = true }, showLabel = false, modifier = Modifier.weight(1f),
                )
                ActionButton("추가", onClick = {
                    val (next, message) = addClosedDate(draft, input)
                    if (message != null) error = message else { draft = next; input = ""; error = null }
                }, tone = ActionTone.Primary)
            }
            // 웹: 오류는 입력 줄 아래 한 줄(줄 안에 두면 '추가' 버튼이 오류 쪽으로 내려앉았다)
            error?.let { FieldError(it) }
        }
        if (dates.isEmpty()) {
            Empty("등록된 데이터가 없습니다")
        } else {
            // 웹 StyledClosedDateList — 위아래 1px 옅은 선, 줄 10 0, 13 진회색
            Column(Modifier.fillMaxWidth().drawBehind { drawRect(TasColors.Black10, size = Size(size.width, 1.dp.toPx())) }) {
                dates.forEach { date ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .drawBehind { drawRect(TasColors.Black10, topLeft = Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx())) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(formatDateLabel(date), color = TasColors.DarkGray, fontSize = TasSizes.MediumFont, modifier = Modifier.weight(1f))
                        if (editing) ActionButton("삭제", onClick = { draft = draft - date }, tone = ActionTone.DangerOutline)
                    }
                }
            }
        }
    }
}

/** 웹 StyledFeatureCard — 적립금 시스템만(회원권·쿠폰·고객 예약은 서버가 있어야 해 게스트엔 감춘다) + 게스트 안내. */
@Composable
private fun FeatureCard(usePointSystem: Boolean, onToggle: (Boolean) -> Unit) {
    StoreCard("기능 사용") {
        Row(
            Modifier.fillMaxWidth().toggleable(usePointSystem, role = Role.Checkbox, onValueChange = onToggle).padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Checkbox(checked = usePointSystem, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = TasColors.Brand), modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("적립금 시스템 사용", color = TasColors.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("결제 시 적립·선불 충전(선불금) 기능. 켜면 설정 메뉴에 ‘적립금 관리’가 나타납니다.", color = TasColors.DarkGray2,
                    fontSize = TasSizes.SmallFont, lineHeight = TasSizes.SmallFont * 1.5f)
            }
        }
        Text("게스트 모드에선 적립금만 사용할 수 있습니다. 회원권·쿠폰·고객 예약 서비스는 로그인 후 켤 수 있습니다.",
            color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont, lineHeight = TasSizes.SmallFont * 1.5f)
    }
}

@Composable
private fun Empty(text: String) {
    Text(text, color = TasColors.DarkGray2, fontSize = TasSizes.MediumFont, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(40.dp))
}

