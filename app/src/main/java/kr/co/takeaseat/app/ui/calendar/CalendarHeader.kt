package kr.co.takeaseat.app.ui.calendar

import kr.co.takeaseat.app.ui.nav.LocalAsideToggle
import kr.co.takeaseat.app.ui.nav.AsideToggleButton
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kr.co.takeaseat.app.domain.Assignee
import kr.co.takeaseat.app.domain.BellIndicator
import kr.co.takeaseat.app.domain.UNASSIGNED_COLOR
import kr.co.takeaseat.app.domain.UNASSIGNED_FILTER
import kr.co.takeaseat.app.domain.assigneeColor
import kr.co.takeaseat.app.domain.splitAssigneesByStatus
import kr.co.takeaseat.app.ui.components.TasSelect
import kr.co.takeaseat.app.ui.components.hexColor
import kr.co.takeaseat.app.domain.CalendarView
import kr.co.takeaseat.app.domain.calendarHeading
import kr.co.takeaseat.app.domain.moveDate
import kr.co.takeaseat.app.ui.components.AddPill
import kr.co.takeaseat.app.ui.components.ButtonLine
import kr.co.takeaseat.app.ui.components.ChevronDownIcon
import kr.co.takeaseat.app.ui.components.SystemDialogEffect
import kr.co.takeaseat.app.ui.components.lineBox
import kr.co.takeaseat.app.ui.components.svgLineIcon
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 헤더 — 1줄(`CalendarDirection` + `CalendarHeading` + 모바일 '+ 예약') · 2줄(StyledToolRow: 담당자 고르기 …… 알림 종).
 * 넓은 화면(641 이상)은 웹처럼 한 줄에 이어 붙는다. 날짜를 누르면 시스템 날짜 선택이 뜬다(웹도 네이티브 `<input type="date">`).
 */
@Composable
fun CalendarHeader(
    view: CalendarView,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onCreate: () -> Unit,
    bell: BellIndicator,
    onBell: () -> Unit,
    assignees: List<Assignee>,
    assigneeFilter: Int?,
    onAssigneeFilter: (Int?) -> Unit,
    assigneeLabel: String,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    if (picking) {
        SystemDialogEffect(onDismissed = { picking = false }) {
            DatePickerDialog(it, { _, y, m, d -> onDateChange(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth)
        }
    }
    val aside = LocalAsideToggle.current
    // 펼침은 여기서 기억 — 한 줄↔두 줄로 바뀌어 고르는 칸 자리가 옮겨가도(폴드 접고 펴기) 펼친 채로
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    val filter = @Composable {
        AssigneeFilter(assignees, assigneeFilter, assigneeLabel, narrow = aside == null, open = filterOpen, onOpenChange = { filterOpen = it }, onSelect = onAssigneeFilter)
    }
    // 웹 헤더의 알림 종(곧 시작하는 예약)
    val notificationBell = @Composable { NotificationBell(count = bell.count, dot = bell.dot, onClick = onBell) }
    // 웹 641px 이상은 한 줄(날짜 칸이 좁아진다). 안드로이드는 폴드에 왼쪽 메뉴를 편 폭(약 470dp)에서 날짜가 잘려,
    // 헤더가 [ONE_ROW_MIN] 보다 좁으면 담당자·종 줄을 모바일처럼 다음 줄로 내린다(안드로이드 보완)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stacked = aside == null || maxWidth < ONE_ROW_MIN
        // 웹: header padding 4 10 2 + 1줄 padding 3 0 5 · 2줄(모바일) padding 4 0
        Column(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 7.dp, bottom = if (stacked) 2.dp else 7.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = if (stacked) 5.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                aside?.let { AsideToggleButton(it.open, it.toggle) }
                PillButton(onClick = { onDateChange(LocalDate.now()) }, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Text("오늘", color = TasColors.Black, fontSize = TasSizes.SmallFont, maxLines = 1, modifier = Modifier.lineBox(ButtonLine, TasSizes.SmallFont))
                }
                PillButton(onClick = { onDateChange(moveDate(date, view, forward = false)) }, description = "이전${view.a11y}") {
                    Icon(arrowLeft, contentDescription = null, tint = TasColors.Icon, modifier = Modifier.size(18.dp))
                }
                PillButton(onClick = { onDateChange(moveDate(date, view, forward = true)) }, description = "다음${view.a11y}") {
                    Icon(arrowRight, contentDescription = null, tint = TasColors.Icon, modifier = Modifier.size(18.dp))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Row(
                        Modifier
                            .height(32.dp)
                            .clip(TasShapes.Chip)
                            .background(TasColors.Gray2)
                            // 웹 aria-label "날짜 선택" + 값. 보이는 "2026 / 10 / 17" 은 읽지 않는다(아래 clearAndSetSemantics).
                            .semantics { contentDescription = "날짜 선택, ${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일" }
                            .clickable(role = Role.Button) { picking = true }
                            .padding(start = 10.dp, end = 6.dp), // 웹 모바일 padding 0 6 0 10
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            calendarHeading(date, view),
                            color = TasColors.Black,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            // 해가 걸친 주("2025 / 12 - 2026 / 1")는 16sp 로 안 들어간다 — 잘리지 않게 그때만 줄인다(웹은 두 줄로 접힌다).
                            autoSize = TextAutoSize.StepBased(minFontSize = TasSizes.SmallFont, maxFontSize = TasSizes.LargeFont),
                            modifier = Modifier.weight(1f, fill = false).clearAndSetSemantics {},
                        )
                        Icon(ChevronDownIcon, contentDescription = null, tint = TasColors.DarkGray2, modifier = Modifier.size(16.dp))
                    }
                }
                if (!stacked) {
                    // 넓은 화면 — 웹 StyledToolRow 가 같은 줄에 붙는다
                    filter()
                    notificationBell()
                }
                // 넓은 화면은 왼쪽 메뉴의 '예약추가'를 쓴다(웹 StyledMobileAddPill 은 641px 이상 숨김)
                if (aside == null) AddPill("예약", description = "예약 추가", onClick = onCreate)
            }
            if (stacked) {
                // 웹 StyledToolRow(모바일) — 담당자 고르기가 왼쪽(margin-right auto), 종은 오른쪽 끝
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    filter()
                    Spacer(Modifier.weight(1f))
                    notificationBell()
                }
            }
        }
    }
}

/** 한 줄에 '메뉴 · 오늘 · ‹ › · 날짜(최소 약 150) · 담당자(128~200) · 종'이 들어가는 헤더 폭. 모자라면 두 줄. */
private val ONE_ROW_MIN = 600.dp

/**
 * 웹 StyledAssigneeFilter — 알약 select. 전체보기 · 미지정(회색 점) · 재직(색 점) · '휴직자' · '퇴직자' 묶음.
 * 고른 줄은 연보라 바탕 + 굵게(웹 option:checked). [selected] 는 지운 담당자를 걸러 낸 값(effectiveCalendarAssignee).
 */
@Composable
private fun AssigneeFilter(
    assignees: List<Assignee>,
    selected: Int?,
    label: String,
    narrow: Boolean,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onSelect: (Int?) -> Unit,
) {
    val (active, onLeave, resigned) = remember(assignees) { splitAssigneesByStatus(assignees) }
    val valueText = when (selected) {
        null -> "전체보기"
        UNASSIGNED_FILTER -> "미지정"
        else -> assignees.firstOrNull { it.id == selected }?.name.orEmpty()
    }
    TasSelect(
        label = "달력 $label 필터",
        valueText = valueText,
        onClick = { onOpenChange(true) },
        // 웹 min-width 128(모바일 96), 넓어도 글자만큼만 — 긴 이름·큰 글자에 날짜 칸이 밀리지 않게 상한(넘치면 말줄임)
        modifier = Modifier.widthIn(min = if (narrow) 96.dp else 128.dp, max = 200.dp).width(IntrinsicSize.Max),
        showLabel = false,
        pill = true,
    ) {
        DropdownMenu(expanded = open, onDismissRequest = { onOpenChange(false) }) {
            @Composable
            fun option(text: String, value: Int?, color: String?) {
                val checked = value == selected
                DropdownMenuItem(
                    // 웹 option::before — 8 점, 글자와 6 간격(머티리얼 leadingIcon 은 간격이 넓어 글자 줄 안에 둔다)
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (color != null) Box(Modifier.size(8.dp).clip(CircleShape).background(hexColor(color)))
                            Text(text, fontSize = TasSizes.SmallFont, color = TasColors.Black, fontWeight = if (checked) FontWeight.SemiBold else null)
                        }
                    },
                    onClick = {
                        onOpenChange(false)
                        onSelect(value)
                    },
                    modifier = Modifier
                        .then(if (checked) Modifier.background(TasColors.BrandBg) else Modifier)
                        .semantics { this.selected = checked },
                )
            }
            @Composable
            fun group(text: String) {
                Text(text, color = TasColors.DarkGray2, fontSize = TasSizes.TinyFont, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 2.dp).semantics { heading() })
            }
            option("전체보기", null, null)
            option("미지정", UNASSIGNED_FILTER, UNASSIGNED_COLOR)
            active.forEach { option(it.name, it.id, assigneeColor(it)) }
            if (onLeave.isNotEmpty()) {
                group("휴직자")
                onLeave.forEach { option(it.name, it.id, assigneeColor(it)) }
            }
            if (resigned.isNotEmpty()) {
                group("퇴직자")
                resigned.forEach { option(it.name, it.id, assigneeColor(it)) }
            }
        }
    }
}

/**
 * 웹 `MobileViewTabs` — 회색 캡슐 트랙 안의 보기 탭. 고른 탭은 흰 캡슐 + 브랜드색 + 그림자 sm.
 * 탭 높이 24dp(웹 padding 6 — WCAG 2.5.8 최소).
 */
@Composable
fun CalendarViewTabs(selected: CalendarView, onSelect: (CalendarView) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 4.dp)
            .clip(TasShapes.Chip)
            .background(TasColors.Gray2)
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CalendarView.entries.forEach { view ->
            val active = view == selected
            Box(
                Modifier
                    .weight(1f)
                    .then(
                        if (active) Modifier.dropShadow(TasShapes.Chip, Shadow(radius = TasSizes.ShadowSmBlur, color = TasColors.ShadowSm, offset = DpOffset(0.dp, TasSizes.ShadowSmOffsetY)))
                        else Modifier,
                    )
                    .clip(TasShapes.Chip)
                    .background(if (active) TasColors.Surface else Color.Transparent)
                    .selectable(selected = active, role = Role.Tab) { onSelect(view) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    view.label,
                    color = if (active) TasColors.Brand else TasColors.DarkGray,
                    fontSize = TasSizes.TinyFont,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.lineBox(12.sp, TasSizes.TinyFont),
                )
            }
        }
    }
}

/**
 * 웹 `ButtonSquare`(오늘) · `StyledDirectionButton`(‹ ›) — 흰 알약 32, 1px 테두리, 그림자 sm, 누르면 회색.
 * 32dp 는 웹과 같은 크기다(WCAG 2.2 AA 최소 24 충족, 안드로이드 권장 48 보다는 작다).
 */
@Composable
private fun PillButton(
    onClick: () -> Unit,
    description: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        Modifier
            .height(32.dp)
            .widthIn(min = 32.dp)
            .dropShadow(TasShapes.Chip, Shadow(radius = TasSizes.ShadowSmBlur, color = TasColors.ShadowSm, offset = DpOffset(0.dp, TasSizes.ShadowSmOffsetY)))
            .clip(TasShapes.Chip)
            .background(if (pressed) TasColors.Gray2 else TasColors.Surface)
            .border(1.dp, TasColors.Border, TasShapes.Chip)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// 웹 DirectionIcon(viewBox 32) 경로 그대로.
internal val arrowLeft: ImageVector = svgLineIcon(32f, listOf("M18.5 10.5L13 16L18.5 21.5"), strokeWidth = 2.2f)
internal val arrowRight: ImageVector = svgLineIcon(32f, listOf("M13.5 10.5L19 16L13.5 21.5"), strokeWidth = 2.2f)
