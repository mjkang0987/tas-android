package kr.co.takeaseat.app.ui.customers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.CustomerEditForm
import kr.co.takeaseat.app.domain.MEMO_TAG_COLORS
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.UNASSIGNED_COLOR
import kr.co.takeaseat.app.domain.addTag
import kr.co.takeaseat.app.domain.applyCustomerEdit
import kr.co.takeaseat.app.domain.buildAssigneeColorMap
import kr.co.takeaseat.app.domain.buildServiceColorMap
import kr.co.takeaseat.app.domain.findTelDuplicate
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.formatTel
import kr.co.takeaseat.app.domain.normalizeTel
import kr.co.takeaseat.app.domain.noshowCount
import kr.co.takeaseat.app.domain.removeTag
import kr.co.takeaseat.app.domain.servicePriceTotal
import kr.co.takeaseat.app.domain.validateCustomerEdit
import kr.co.takeaseat.app.ui.calendar.ReservationInfoCard
import kr.co.takeaseat.app.ui.components.ActionButton
import kr.co.takeaseat.app.ui.components.ActionTone
import kr.co.takeaseat.app.ui.components.DetailSheet
import kr.co.takeaseat.app.ui.components.FieldError
import kr.co.takeaseat.app.ui.components.TasTextField
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes

private val editFormSaver = Saver<CustomerEditForm, String>(
    save = { GuestStore.json.encodeToString(CustomerEditForm.serializer(), it) },
    restore = { GuestStore.json.decodeFromString(CustomerEditForm.serializer(), it) },
)

// 웹 PAGE_SIZE — 예약 내역을 5건씩 더 보여 준다
private const val PAGE_SIZE = 5

/**
 * 웹 `overlays/CustomerDetail` — 고객 정보 시트. 보기(연락처·적립금·노쇼 / 고객 메모 / 적립금 이력 / 예약 내역)와 수정(이름·연락처·메모 태그).
 * 게스트라 삭제(사장 전용)·병합·분리는 없다(10-4 결정). 삭제(익명)된 고객은 보기만. 규칙은 `domain/CustomerDetail.kt`.
 */
@Composable
fun CustomerDetailSheet(store: GuestStore, customerId: Int, onReservationClick: (Reservation) -> Unit, onDismiss: () -> Unit) {
    val snapshot by store.snapshot.collectAsState()
    val customer = snapshot.customers.firstOrNull { it.id == customerId }
    if (customer == null) { LaunchedEffect(customerId) { onDismiss() }; return }
    // 웹: 날짜·시작 시간 오름차순(오래된 것부터)
    val reservations = remember(snapshot.reservations, customerId) {
        snapshot.reservations.filter { it.customerId == customerId }.sortedWith(compareBy({ it.date }, { it.startTime }))
    }
    val serviceColorMap = remember(snapshot.services, snapshot.categoryBaseColors) { buildServiceColorMap(snapshot.services, snapshot.categoryBaseColors) }
    val assigneeColors = remember(snapshot.assignees) { buildAssigneeColorMap(snapshot.assignees) }
    val assigneeNames = remember(snapshot.assignees) { snapshot.assignees.associate { it.id to it.name } }

    var editing by rememberSaveable(customerId) { mutableStateOf(false) }
    var form by rememberSaveable(customerId, stateSaver = editFormSaver) { mutableStateOf(CustomerEditForm.of(customer)) }
    var newTag by rememberSaveable(customerId) { mutableStateOf("") }
    var tagColor by rememberSaveable(customerId) { mutableStateOf(MEMO_TAG_COLORS[0].first) }
    var error by rememberSaveable(customerId) { mutableStateOf<String?>(null) }
    // 오류를 붙일 칸 — 'name'·'tel' 은 그 칸에(낭독기가 칸에서 읽게), 'tag' 는 메모 칸 아래(웹 editError 자리)
    var errorField by rememberSaveable(customerId) { mutableStateOf("tag") }
    var errorSeq by rememberSaveable(customerId) { mutableIntStateOf(0) }
    var dupWith by rememberSaveable(customerId) { mutableStateOf<Int?>(null) } // 같은 번호를 쓰는 다른 고객 번호
    var visibleCount by rememberSaveable(customerId) { mutableIntStateOf(PAGE_SIZE) }

    fun startEdit() { form = CustomerEditForm.of(customer); newTag = ""; error = null; editing = true }
    fun commit() { store.update { applyCustomerEdit(it, customerId, form) }; dupWith = null; error = null; editing = false }
    fun save() {
        validateCustomerEdit(form)?.let { error = it; errorField = if (form.name.isBlank()) "name" else "tel"; errorSeq++; return }
        // 같은 번호 — 가족 공유번호가 있어 막지 않고 확인만(웹은 '같은 분이에요'로 병합도 권하지만 게스트는 병합이 없다)
        val dup = findTelDuplicate(snapshot.customers, customerId, form.tel)
        if (dup != null) { error = null; dupWith = dup.id; return }
        commit()
    }

    DetailSheet(
        title = customer.name,
        onDismiss = onDismiss,
        headerActions = {
            if (editing) {
                ActionButton("취소", onClick = { editing = false; error = null })
                ActionButton("저장", tone = ActionTone.Primary, onClick = ::save)
            } else if (customer.deleted != true) {
                ActionButton("수정", onClick = ::startEdit)
            }
        },
    ) {
        // 웹 StyledCustomerContent — 칸마다 위아래 8, 아래 1px 옅은 회색 선
        Section {
            if (editing) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TasTextField(label = "고객명", value = form.name, onValueChange = { form = form.copy(name = it); error = null },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        error = error?.takeIf { errorField == "name" }, errorKey = errorSeq)
                    TasTextField(label = "연락처", value = form.tel, onValueChange = { form = form.copy(tel = it); error = null },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                        error = error?.takeIf { errorField == "tel" }, errorKey = errorSeq)
                    // 웹: 번호가 비어 있으면 네이버 예약 페이지로 가는 길(네이버 유입 고객은 번호가 네이버에만 있다)
                    if (normalizeTel(form.tel).isEmpty()) {
                        reservations.lastOrNull { !it.naverBookingUrl.isNullOrBlank() }?.naverBookingUrl?.let { url ->
                            val context = LocalContext.current
                            Text("네이버 예약에서 번호 확인", color = TasColors.Blue, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.heightIn(min = 24.dp).wrapContentHeight(Alignment.CenterVertically)
                                    .clickable(role = Role.Button) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } })
                        }
                    }
                    Text("적립금 ${formatPrice(customer.points ?: 0)}", color = TasColors.DarkGray, fontSize = TasSizes.SmallFont)
                }
            } else {
                InfoRow("연락처") {
                    val context = LocalContext.current
                    if (customer.tel.isNotBlank()) {
                        Text(formatTel(customer.tel), color = TasColors.Black, fontSize = TasSizes.MediumFont,
                            modifier = Modifier.heightIn(min = 24.dp).wrapContentHeight(Alignment.CenterVertically)
                                .clickable(role = Role.Button, onClickLabel = "전화 걸기") {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.tel}"))) }
                                })
                    } else {
                        Text("-", color = TasColors.Black, fontSize = TasSizes.MediumFont)
                    }
                }
                InfoRow("적립금") { Text(formatPrice(customer.points ?: 0), color = TasColors.Black, fontSize = TasSizes.MediumFont) }
                InfoRow("노쇼") {
                    val n = noshowCount(reservations)
                    // 웹 StyledNoshowCount — 있으면 경고색 굵게
                    Text("${n}회", color = if (n > 0) TasColors.Warning else TasColors.Black, fontSize = TasSizes.MediumFont,
                        fontWeight = if (n > 0) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        // 고객 메모 — 웹 CustomerMemoTagSection
        Section(spacing = 10) {
            SectionTitle("고객 메모")
            if (editing) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TasTextField(label = "메모 태그 입력", value = newTag, onValueChange = { newTag = it; error = null }, placeholder = "메모 태그 입력",
                        showLabel = false, compact = true, modifier = Modifier.weight(1f))
                    ActionButton("추가", onClick = {
                        val (next, message) = form.addTag(newTag, tagColor)
                        if (message != null) { error = message; errorField = "tag"; errorSeq++ } else if (next != form) { form = next; newTag = "" }
                    })
                }
                MemoColorPalette(tagColor, { tagColor = it }, dot = 22.dp)
            }
            val tags = if (editing) form.memoTags else customer.memoTags.orEmpty()
            if (tags.isEmpty()) {
                EmptyText()
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag -> MemoChip(tag, onRemove = if (editing) ({ form = form.removeTag(tag.text) }) else null) }
                }
            }
            error?.takeIf { errorField == "tag" }?.let { FieldError(it, scrollKey = errorSeq) }
        }

        // 적립금 이력 — 최근 1건 + 더보기(전체 창). 예약 이력은 누르면 그 예약.
        val histories = remember(customer.pointHistories) { customer.pointHistories.orEmpty().asReversed() }
        var showAllHistory by rememberSaveable(customerId) { mutableStateOf(false) }
        fun historyClick(id: Int?): (() -> Unit)? = id?.let { rid -> reservations.firstOrNull { it.id == rid } }?.let { r -> { onReservationClick(r) } }
        Section {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("적립금 이력 (${histories.size})", Modifier.weight(1f))
                if (histories.size > 1) MoreLink("더보기", "적립금 이력 전체 보기") { showAllHistory = true }
            }
            if (histories.isEmpty()) EmptyText() else PointHistoryCard(histories.first(), historyClick(histories.first().relatedReservationId))
        }
        if (showAllHistory) {
            DetailSheet(title = "적립금 이력 (${histories.size})", onDismiss = { showAllHistory = false }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 예약으로 가면 이 창은 닫는다 — 열어 두면 다시 그릴 때(회전) 이 창이 예약 상세 위로 올라왔다
                    histories.forEach { h -> PointHistoryCard(h, historyClick(h.relatedReservationId)?.let { go -> { showAllHistory = false; go() } }) }
                }
            }
        }

        // 예약 내역 — 5건씩
        Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("예약 내역 (${reservations.size})")
            if (reservations.isEmpty()) EmptyText()
            reservations.take(visibleCount).forEach { r ->
                val id = r.assigneeId?.takeIf { it != 0 }
                ReservationInfoCard(
                    reservation = r,
                    serviceColorMap = serviceColorMap,
                    assigneeName = id?.let { assigneeNames[it] } ?: "미지정",
                    assigneeColor = hexColor(id?.let { assigneeColors[it] } ?: UNASSIGNED_COLOR),
                    displayPrice = r.price ?: servicePriceTotal(r.service, snapshot.services),
                    onClick = { onReservationClick(r) },
                )
            }
            // 웹 StyledMoreButton — 폭 전체, 1px 회색 테두리, 4 둥글게
            if (reservations.size > visibleCount) {
                val shape = RoundedCornerShape(4.dp)
                Text(
                    "더보기", color = TasColors.DarkGray, fontSize = TasSizes.MediumFont,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(shape).border(1.dp, TasColors.DarkGray2, shape)
                        .clickable(role = Role.Button, onClickLabel = "예약 내역 더 보기") { visibleCount += PAGE_SIZE }.padding(8.dp),
                )
            }
        }
    }

    dupWith?.let { dupId ->
        val match = snapshot.customers.firstOrNull { it.id == dupId }
        DetailSheet(
            title = "이미 등록된 번호입니다",
            onDismiss = { dupWith = null },
            centered = true,
            footer = {
                ActionButton("취소", onClick = { dupWith = null })
                ActionButton("다른 고객으로 저장", tone = ActionTone.Primary, onClick = ::commit)
            },
        ) {
            Text("${match?.name ?: "다른"} 고객과 같은 번호입니다. 다른 분(가족 공유번호 등)이면 다른 고객으로 저장하세요.",
                color = TasColors.DarkGray, fontSize = TasSizes.MediumFont, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun Section(spacing: Int = 0, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind { drawLine(TasColors.LightGray, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content,
    )
}

/** 웹 StyledInfoList — 이름 60 + 값, 간격 4·12, 14. */
@Composable
private fun InfoRow(term: String, value: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(term, color = TasColors.DarkGray, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.Medium, modifier = Modifier.width(72.dp))
        value()
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, color = TasColors.Black, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.SemiBold, modifier = modifier.semantics { heading() })
}

@Composable
private fun EmptyText() {
    Text("등록된 데이터가 없습니다", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
}

@Composable
private fun MoreLink(text: String, label: String, onClick: () -> Unit) {
    Text(text, color = TasColors.Blue, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.heightIn(min = 24.dp).wrapContentHeight(Alignment.CenterVertically).clickable(role = Role.Button, onClickLabel = label, onClick = onClick))
}
