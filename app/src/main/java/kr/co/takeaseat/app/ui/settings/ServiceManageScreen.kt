package kr.co.takeaseat.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.NEW_CATEGORY
import kr.co.takeaseat.app.domain.ServiceForm
import kr.co.takeaseat.app.domain.ServiceItem
import kr.co.takeaseat.app.domain.addService
import kr.co.takeaseat.app.domain.buildServiceColorMap
import kr.co.takeaseat.app.domain.categoryBaseColor
import kr.co.takeaseat.app.domain.deleteService
import kr.co.takeaseat.app.domain.formatDuration
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.groupedCatalog
import kr.co.takeaseat.app.domain.renameCategory
import kr.co.takeaseat.app.domain.resolvedCategory
import kr.co.takeaseat.app.domain.setCategoryColor
import kr.co.takeaseat.app.domain.storeLabels
import kr.co.takeaseat.app.domain.toDateKey
import kr.co.takeaseat.app.domain.toItem
import kr.co.takeaseat.app.domain.updateService
import kr.co.takeaseat.app.domain.validateCategoryName
import kr.co.takeaseat.app.domain.validateServiceAdd
import kr.co.takeaseat.app.domain.validateServiceEdit
import kr.co.takeaseat.app.ui.components.ActionButton
import kr.co.takeaseat.app.ui.components.ActionTone
import kr.co.takeaseat.app.ui.components.ChevronDownIcon
import kr.co.takeaseat.app.ui.components.DetailSheet
import kr.co.takeaseat.app.ui.components.FieldError
import kr.co.takeaseat.app.ui.components.PageHero
import kr.co.takeaseat.app.ui.components.ScreenHeader
import kr.co.takeaseat.app.ui.components.ServiceChipList
import kr.co.takeaseat.app.ui.components.TasSelect
import kr.co.takeaseat.app.ui.components.TasTextField
import kr.co.takeaseat.app.ui.customers.MemoColorPalette
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 `/settings/service`(ServiceManageSection, 모바일) — 소개 · 카테고리 묶음(접기 · '수정' = 이름·대표색) · 항목 줄(누르면 수정) · '+ 추가'.
 * 순서 바꾸기(끌기)는 웹도 모바일 터치에선 안 돼 넣지 않았다. 대표색은 메모 태그 8색 중에서(안드로이드엔 기본 색 고르기가 없다).
 */
@Composable
fun ServiceManageScreen(store: GuestStore, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val snapshot by store.snapshot.collectAsState()
    val labels = remember(snapshot.shopType) { storeLabels(snapshot.shopType) }
    val grouped = remember(snapshot.services) { groupedCatalog(snapshot.services) }
    val serviceColorMap = remember(snapshot.services, snapshot.categoryBaseColors) { buildServiceColorMap(snapshot.services, snapshot.categoryBaseColors) }
    var closed by rememberSaveable { mutableStateOf(listOf<String>()) }
    var editingCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryName by rememberSaveable { mutableStateOf("") }
    var manageError by rememberSaveable { mutableStateOf<String?>(null) }
    var editingName by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    Column(modifier.fillMaxSize()) {
        ScreenHeader("${labels.service} 관리")
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item { PageHero("SERVICE", "${labels.service} 카테고리와 항목을 등록하고 가격을 설정합니다.", PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 18.dp)) }
            if (grouped.isEmpty()) {
                item {
                    Text("등록된 데이터가 없습니다", color = TasColors.DarkGray2, fontSize = TasSizes.MediumFont, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(40.dp))
                }
            }
            grouped.forEach { (category, services) ->
                item(key = "c:$category") {
                    val open = category !in closed
                    CategoryHeader(
                        category = category,
                        open = open,
                        editing = editingCategory == category,
                        onToggle = { closed = if (open) closed + category else closed - category },
                        onEdit = { editingCategory = category; categoryName = category; manageError = null },
                    )
                    if (editingCategory == category) {
                        CategoryEditRow(
                            name = categoryName,
                            onName = { categoryName = it },
                            color = categoryBaseColor(category, snapshot.categoryBaseColors),
                            // 웹: 색은 고르는 즉시 바뀐다(저장 버튼과 따로)
                            onColor = { c -> store.update { setCategoryColor(it, category, c) } },
                            onSave = {
                                val error = validateCategoryName(grouped.keys, category, categoryName)
                                if (error != null) manageError = error
                                else {
                                    store.update { renameCategory(it, category, categoryName) }
                                    editingCategory = null; categoryName = ""; manageError = null
                                }
                            },
                            onCancel = { editingCategory = null; categoryName = ""; manageError = null },
                            // 오류는 고치는 줄 바로 아래 — 목록 맨 아래에 두면 서비스가 많을 때 화면 밖이라 안 보이고 안 읽혔다
                            error = manageError,
                        )
                    }
                }
                if (category !in closed) {
                    items(services, key = { "s:${it.name}" }) { item ->
                        ServiceRow(item, serviceColorMap) { editingName = item.name }
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 10.dp)) {
                    AddServiceButton("+ ${labels.service} 추가") { adding = true }
                }
            }
            item { Box(Modifier.height(16.dp)) }
        }
    }

    editingName?.let { name ->
        val item = snapshot.services.firstOrNull { it.name == name }
        if (item != null) {
            ServiceSheet(
                serviceLabel = labels.service,
                original = item,
                categories = grouped.keys.toList(),
                catalog = snapshot.services,
                onDismiss = { editingName = null },
                onSave = { updated ->
                    var applied = 0
                    store.update { s -> updateService(s, item.name, updated, LocalDate.now().toDateKey()).also { applied = it.appliedCount }.snapshot }
                    editingName = null
                    toast(if (applied > 0) "${labels.service}가 저장되었습니다. 예정된 예약 ${applied}건에 반영했습니다." else "${labels.service}가 저장되었습니다.")
                },
                onDelete = {
                    store.update { deleteService(it, item.name) }
                    editingName = null
                    toast("\"${item.name}\" ${labels.service}가 삭제되었습니다.")
                },
            )
        }
    }
    if (adding) {
        ServiceSheet(
            serviceLabel = labels.service,
            original = null,
            categories = grouped.keys.toList(),
            catalog = snapshot.services,
            onDismiss = { adding = false },
            onSave = { item ->
                store.update { addService(it, item) }
                adding = false
                toast("${labels.service}가 추가되었습니다.")
            },
            onDelete = {},
        )
    }
}

/** 웹 StyledCategoryHeader — 이름(11 굵게) · 접기 동그라미 · 오른쪽 '수정'(편집 중엔 숨김). 36 높이. */
@Composable
private fun CategoryHeader(category: String, open: Boolean, editing: Boolean, onToggle: () -> Unit, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp).heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 36.dp)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = if (open) "펼침" else "접힘" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(category, color = TasColors.DarkGray, fontSize = TasSizes.XSmallFont, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
            Box(Modifier.size(20.dp).clip(CircleShape).border(1.dp, TasColors.LightGray, CircleShape), contentAlignment = Alignment.Center) {
                Icon(ChevronDownIcon, contentDescription = null, tint = TasColors.DarkGray, modifier = Modifier.size(12.dp).rotate(if (open) 0f else -90f))
            }
        }
        if (!editing) SmallButton("수정", onEdit)
    }
}

/** 웹 StyledCategoryEditRow — 이름 칸 · 대표색 · 저장 · 취소. */
@Composable
private fun CategoryEditRow(name: String, onName: (String) -> Unit, color: String, onColor: (String) -> Unit, onSave: () -> Unit, onCancel: () -> Unit, error: String?) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TasTextField(label = "카테고리명", value = name, onValueChange = onName, placeholder = "카테고리명", showLabel = false, compact = true, error = error)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            MemoColorPalette(color, onColor)
            ActionButton("저장", onClick = onSave, tone = ActionTone.Primary)
            ActionButton("취소", onClick = onCancel)
        }
    }
}

/** 웹 StyledItem — 40 높이, 아래 1px 옅은 회색. 서비스 색 칩 · 소요시간(11 회색) · 가격(12 굵게, 0원이면 숨김). 누르면 수정. */
@Composable
private fun ServiceRow(item: ServiceItem, serviceColorMap: Map<String, String>, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .drawBehind { drawRect(TasColors.LightGray, topLeft = Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx())) }
            .clickable(role = Role.Button, onClickLabel = "수정", onClick = onClick)
            .padding(start = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ServiceChipList(item.name, serviceColorMap)
        val duration = formatDuration(item.durationMinutes)
        if (duration.isNotEmpty()) Text(duration, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
        if (item.price > 0) Text(formatPrice(item.price), color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
    }
}

/** 웹 StyledEditBtn — 30 높이, 6 둥글게, 11 글자, 1px 옅은 회색. */
@Composable
private fun SmallButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier.heightIn(min = 30.dp).clip(shape).border(1.dp, TasColors.LightGray, shape).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = TasColors.DarkGray, fontSize = TasSizes.XSmallFont, fontWeight = FontWeight.Medium) }
}

/** 웹 StyledAddButton — 꽉 찬 폭, 30 높이, 1px 점선, 6 둥글게, 13. */
@Composable
private fun AddServiceButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 30.dp)
            .clip(RoundedCornerShape(6.dp))
            .drawBehind {
                val w = 1.dp.toPx()
                drawRoundRect(
                    TasColors.LightGray, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w), cornerRadius = CornerRadius(6.dp.toPx()),
                    style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))),
                )
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = TasColors.DarkGray, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.Medium) }
}

private val serviceFormSaver = Saver<ServiceForm, String>(
    save = { GuestStore.json.encodeToString(ServiceForm.serializer(), it) },
    restore = { GuestStore.json.decodeFromString(ServiceForm.serializer(), it) },
)

/**
 * 웹 ServiceAddModal / ServiceEditModal — 추가는 카테고리(+ 새 카테고리)부터, 수정은 카테고리 없이 삭제(확인창) 포함.
 * 이름 · 다국어 이름(English·日本語·中文) · 소요시간(분) · 가격(원). 검사는 `domain/ServiceManage.kt`.
 */
@Composable
private fun ServiceSheet(
    serviceLabel: String,
    original: ServiceItem?,
    categories: List<String>,
    catalog: List<ServiceItem>,
    onDismiss: () -> Unit,
    onSave: (ServiceItem) -> Unit,
    onDelete: () -> Unit,
) {
    var form by rememberSaveable(original?.name, stateSaver = serviceFormSaver) { mutableStateOf(original?.let { ServiceForm.of(it) } ?: ServiceForm()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var errorSeq by rememberSaveable { mutableIntStateOf(0) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    fun update(next: ServiceForm) { form = next; error = null }
    fun submit() {
        val message = if (original == null) validateServiceAdd(form, catalog, serviceLabel) else validateServiceEdit(form, original.name, catalog, serviceLabel)
        if (message != null) { error = message; errorSeq++; return }
        onSave(form.toItem(original?.category ?: form.resolvedCategory()))
    }

    DetailSheet(
        title = "$serviceLabel ${if (original == null) "추가" else "수정"}",
        onDismiss = onDismiss,
        footer = {
            if (original != null) ActionButton("삭제", onClick = { confirmDelete = true }, tone = ActionTone.Danger)
            ActionButton("취소", onClick = onDismiss)
            ActionButton(if (original == null) "추가" else "저장", onClick = ::submit, tone = ActionTone.Primary)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (original == null) {
                val options = categories + "+ 새 카테고리"
                val selected = if (form.category == NEW_CATEGORY) categories.size else categories.indexOf(form.category)
                TasSelect(
                    label = "카테고리",
                    options = options,
                    selectedIndex = selected,
                    onSelect = { i -> update(form.copy(category = if (i == categories.size) NEW_CATEGORY else categories[i])) },
                    placeholder = "카테고리 선택",
                )
                if (form.category == NEW_CATEGORY) {
                    TasTextField(label = "새 카테고리명", value = form.newCategory, onValueChange = { update(form.copy(newCategory = it)) },
                        placeholder = "새 카테고리명", showLabel = false)
                }
            }
            TasTextField(label = "${serviceLabel}명", value = form.name, onValueChange = { update(form.copy(name = it)) }, placeholder = "${serviceLabel}명")
            // 웹 StyledI18nBlock — 제목 · 설명 · 언어별 칸
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("다국어 이름 (선택)", color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
                Text("고객 예약 페이지에서 해당 언어로 보일 이름입니다. 비우면 위 이름이 그대로 표시됩니다.", color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
                TasTextField(label = "English", value = form.nameEn, onValueChange = { update(form.copy(nameEn = it)) }, placeholder = "English", compact = true)
                TasTextField(label = "日本語", value = form.nameJa, onValueChange = { update(form.copy(nameJa = it)) }, placeholder = "日本語", compact = true)
                TasTextField(label = "中文", value = form.nameZh, onValueChange = { update(form.copy(nameZh = it)) }, placeholder = "中文", compact = true)
            }
            TasTextField(label = "소요시간 (분)", value = form.duration, onValueChange = { v -> update(form.copy(duration = v.filter { it.isDigit() }.take(4))) },
                placeholder = "분", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TasTextField(label = "가격 (원)", value = form.price, onValueChange = { v -> update(form.copy(price = v.filter { it.isDigit() }.take(9))) },
                placeholder = "원", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            error?.let { FieldError(it, scrollKey = errorSeq) }
        }
    }

    if (confirmDelete && original != null) {
        DetailSheet(
            title = "$serviceLabel 삭제",
            onDismiss = { confirmDelete = false },
            centered = true,
            footer = {
                ActionButton("취소", onClick = { confirmDelete = false })
                ActionButton("삭제", onClick = onDelete, tone = ActionTone.Danger)
            },
        ) {
            Text("\"${original.name}\" ${serviceLabel}를 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.", color = TasColors.DarkGray, fontSize = TasSizes.MediumFont)
        }
    }
}
