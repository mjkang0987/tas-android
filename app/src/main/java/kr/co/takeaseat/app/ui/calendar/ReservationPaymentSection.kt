package kr.co.takeaseat.app.ui.calendar

import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import kr.co.takeaseat.app.ui.components.lineBox
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kr.co.takeaseat.app.domain.PAYMENT_METHOD_OPTIONS
import kr.co.takeaseat.app.domain.PaymentDraft
import kr.co.takeaseat.app.domain.PaymentForm
import kr.co.takeaseat.app.domain.PointAward
import kr.co.takeaseat.app.domain.PointSettings
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.removeEntry
import kr.co.takeaseat.app.domain.withEntries
import kr.co.takeaseat.app.ui.components.FieldError
import kr.co.takeaseat.app.ui.components.TasSelect
import kr.co.takeaseat.app.ui.components.TasTextField
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 `ReservationDetailPaymentLayer` — 결제 종류·금액 줄(모바일: 종류 한 줄, 아래에 금액 + 삭제), 결제수단 추가, 적립 적용.
 * 쿠폰·회원권 칸은 로그인 전용이라 게스트엔 없다. 보유 적립금은 웹에선 적립금 설정으로 가는 버튼인데 여긴 그 화면이 없어 글자로만.
 */
@Composable
internal fun ReservationPaymentSection(
    form: PaymentForm,
    onChange: (PaymentForm) -> Unit,
    settings: PointSettings,
    displayPrice: Int,
    naverDeposit: Int,
    customerPoints: Int,
    error: String?,
    errorKey: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text("결제 종류와 금액을 입력해 주세요.", color = TasColors.DarkGray, fontSize = TasSizes.MediumFont, modifier = Modifier.weight(1f))
            Pill("보유 적립금 ${formatPrice(customerPoints)}", TasColors.PointPillBg, TasColors.PointPillText, TasSizes.SmallFont, 28)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text("서비스 금액 ${formatPrice(displayPrice)}", color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
            if (naverDeposit > 0) {
                Pill("네이버 예약금 -${formatPrice(naverDeposit)}", TasColors.DepositPillBg, TasColors.DepositPillText, TasSizes.XSmallFont, 24)
                Pill("실결제 ${formatPrice(displayPrice - naverDeposit)}", TasColors.ExpectedPillBg, TasColors.ExpectedPillText, TasSizes.XSmallFont, 24)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            form.entries.forEachIndexed { index, entry ->
              // 줄을 지우면 아래 줄이 올라온다 — 줄마다 키를 둬야 펼친 목록·입력 초점이 엉뚱한 줄로 옮겨가지 않는다
              key(entry.key) {
                // '회원권' 등 목록에 없는 종류가 저장돼 있으면 그 줄에서만 보이게(웹과 같다)
                val options = if (entry.method.isNotEmpty() && entry.method !in PAYMENT_METHOD_OPTIONS) listOf(entry.method) + PAYMENT_METHOD_OPTIONS else PAYMENT_METHOD_OPTIONS
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TasSelect(
                        label = "결제종류 ${index + 1}",
                        options = options,
                        selectedIndex = options.indexOf(entry.method),
                        onSelect = { i -> onChange(form.withEntries(form.entries.mapIndexed { j, e -> if (j == index) e.copy(method = options[i]) else e }, settings)) },
                        showLabel = false,
                        compact = true,
                        placeholder = "결제종류",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TasTextField(
                            label = "결제 금액 ${index + 1}",
                            value = entry.amount,
                            onValueChange = { v ->
                                val digits = v.filter { it.isDigit() }.take(9)
                                onChange(form.withEntries(form.entries.mapIndexed { j, e -> if (j == index) e.copy(amount = digits) else e }, settings))
                            },
                            placeholder = "금액",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            showLabel = false,
                            compact = true,
                            modifier = Modifier.weight(1f),
                        )
                        // 웹 StyledPaymentRemoveButton — 옅은 빨강 바탕, 최소 52
                        Box(
                            Modifier
                                .widthIn(min = 52.dp)
                                .height(30.dp)
                                .clip(TasShapes.RadiusSm)
                                .background(TasColors.DangerBg)
                                .border(1.dp, TasColors.DangerBorder, TasShapes.RadiusSm)
                                .clickable(role = Role.Button) { onChange(form.removeEntry(index, settings)) }
                                // 줄마다 '삭제'만 들리면 어느 줄인지 모른다
                                .semantics { contentDescription = "결제 ${index + 1} 삭제" }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("삭제", color = TasColors.Danger, fontSize = TasSizes.SmallFont) }
                    }
                }
              }
            }
        }

        // 웹 StyledPaymentAddButton — 점선 테두리 30, 8 둥글게
        Box(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .drawBehind {
                    val r = 8.dp.toPx()
                    drawRoundRect(
                        TasColors.LightGray,
                        cornerRadius = CornerRadius(r),
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                    )
                }
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button) { onChange(form.withEntries(form.entries + PaymentDraft(), settings)) },
            contentAlignment = Alignment.Center,
        ) { Text("결제수단 추가", color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold) }

        if (settings.enableServiceRate) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier
                        .toggleable(form.pointAward.enabled, role = Role.Checkbox) { onChange(form.copy(pointAward = form.pointAward.copy(enabled = it))) }
                        .heightIn(min = 30.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(checked = form.pointAward.enabled, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = TasColors.Brand), modifier = Modifier.size(20.dp))
                    Text("적립 적용", color = TasColors.DarkGray, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
                }
                // 웹: 끄면 금액 칸이 흐려진다(값은 남는다)
                TasTextField(
                    label = "적립 금액",
                    value = form.pointAward.amount,
                    onValueChange = { v -> onChange(form.copy(pointAward = PointAward(true, v.filter { it.isDigit() }.take(9)), isPointAwardManual = true)) },
                    placeholder = "적립 금액",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    showLabel = false,
                    compact = true,
                    enabled = form.pointAward.enabled,
                )
            }
        }

        // 저장을 누르고 나오는 안내 — 낭독기가 읽고, 화면 밖(키보드·긴 줄 아래)이면 보이는 곳까지 스크롤(FieldError)
        if (error != null) FieldError(error, scrollKey = errorKey)
    }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color, fontSize: androidx.compose.ui.unit.TextUnit, minHeight: Int) {
    Box(
        Modifier
            .heightIn(min = minHeight.dp)
            .background(bg, RoundedCornerShape(percent = 50))
            .padding(horizontal = if (minHeight >= 28) 10.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = fg, fontSize = fontSize, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.lineBox((fontSize.value * 1.2f).roundToInt().sp, fontSize)) }
}
