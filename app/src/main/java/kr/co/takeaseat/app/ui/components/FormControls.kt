package kr.co.takeaseat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import android.app.Dialog
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

// 웹 `StyledForm` 의 label > strong + input/select(formControlStyle, 36 높이) · `FieldError` inline · `StyledActionButton` 이식.

/**
 * 웹 formControlStyle — 흰 바탕, 1px #e3e3e3, 6 둥글게(36 높이), 아주 옅은 그림자. 초점이면 테두리 보라 + 바깥 3px 옅은 보라 링.
 * [compact] = 결제 줄 칸(웹 paymentEntryFieldStyles — 30 높이, 4 둥글게).
 */
private fun Modifier.formControl(focused: Boolean, compact: Boolean = false): Modifier {
    val shape = if (compact) TasShapes.RadiusSm else TasShapes.RadiusMd
    val radius = if (compact) 4.dp else 6.dp
    return this
        .drawBehind {
            if (focused) {
                val ring = 3.dp.toPx()
                drawRoundRect(
                    TasColors.FocusRing,
                    topLeft = Offset(-ring, -ring),
                    size = Size(size.width + ring * 2, size.height + ring * 2),
                    cornerRadius = CornerRadius(radius.toPx() + ring),
                )
            }
        }
        .dropShadow(shape, Shadow(radius = 2.dp, color = TasColors.Scrim.copy(alpha = 0.04f), offset = DpOffset(0.dp, 1.dp)))
        .clip(shape)
        .background(TasColors.White)
        .border(1.dp, if (focused) TasColors.Brand else TasColors.LightGray, shape)
        // 최소 높이 — 큰 글자(시스템 글꼴 크기)에선 글자에 맞춰 늘어난다(고정이면 글자가 잘렸다)
        .heightIn(min = if (compact) 30.dp else 36.dp)
}

@Composable
private fun FieldLabel(text: String) {
    // 낭독은 입력 칸이 이름으로 읽는다 — 라벨 글자는 따로 읽지 않는다.
    Text(text, color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold, modifier = Modifier.clearAndSetSemantics {})
}

/** 웹 FieldError inline — 12, 빨강. 나타나면 낭독기가 읽고, 시트 밖(스크롤 아래)에 있으면 보이는 곳까지 스크롤한다. */
@Composable
fun FieldError(message: String, modifier: Modifier = Modifier, scrollKey: Any? = null) {
    val requester = remember { BringIntoViewRequester() }
    // scrollKey — 같은 오류로 다시 저장을 눌러도(문구가 같아도) 다시 보여 준다.
    LaunchedEffect(message, scrollKey) { requester.bringIntoView() }
    Text(
        message,
        color = TasColors.Danger,
        fontSize = TasSizes.SmallFont,
        lineHeight = (12 * 1.4).sp,
        modifier = modifier.bringIntoViewRequester(requester).semantics { liveRegion = LiveRegionMode.Polite },
    )
}

private val LineBreaks = Regex("[\\r\\n]")

/** 한 줄 칸의 새 글자에서 줄바꿈을 뺀다. 줄바꿈 한 글자만 들어왔으면(엔터) 두 번째 값이 true. */
internal fun stripLineBreaks(old: String, new: String): Pair<String, Boolean> {
    if (!LineBreaks.containsMatchIn(new)) return new to false
    return new.replace(LineBreaks, "") to (new.length == old.length + 1)
}

private val NoDefaultKeyboardAction = object : KeyboardActionScope {
    override fun defaultKeyboardAction(imeAction: ImeAction) = Unit
}

/** 라벨 + 한 줄 입력. [suffix] 는 가격 칸의 '원'(입력 오른쪽 정렬). */
@Composable
fun TasTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    errorKey: Any? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    suffix: String? = null,
    onFocusChange: (Boolean) -> Unit = {},
    showLabel: Boolean = true,
    compact: Boolean = false,
    enabled: Boolean = true,
) {
    val fontSize = if (compact) TasSizes.SmallFont else TasSizes.MediumFont
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicTextField(
            value = value,
            onValueChange = { v ->
                // 한 줄 칸인데 하드웨어 키보드 엔터·붙여넣기는 줄바꿈을 넣는다(IME 가 글자로 보냄) — 빼고, 엔터 한 번이면 칸의 키보드 동작(완료·다음·검색)으로
                val (text, enterPressed) = stripLineBreaks(value, v)
                if (text != v) {
                    onValueChange(text)
                    if (enterPressed) {
                        val action = when (keyboardOptions.imeAction) {
                            ImeAction.Next -> keyboardActions.onNext
                            ImeAction.Search -> keyboardActions.onSearch
                            ImeAction.Go -> keyboardActions.onGo
                            ImeAction.Send -> keyboardActions.onSend
                            else -> keyboardActions.onDone
                        }
                        if (action != null) action(NoDefaultKeyboardAction)
                        else if (keyboardOptions.imeAction == ImeAction.Next) focusManager.moveFocus(FocusDirection.Next)
                    }
                } else onValueChange(v)
            },
            singleLine = true,
            enabled = enabled,
            textStyle = TextStyle(
                // 웹 formControlStyle :disabled — 회색 바탕, 흐린 글자
                color = if (!enabled) TasColors.DarkGray2 else if (compact) TasColors.DarkGray else TasColors.Black,
                fontSize = fontSize,
                textAlign = if (suffix != null) TextAlign.End else TextAlign.Start,
            ),
            cursorBrush = SolidColor(TasColors.Brand),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChange(it.isFocused) },
            decorationBox = { inner ->
                // 라벨·'원'까지 장식 상자 안에 둔다 — 입력 칸 낭독 노드 안에 들어가야 칸이 제 이름으로 읽힌다(바깥에 두면 따로 읽혔다).
                // 웹 <label> 처럼 라벨을 눌러도 입력 칸에 들어간다.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (showLabel) {
                        Text(label, color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .formControl(focused, compact)
                                .then(if (enabled) Modifier else Modifier.background(TasColors.Gray2))
                                .semantics {
                                    // 라벨을 숨기면(결제 줄) 칸 이름은 여기서 준다 — 웹 aria-label
                                    if (!showLabel) contentDescription = label
                                    if (error != null) error(error)
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = if (suffix != null) Alignment.CenterEnd else Alignment.CenterStart,
                        ) {
                            if (value.isEmpty() && placeholder.isNotEmpty()) {
                                Text(placeholder, color = TasColors.Placeholder, fontSize = fontSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            inner()
                        }
                        if (suffix != null) Text(suffix, color = TasColors.DarkGray, fontSize = TasSizes.MediumFont)
                    }
                }
            },
        )
        if (error != null) FieldError(error, scrollKey = errorKey)
    }
}

/**
 * 라벨 + 고르는 칸(웹 select · date · time 이 같은 모양). 누르면 [onClick] — 날짜·시간은 시스템 선택 창을 띄운다.
 * [valueDescription] 은 낭독용 값(예: "2026년 10월 19일"). 없으면 [valueText].
 */
@Composable
fun TasSelect(
    label: String,
    valueText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    valueDescription: String = valueText,
    error: String? = null,
    errorKey: Any? = null,
    centered: Boolean = false,
    showLabel: Boolean = true,
    compact: Boolean = false,
    enabled: Boolean = true,
    menu: @Composable () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showLabel) FieldLabel(label)
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .formControl(focused, compact)
                    // 웹 formControlStyle :disabled — 옅은 회색 바탕 · 회색 글자(TasTextField 와 같게)
                    .then(if (enabled) Modifier else Modifier.background(TasColors.Gray2))
                    .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.DropdownList, onClick = onClick)
                    .semantics {
                        contentDescription = label
                        stateDescription = valueDescription
                        if (error != null) error(error)
                    }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    valueText,
                    color = if (!enabled) TasColors.DarkGray2 else if (compact) TasColors.DarkGray else TasColors.Black,
                    fontSize = if (compact) TasSizes.SmallFont else TasSizes.MediumFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.weight(1f),
                )
                if (!centered) Icon(ChevronDownIcon, contentDescription = null, tint = TasColors.DarkGray2, modifier = Modifier.size(12.dp))
            }
            menu()
        }
        if (error != null) FieldError(error, scrollKey = errorKey)
    }
}

/** 웹 `<select>` — 선택지 중 하나. */
@Composable
fun TasSelect(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    errorKey: Any? = null,
    showLabel: Boolean = true,
    compact: Boolean = false,
    placeholder: String = "",
) {
    var open by rememberSaveable { mutableStateOf(false) }
    TasSelect(
        label = label,
        valueText = options.getOrElse(selectedIndex) { placeholder },
        onClick = { open = true },
        modifier = modifier,
        error = error,
        errorKey = errorKey,
        showLabel = showLabel,
        compact = compact,
    ) {
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = TasSizes.MediumFont, color = TasColors.Black, fontWeight = if (index == selectedIndex) FontWeight.SemiBold else null) },
                    onClick = {
                        open = false
                        onSelect(index)
                    },
                )
            }
        }
    }
}

/** 버튼 글자 한 줄 높이 — 웹 12 × 1.2 ≈ 14. 짝수 px(2배 화면 28)로 둬야 버튼 안에서 반 픽셀 밀리지 않는다. */
internal val ButtonLine = 14.sp

/** 웹 StyledActionButton 의 $primary / $danger / $dangerOutline. */
/** [Blue] = 웹 --blue-color 채운 버튼(적립금 '충전'). [DangerSoft] = 웹 설정 StyledDeleteBtn(옅은 빨강 바탕 · 빨강 테두리·글자). */
enum class ActionTone { Default, Primary, Danger, DangerOutline, DangerSoft, Blue }

/** 웹 StyledActionButton(모바일) — 30 높이, 7 둥글게, 좌우 10, 12. 채운 것(주·위험)은 흰 글자 600, 나머지 500. */
@Composable
fun ActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tone: ActionTone = ActionTone.Default, enabled: Boolean = true) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(7.dp)
    Box(
        modifier
            .heightIn(min = 30.dp)
            .dropShadow(shape, Shadow(radius = TasSizes.ShadowSmBlur, color = TasColors.ShadowSm, offset = DpOffset(0.dp, TasSizes.ShadowSmOffsetY)))
            .clip(shape)
            .background(
                when (tone) {
                    ActionTone.Primary -> TasColors.Brand
                    ActionTone.Danger -> TasColors.Danger
                    ActionTone.Blue -> TasColors.Blue
                    ActionTone.DangerSoft -> TasColors.DangerBg
                    else -> TasColors.White
                },
            )
            .then(
                when (tone) {
                    ActionTone.Default -> Modifier.border(1.dp, TasColors.Border, shape)
                    ActionTone.DangerOutline, ActionTone.DangerSoft -> Modifier.border(1.dp, TasColors.DangerBorder, shape)
                    else -> Modifier
                },
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = when (tone) {
                ActionTone.Default -> TasColors.DarkGray
                ActionTone.DangerOutline, ActionTone.DangerSoft -> TasColors.Danger
                else -> TasColors.White
            },
            fontSize = TasSizes.SmallFont,
            fontWeight = if (tone == ActionTone.Default) FontWeight.Medium else FontWeight.SemiBold,
            maxLines = 1,
            // 글자 상자 가운데가 아니라 글자 몸통 가운데로 — 한글이 1dp 아래로 처졌다(배지와 같은 방식, 웹 line-height normal 1.2)
            modifier = Modifier.lineBox(ButtonLine, TasSizes.SmallFont),
        )
    }
}

/**
 * 시스템 대화상자(날짜·시간 선택)를 컴포지션에 묶는다 — 화면이 사라지면 닫고(창 누수 방지), 회전하면 다시 띄운다.
 * 보일 때만 부른다: `if (picking) SystemDialogEffect(onDismissed = { picking = false }) { DatePickerDialog(it, ...) }`.
 */
@Composable
fun SystemDialogEffect(onDismissed: () -> Unit, create: (Context) -> Dialog) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val dialog = create(context)
        dialog.setOnDismissListener { onDismissed() }
        dialog.show()
        onDispose {
            dialog.setOnDismissListener(null)
            dialog.dismiss()
        }
    }
}
