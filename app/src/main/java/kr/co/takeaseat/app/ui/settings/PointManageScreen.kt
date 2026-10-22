package kr.co.takeaseat.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.Customer
import kr.co.takeaseat.app.domain.POINT_HISTORY_LABELS
import kr.co.takeaseat.app.domain.PointHistoryEntry
import kr.co.takeaseat.app.domain.PointRechargeRule
import kr.co.takeaseat.app.domain.PointSettings
import kr.co.takeaseat.app.domain.adjustPoints
import kr.co.takeaseat.app.domain.customersWithPoints
import kr.co.takeaseat.app.domain.formatHistoryTimestamp
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.formatTel
import kr.co.takeaseat.app.domain.parseServiceRate
import kr.co.takeaseat.app.domain.pointCustomers
import kr.co.takeaseat.app.domain.rechargeBonusPercent
import kr.co.takeaseat.app.domain.removeRechargeRule
import kr.co.takeaseat.app.domain.searchPointCustomers
import kr.co.takeaseat.app.ui.DetailSheets
import kr.co.takeaseat.app.ui.components.ActionButton
import kr.co.takeaseat.app.ui.components.ActionTone
import kr.co.takeaseat.app.ui.components.PageHero
import kr.co.takeaseat.app.ui.components.ScreenHeader
import kr.co.takeaseat.app.ui.components.TasTextField
import kr.co.takeaseat.app.ui.rememberDetailStack
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes

private enum class PointTab(val label: String) { History("내역"), Adjust("적립"), Settings("설정") }

private val pointSettingsSaver = Saver<PointSettings, String>(
    save = { GuestStore.json.encodeToString(PointSettings.serializer(), it) },
    restore = { GuestStore.json.decodeFromString(PointSettings.serializer(), it) },
)

/**
 * 웹 `/settings/point`(PointManageSection, 모바일) — 소개 · 위에 붙는 줄(내역 · 적립 · 설정 + 전체 잔액) · 탭 내용.
 * 고객 이름 → 고객 상세, 예약에 묶인 이력 → 예약 상세(연 순서대로 쌓인다). 규칙은 `domain/PointManage.kt`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PointManageScreen(store: GuestStore, modifier: Modifier = Modifier) {
    val snapshot by store.snapshot.collectAsState()
    val details = rememberDetailStack()
    var tab by rememberSaveable { mutableStateOf(PointTab.History) }
    var search by rememberSaveable { mutableStateOf("") }
    // 고객별 금액 칸 — 번호 → 친 글자
    var amounts by rememberSaveable { mutableStateOf(mapOf<Int, String>()) }
    var editingPolicy by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable(stateSaver = pointSettingsSaver) { mutableStateOf(snapshot.storeSettings.pointSettings) }
    val saved = snapshot.storeSettings.pointSettings
    val policy = if (editingPolicy) draft else saved

    val customers = remember(snapshot.customers) { pointCustomers(snapshot.customers) }
    val withPoints = remember(snapshot.customers) { customersWithPoints(snapshot.customers) }
    val filtered = remember(customers, search) { searchPointCustomers(customers, search) }
    val total = customers.sumOf { it.points ?: 0 }

    Column(modifier.fillMaxSize()) {
        ScreenHeader("적립금 관리")
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item { PageHero("POINT", "고객 적립금·선불금(충전) 내역 조회, 수동 적립/차감, 적립·충전 정책을 설정합니다.", PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 18.dp)) }
            stickyHeader {
                // 웹 StyledStickyHeader — 위에 붙고 아래 1px. 탭 · 전체 잔액
                Column(Modifier.fillMaxWidth().background(TasColors.White)) {
                    // 웹: 넓으면 한 줄(탭 · 잔액 오른쪽 끝), 640 이하면 잔액이 다음 줄 오른쪽 끝으로
                    BoxWithConstraints(Modifier.fillMaxWidth().padding(10.dp)) {
                        val tabs: @Composable () -> Unit = {
                            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PointTab.entries.forEach { t -> TabChip(t.label, tab == t) { tab = t } }
                            }
                        }
                        val totalBadge: @Composable () -> Unit = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("전체 잔액", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont)
                                Text(formatPrice(total), color = TasColors.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                        if (maxWidth > 640.dp) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { tabs(); totalBadge() }
                        } else {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.fillMaxWidth()) { tabs() }
                                totalBadge()
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(TasColors.LightGray))
                }
            }
            when (tab) {
                PointTab.History -> item {
                    HistoryTab(withPoints, onCustomer = { details.openCustomer(it) }) { entry ->
                        entry.relatedReservationId?.takeIf { id -> snapshot.reservations.any { it.id == id } }?.let { details.openReservation(it) }
                    }
                }
                PointTab.Adjust -> {
                    item {
                        Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("고객 적립금 조정", color = TasColors.DarkGray, fontSize = TasSizes.LargeFont, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                            TasTextField(label = "고객 검색", value = search, onValueChange = { search = it }, placeholder = "고객명 또는 연락처 검색",
                                showLabel = false, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search))
                        }
                    }
                    if (filtered.isEmpty()) item { EmptyText(40) }
                    items(filtered, key = { it.id }) { c ->
                        AdjustCard(
                            customer = c,
                            amount = amounts[c.id].orEmpty(),
                            onAmount = { v -> amounts = amounts + (c.id to v.filter { it.isDigit() }.take(9)) },
                            onCustomer = { details.openCustomer(c.id) },
                        ) { add ->
                            val amount = amounts[c.id]?.toIntOrNull() ?: 0
                            store.update { adjustPoints(it, c.id, amount, add) }
                            amounts = amounts + (c.id to "")
                        }
                    }
                }
                PointTab.Settings -> item {
                    Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PolicyCard(
                            policy = policy,
                            editing = editingPolicy,
                            dirty = draft != saved,
                            onEdit = { draft = saved; editingPolicy = true },
                            onCancel = { draft = saved; editingPolicy = false },
                            onSave = { store.update { it.copy(storeSettings = it.storeSettings.copy(pointSettings = draft)) }; editingPolicy = false },
                            onChange = { draft = it },
                        )
                        if (policy.enableRecharge) {
                            RechargeCard(
                                policy = policy,
                                editing = editingPolicy,
                                // 웹: 이 카드엔 [수정]이 없어 '기준 추가'를 누르면 바로 수정 모드로
                                onAdd = {
                                    val base = if (editingPolicy) draft else saved
                                    draft = base.copy(rechargeRules = base.rechargeRules + PointRechargeRule(0, 0))
                                    editingPolicy = true
                                },
                                onChange = { draft = it },
                            )
                        }
                    }
                }
            }
            item { Box(Modifier.height(24.dp)) }
        }
    }
    DetailSheets(store, details)
}

/** 웹 StyledTabButton — 30 높이, 최소 72, 고른 것 옅은 파랑 + 파랑 테두리 · 굵게. */
@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .heightIn(min = 30.dp)
            .widthIn(min = 72.dp)
            .clip(shape)
            .background(if (selected) TasColors.InfoBg else TasColors.White)
            .border(1.dp, if (selected) TasColors.Blue else TasColors.LightGray, shape)
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) TasColors.Blue else TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun EmptyText(padding: Int) {
    Text("등록된 데이터가 없습니다", color = TasColors.DarkGray2, fontSize = TasSizes.MediumFont, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(padding.dp))
}

/** 웹 cardSurfaceStyle — 1px 옅은 회색, 8 둥글게, 흰 바탕, 그림자 sm. */
@Composable
private fun SurfaceCard(padding: PaddingValues, modifier: Modifier = Modifier, gap: Int = 10, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier
            .fillMaxWidth()
            .dropShadow(shape, Shadow(radius = TasSizes.ShadowSmBlur, color = TasColors.ShadowSm, offset = DpOffset(0.dp, TasSizes.ShadowSmOffsetY)))
            .clip(shape)
            .background(TasColors.White)
            .border(1.dp, TasColors.LightGray, shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(gap.dp),
        content = content,
    )
}

/** 고객 이름(14 굵게, 누르면 고객 상세) + 연락처(누르면 전화). 웹 모바일은 위아래로. */
@Composable
private fun NameAndTel(customer: Customer, onCustomer: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(customer.name, color = TasColors.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.heightIn(min = 24.dp).wrapContentHeight(Alignment.CenterVertically).clickable(role = Role.Button, onClickLabel = "고객 정보 보기", onClick = onCustomer))
        if (customer.tel.isNotBlank()) {
            Text(formatTel(customer.tel), color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont,
                modifier = Modifier.heightIn(min = 24.dp).wrapContentHeight(Alignment.CenterVertically).clickable(role = Role.Button, onClickLabel = "전화 걸기") {
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.tel}"))) }
                })
        }
    }
}

/** 웹 PointHistoryTab — 옅은 회색 상자 안에 제목·설명·n명, 고객 카드마다 이력(최근부터). */
@Composable
private fun HistoryTab(customers: List<Customer>, onCustomer: (Int) -> Unit, onEntry: (PointHistoryEntry) -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier
            .padding(start = 10.dp, end = 10.dp, top = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Color(248, 250, 252).copy(alpha = 0.88f))
            .border(1.dp, TasColors.LightGray, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("현재 적립금 보유 고객 전체 내역", color = TasColors.DarkGray, fontSize = TasSizes.LargeFont, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                Text("현재 잔액이 남아있는 고객만 모아서 전체 적립/차감/충전 이력을 표시합니다.", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont, lineHeight = TasSizes.SmallFont * 1.45f)
            }
            Text("${customers.size}명", color = TasColors.Blue, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
        }
        if (customers.isEmpty()) EmptyText(12)
        customers.forEach { c ->
            SurfaceCard(PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NameAndTel(c, onCustomer = { onCustomer(c.id) }, modifier = Modifier.weight(1f))
                    Text(formatPrice(c.points ?: 0), color = TasColors.Blue, fontSize = TasSizes.LargeFont, fontWeight = FontWeight.Bold)
                }
                val histories = c.pointHistories.orEmpty().asReversed()
                if (histories.isEmpty()) EmptyText(12)
                histories.forEach { h ->
                    val shapeItem = RoundedCornerShape(10.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(shapeItem)
                            .background(TasColors.Gray2)
                            .then(if (h.relatedReservationId != null) Modifier.clickable(role = Role.Button, onClickLabel = "예약 보기") { onEntry(h) } else Modifier)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(POINT_HISTORY_LABELS[h.type] ?: h.type, color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${if (h.delta > 0) "+" else ""}${formatPrice(h.delta)}", color = TasColors.Blue, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.Bold)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(h.description, "잔액 ${formatPrice(h.balance)}", formatHistoryTimestamp(h.createdAt)).forEach {
                                Text(it, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 웹 PointAdjustTab 고객 카드(모바일 한 줄씩) — 이름·연락처 / 잔액(16 굵게) / 금액 칸 · 적립 · 차감(72씩). */
@Composable
private fun AdjustCard(customer: Customer, amount: String, onAmount: (String) -> Unit, onCustomer: () -> Unit, onApply: (add: Boolean) -> Unit) {
    SurfaceCard(PaddingValues(horizontal = 14.dp, vertical = 12.dp), modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp), gap = 12) {
        NameAndTel(customer, onCustomer)
        Text(formatPrice(customer.points ?: 0), color = TasColors.Black, fontSize = TasSizes.LargeFont, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TasTextField(label = "${customer.name} 적립금 금액", value = amount, onValueChange = onAmount, placeholder = "금액 입력", showLabel = false,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            ActionButton("적립", onClick = { onApply(true) }, modifier = Modifier.width(72.dp))
            ActionButton("차감", onClick = { onApply(false) }, tone = ActionTone.DangerOutline, modifier = Modifier.width(72.dp))
        }
    }
}

/** 웹 '적립 방식' 카드 — 수정 → 취소·저장. 서비스 금액 % 적립(켜면 % 칸) · 충전하는 방식. */
@Composable
private fun PolicyCard(
    policy: PointSettings,
    editing: Boolean,
    dirty: Boolean,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onChange: (PointSettings) -> Unit,
) {
    SurfaceCard(PaddingValues(14.dp), gap = 12) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("적립 방식", color = TasColors.DarkGray, fontSize = TasSizes.LargeFont, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                Text("적립금이 쌓이는 기본 방식을 설정합니다.", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont)
            }
            if (!editing) SmallButton("수정", onEdit)
            else Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionButton("취소", onClick = onCancel)
                ActionButton("저장", onClick = onSave, tone = ActionTone.Primary, enabled = dirty)
            }
        }
        PolicyOption(
            checked = policy.enableServiceRate,
            enabled = editing,
            title = "서비스 금액 일정 퍼센트 적립",
            desc = "결제완료 시 적립금 결제를 제외한 금액 기준으로 자동 적립됩니다.",
            onCheck = { onChange(policy.copy(enableServiceRate = it)) },
        ) {
            if (policy.enableServiceRate) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("적립 퍼센트", color = TasColors.DarkGray, fontSize = TasSizes.SmallFont)
                    TasTextField(label = "적립 퍼센트", value = policy.serviceRate.toInt().toString(), onValueChange = { onChange(policy.copy(serviceRate = parseServiceRate(it))) },
                        showLabel = false, compact = true, enabled = editing, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(80.dp))
                    Text("%", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont)
                }
            }
        }
        PolicyOption(
            checked = policy.enableRecharge,
            enabled = editing,
            title = "충전하는 방식",
            desc = "고객별 적립/차감으로 직접 충전하고 관리합니다.",
            onCheck = { onChange(policy.copy(enableRecharge = it)) },
        ) {}
    }
}

/** 웹 StyledPolicyBlock — 켜면 옅은 파랑(테두리 35%·바탕 6%), 끄면 회색 바탕에 흐리게(60%). 체크 + 제목(13 굵게) · 설명(12 회색) + 아래 내용. */
@Composable
private fun PolicyOption(checked: Boolean, enabled: Boolean, title: String, desc: String, onCheck: (Boolean) -> Unit, body: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (checked) 1f else 0.6f }
            .clip(shape)
            .background(if (checked) PolicyActiveBg else TasColors.Gray2)
            .border(1.dp, if (checked) PolicyActiveBorder else TasColors.LightGray, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheck), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled, colors = CheckboxDefaults.colors(checkedColor = TasColors.Brand), modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = TasColors.Black, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.Bold)
                Text(desc, color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont, lineHeight = TasSizes.SmallFont * 1.45f)
            }
        }
        body()
    }
}

/** 웹 '충전 기준' 카드 — 기준금액 + 추가금 · 삭제 · 추가 적립 n%, 오른쪽 위 '기준 추가'. */
@Composable
private fun RechargeCard(policy: PointSettings, editing: Boolean, onAdd: () -> Unit, onChange: (PointSettings) -> Unit) {
    SurfaceCard(PaddingValues(14.dp), gap = 12) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("충전 기준", color = TasColors.DarkGray, fontSize = TasSizes.LargeFont, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).semantics { heading() })
            SmallButton("기준 추가", onAdd)
        }
        policy.rechargeRules.forEachIndexed { index, rule ->
            fun set(next: PointRechargeRule) = onChange(policy.copy(rechargeRules = policy.rechargeRules.mapIndexed { i, r -> if (i == index) next else r }))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TasTextField(label = "기준금액", value = rule.baseAmount.takeIf { it > 0 }?.toString().orEmpty(), placeholder = "기준금액", compact = true, enabled = editing,
                        onValueChange = { v -> set(rule.copy(baseAmount = v.filter { it.isDigit() }.take(9).toIntOrNull() ?: 0)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    Text("+", color = TasColors.DarkGray2, fontSize = TasSizes.MediumFont, modifier = Modifier.padding(bottom = 6.dp))
                    TasTextField(label = "추가금", value = rule.bonusAmount.takeIf { it > 0 }?.toString().orEmpty(), placeholder = "추가금", compact = true, enabled = editing,
                        onValueChange = { v -> set(rule.copy(bonusAmount = v.filter { it.isDigit() }.take(9).toIntOrNull() ?: 0)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                // 웹 모바일: 삭제는 한 줄을 다 쓴다
                ActionButton("삭제", onClick = { onChange(policy.copy(rechargeRules = removeRechargeRule(policy.rechargeRules, index))) },
                    tone = ActionTone.DangerOutline, enabled = editing, modifier = Modifier.fillMaxWidth())
                Text("추가 적립 ${rechargeBonusPercent(rule)}", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont)
            }
        }
    }
}

private val PolicyActiveBg = Color(45, 127, 249).copy(alpha = 0.06f)
private val PolicyActiveBorder = Color(45, 127, 249).copy(alpha = 0.35f)
