package kr.co.takeaseat.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.ASSIGNEE_STATUSES
import kr.co.takeaseat.app.domain.Assignee
import kr.co.takeaseat.app.domain.CLOSED_WEEKDAY_LABELS
import kr.co.takeaseat.app.domain.addAssignee
import kr.co.takeaseat.app.domain.assigneeColor
import kr.co.takeaseat.app.domain.assigneeStatus
import kr.co.takeaseat.app.domain.deleteAssignee
import kr.co.takeaseat.app.domain.splitAssigneesByStatus
import kr.co.takeaseat.app.domain.storeLabels
import kr.co.takeaseat.app.domain.summarizeSchedule
import kr.co.takeaseat.app.domain.updateAssignee
import kr.co.takeaseat.app.domain.updateAssigneeDay
import kr.co.takeaseat.app.domain.withNameI18n
import kr.co.takeaseat.app.ui.components.ActionButton
import kr.co.takeaseat.app.ui.components.ActionTone
import kr.co.takeaseat.app.ui.components.DetailSheet
import kr.co.takeaseat.app.ui.components.LabelBadge
import kr.co.takeaseat.app.ui.components.LabelTone
import kr.co.takeaseat.app.ui.components.PageHero
import kr.co.takeaseat.app.ui.components.ScreenHeader
import kr.co.takeaseat.app.ui.components.TasSelect
import kr.co.takeaseat.app.ui.components.TasTextField
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.ui.customers.MemoColorPalette
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 `/settings/assignee`(AssigneeManageSection, 모바일) — 재직자 · 휴직자 · 퇴직자 카드, 아래 '{담당자} 추가'.
 * 웹처럼 고치는 즉시 저장한다('완료'는 수정 모드를 닫을 뿐). 퇴직은 확인창, 퇴직자만 영구 삭제(예약은 미지정으로).
 * 색은 웹 브라우저 색 고르기 대신 메모 태그 8색 중에서. 규칙은 `domain/AssigneeManage.kt`.
 */
@Composable
fun AssigneeManageScreen(store: GuestStore, modifier: Modifier = Modifier) {
    val snapshot by store.snapshot.collectAsState()
    val labels = remember(snapshot.shopType) { storeLabels(snapshot.shopType) }
    val (active, onLeave, resigned) = remember(snapshot.assignees) { splitAssigneesByStatus(snapshot.assignees) }
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var resignTarget by rememberSaveable { mutableStateOf<Int?>(null) }
    var deleteTarget by rememberSaveable { mutableStateOf<Int?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    fun change(id: Int, f: (Assignee) -> Assignee) = store.update { updateAssignee(it, id, f) }

    Column(modifier.fillMaxSize()) {
        ScreenHeader("${labels.assignee} 관리")
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            PageHero("ASSIGNEE", "${labels.assignee} 정보, 근무 일정, 재직 상태를 관리합니다.", PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 18.dp))
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf("재직자" to active, "휴직자" to onLeave, "퇴직자" to resigned).forEach { (title, list) ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(title, color = TasColors.DarkGray, fontSize = TasSizes.LargeFont, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                        if (list.isEmpty()) {
                            Text("등록된 데이터가 없습니다", color = TasColors.DarkGray2, fontSize = TasSizes.MediumFont, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(24.dp))
                        }
                        list.forEach { a ->
                            AssigneeCard(
                                assignee = a,
                                editing = editingId == a.id,
                                assigneeLabel = labels.assignee,
                                onEdit = { editingId = a.id },
                                onFinish = { editingId = null },
                                onChange = { f -> change(a.id, f) },
                                onDay = { day, f -> store.update { updateAssigneeDay(it, a.id, day, f) } },
                                onResign = { resignTarget = a.id },
                                onPermanentDelete = if (assigneeStatus(a) == "퇴직") ({ deleteTarget = a.id }) else null,
                            )
                        }
                    }
                }
                if (adding) {
                    AddAssigneeForm(labels.assignee, nextId = (snapshot.assignees.maxOfOrNull { it.id } ?: 0) + 1, onCancel = { adding = false }) { name, status, phone, note, color ->
                        store.update { addAssignee(it, name, status, phone, note, color) }
                        adding = false
                    }
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { SmallButton("${labels.assignee} 추가") { adding = true } }
                }
            }
        }
    }

    // 확인창 대상이 사라졌으면(다른 데서 지움) 그리지 않는다
    resignTarget?.let { id -> snapshot.assignees.firstOrNull { it.id == id } }?.let { a ->
        val id = a.id
        ConfirmSheet(
            title = "${a.name} 퇴직 처리할까요?",
            message = "새 예약에서 고를 수 없게 되고 근무시간 설정이 숨겨집니다.\n기존 예약은 그대로 남고, 나중에 다시 재직으로 바꿀 수 있습니다.",
            confirm = "퇴직",
            onDismiss = { resignTarget = null },
        ) {
            change(id) { it.copy(status = "퇴직") }
            editingId = null; resignTarget = null
        }
    }
    deleteTarget?.let { id -> snapshot.assignees.firstOrNull { it.id == id } }?.let { a ->
        val id = a.id
        val count = snapshot.reservations.count { it.assigneeId == id }
        ConfirmSheet(
            title = "${a.name} 삭제할까요?",
            message = (if (count > 0) "이 ${labels.assignee}의 예약 ${count}건은 미지정으로 남고, " else "") +
                "${labels.assignee} 정보와 근무 일정은 완전히 삭제됩니다.\n되돌릴 수 없습니다.",
            confirm = "삭제",
            onDismiss = { deleteTarget = null },
        ) {
            store.update { deleteAssignee(it, id) }
            editingId = null; deleteTarget = null
        }
    }
}

/** 웹 ConfirmDialog(danger) — 가운데 확인창, 취소 · 빨간 확인. */
@Composable
private fun ConfirmSheet(title: String, message: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    DetailSheet(
        title = title,
        onDismiss = onDismiss,
        centered = true,
        footer = {
            ActionButton("취소", onClick = onDismiss)
            ActionButton(confirm, onClick = onConfirm, tone = ActionTone.Danger)
        },
    ) { Text(message, color = TasColors.DarkGray, fontSize = TasSizes.MediumFont, lineHeight = 20.sp) }
}

private fun statusTone(status: String) = when (status) {
    "휴직" -> LabelTone.Warning
    "퇴직" -> LabelTone.Neutral
    else -> LabelTone.Success
}

/**
 * 웹 StyledAssigneeCard — 담당자 색 테두리(44)·왼쪽 막대 4·10 둥글게·흰 → 색(10) 바탕, 수정 중 파란 2 테두리.
 * 머리: 색 점 · 이름(또는 칸) · 상태 배지(또는 고르기) · 오른쪽 '수정' / '삭제'·'완료' / 퇴직자 '영구 삭제'.
 */
@Composable
private fun AssigneeCard(
    assignee: Assignee,
    editing: Boolean,
    assigneeLabel: String,
    onEdit: () -> Unit,
    onFinish: () -> Unit,
    onChange: ((Assignee) -> Assignee) -> Unit,
    onDay: (Int, (kr.co.takeaseat.app.domain.DaySchedule) -> kr.co.takeaseat.app.domain.DaySchedule) -> Unit,
    onResign: () -> Unit,
    onPermanentDelete: (() -> Unit)?,
) {
    val color = hexColor(assigneeColor(assignee))
    val status = assigneeStatus(assignee)
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (editing) Modifier.border(2.dp, TasColors.Blue, shape).padding(2.dp) else Modifier)
            .dropShadow(shape, Shadow(radius = 10.dp, color = Color(15, 23, 42).copy(alpha = 0.05f), offset = DpOffset(0.dp, 4.dp)))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.96f), color.copy(alpha = 0x10 / 255f))))
            .drawBehind {
                val w = 1.dp.toPx()
                drawRoundRect(color.copy(alpha = 0x44 / 255f), topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w), cornerRadius = CornerRadius(10.dp.toPx()), style = Stroke(w))
                drawRect(color, size = Size(4.dp.toPx(), size.height))
            }
            .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (editing) {
                // 웹 모바일: 이름 칸은 왼쪽 폭을 다 쓰고 상태 고르기는 다음 줄로(버튼이 많은 퇴직자 카드에서 이름 칸이 찌그러졌다)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                        TasTextField(label = "${assigneeLabel}명", value = assignee.name, onValueChange = { v -> onChange { it.copy(name = v) } },
                            placeholder = "${assigneeLabel}명", showLabel = false, compact = true, modifier = Modifier.weight(1f))
                    }
                    TasSelect(label = "${assignee.name} 상태", options = ASSIGNEE_STATUSES, selectedIndex = ASSIGNEE_STATUSES.indexOf(status), showLabel = false, compact = true,
                        modifier = Modifier.padding(start = 16.dp).width(80.dp), onSelect = { i ->
                            val next = ASSIGNEE_STATUSES[i]
                            // 웹: 퇴직은 바로 바꾸지 않고 확인창부터
                            if (next == status) Unit else if (next == "퇴직") onResign() else onChange { it.copy(status = next) }
                        })
                }
            } else {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                // 이름 + 상태 배지가 남는 폭을 차지하고 버튼은 오른쪽 끝에(이름이 길면 말줄임)
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(assignee.name, color = TasColors.DarkGray, fontSize = TasSizes.LargeFont, fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    LabelBadge(status, statusTone(status), pill = true, fontSize = TasSizes.TinyFont)
                }
            }
            if (editing) {
                ActionButton("삭제", onClick = onResign, tone = ActionTone.DangerSoft)
                ActionButton("완료", onClick = onFinish)
            } else {
                SmallButton("수정", onEdit)
            }
            onPermanentDelete?.let { ActionButton("영구 삭제", onClick = it, tone = ActionTone.DangerSoft) }
        }
        if (editing) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TasTextField(label = "연락처", value = assignee.phone.orEmpty(), onValueChange = { v -> onChange { it.copy(phone = v) } },
                    placeholder = "010-0000-0000", compact = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(1f))
                TasTextField(label = "메모", value = assignee.note.orEmpty(), onValueChange = { v -> onChange { it.copy(note = v) } },
                    placeholder = "특이사항 메모", compact = true, modifier = Modifier.weight(1f))
            }
            MetaLabel("컬러")
            MemoColorPalette(assigneeColor(assignee), onColor = { c -> onChange { it.copy(color = c) } })
            // 공개 예약 페이지 다국어 이름(선택). 비우면 위 이름이 그대로.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("en" to "English", "ja" to "日本語", "zh" to "中文").forEach { (code, label) ->
                    val value = when (code) { "en" -> assignee.nameI18n?.en; "ja" -> assignee.nameI18n?.ja; else -> assignee.nameI18n?.zh }.orEmpty()
                    TasTextField(label = label, value = value, onValueChange = { v -> onChange { it.copy(nameI18n = withNameI18n(it.nameI18n, code, v)) } },
                        placeholder = label, compact = true, modifier = Modifier.weight(1f))
                }
            }
        } else {
            // 보기: 값 있는 것만(색은 머리 점이 보여 준다)
            val fields = listOf("연락처" to assignee.phone, "메모" to assignee.note, "English" to assignee.nameI18n?.en, "日本語" to assignee.nameI18n?.ja, "中文" to assignee.nameI18n?.zh)
                .filter { !it.second.isNullOrBlank() }
            if (fields.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    fields.forEach { (label, value) ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            MetaLabel(label)
                            Text(value.orEmpty(), color = TasColors.DarkGray, fontSize = TasSizes.MediumFont)
                        }
                    }
                }
            }
        }
        when {
            status != "재직" -> StatusNotice(status)
            editing -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CLOSED_WEEKDAY_LABELS.forEachIndexed { index, label ->
                    val day = assignee.schedule.getOrNull(index) ?: return@forEachIndexed
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(label, color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.width(20.dp))
                        // 웹 StyledDaySwitch — 체크 + '근무'/'휴무'
                        Row(
                            Modifier.heightIn(min = 30.dp).toggleable(day.enabled, role = Role.Checkbox) { on -> onDay(index) { it.copy(enabled = on) } },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Checkbox(checked = day.enabled, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = TasColors.Brand), modifier = Modifier.size(18.dp))
                            Text(if (day.enabled) "근무" else "휴무", color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont, modifier = Modifier.width(26.dp))
                        }
                        TimeField("$label 시작", day.start, day.enabled, Modifier.weight(1f), showLabel = false) { t -> onDay(index) { it.copy(start = t) } }
                        Text("~", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont)
                        TimeField("$label 종료", day.end, day.enabled, Modifier.weight(1f), showLabel = false) { t -> onDay(index) { it.copy(end = t) } }
                    }
                }
            }
            else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                summarizeSchedule(assignee.schedule).forEach { (days, hours) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(days, color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
                        Text(hours, color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont)
                    }
                }
            }
        }
    }
}

/** 웹 StyledScheduleCollapsedNotice — 상태 색 점선 테두리 안내. */
@Composable
private fun StatusNotice(status: String) {
    val tone = statusTone(status)
    Text(
        if (status == "휴직") "휴직 상태에서는 근무시간 설정을 접어 둡니다. 복귀 후 다시 조정할 수 있습니다."
        else "퇴직 상태에서는 예약 선택이 비활성화되며 근무시간 설정을 표시하지 않습니다.",
        color = tone.text,
        fontSize = TasSizes.XSmallFont,
        lineHeight = TasSizes.XSmallFont * 1.5f,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TasColors.White60)
            .drawBehind {
                val w = 1.dp.toPx()
                drawRoundRect(tone.border, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w), cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
private fun MetaLabel(text: String) = Text(text, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)

/** 웹 StyledAddForm — 이름 · 상태 · 연락처 · 색 / 메모 / 취소 · 추가(이름 비면 아무것도 안 함). */
@Composable
private fun AddAssigneeForm(assigneeLabel: String, nextId: Int, onCancel: () -> Unit, onAdd: (String, String, String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("재직") }
    var phone by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf(assigneeColor(Assignee(id = nextId, name = ""))) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(TasColors.White).border(1.dp, TasColors.LightGray, RoundedCornerShape(10.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TasTextField(label = "${assigneeLabel}명", value = name, onValueChange = { name = it }, placeholder = "새 ${assigneeLabel}명", compact = true, modifier = Modifier.weight(1f))
            TasSelect(label = "상태", options = ASSIGNEE_STATUSES, selectedIndex = ASSIGNEE_STATUSES.indexOf(status), onSelect = { status = ASSIGNEE_STATUSES[it] },
                compact = true, modifier = Modifier.width(80.dp))
        }
        TasTextField(label = "연락처", value = phone, onValueChange = { phone = it }, placeholder = "연락처", compact = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        MetaLabel("컬러")
        MemoColorPalette(color, onColor = { color = it })
        TasTextField(label = "메모", value = note, onValueChange = { note = it }, placeholder = "특이사항 메모", compact = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
            ActionButton("취소", onClick = onCancel)
            ActionButton("추가", onClick = { if (name.isNotBlank()) onAdd(name, status, phone, note, color) }, tone = ActionTone.Primary)
        }
    }
}
