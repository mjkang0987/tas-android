package kr.co.takeaseat.app.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kr.co.takeaseat.app.domain.Customer
import kr.co.takeaseat.app.domain.POINT_HISTORY_LABELS
import kr.co.takeaseat.app.domain.PointHistoryEntry
import kr.co.takeaseat.app.domain.PointSettings
import kr.co.takeaseat.app.domain.RECEIVED_PAYMENT_METHODS
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.formatHistoryTimestamp
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.rechargeOptions
import kr.co.takeaseat.app.ui.components.ActionButton
import kr.co.takeaseat.app.ui.components.ActionTone
import kr.co.takeaseat.app.ui.components.DetailSheet
import kr.co.takeaseat.app.ui.components.TasSelect
import kr.co.takeaseat.app.ui.components.TasTextField
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 `AddressCustomerRecharge` — 펼친 고객 행의 적립금 충전 + 적립금 이력(가장 최근 1건, '더보기' = 전체 창).
 * 매장이 충전을 안 쓰고 이력도 없으면 아무것도 안 그린다. 규칙은 `domain/CustomerPoints.kt`.
 */
@Composable
internal fun CustomerPointSection(
    customer: Customer,
    settings: PointSettings,
    reservations: List<Reservation>,
    onCharge: (amount: Int, paid: Int, method: String, custom: Boolean) -> Unit,
    onReservationClick: (Reservation) -> Unit,
) {
    val histories = remember(customer.pointHistories) { customer.pointHistories.orEmpty().asReversed() }
    if (!settings.enableRecharge && histories.isEmpty()) return
    val options = remember(settings.rechargeRules) { rechargeOptions(settings) }
    // 고른 것은 이름으로 기억한다 — 번호로 두면 매장 규칙이 바뀌었을 때 다른 규칙·직접입력을 가리켰다. 없으면 첫 규칙.
    val labels = remember(options) { options.map { it.label } + CUSTOM_LABEL }
    var selectedLabel by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = labels.indexOf(selectedLabel).takeIf { it >= 0 } ?: 0
    var customAmount by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf("카드") }
    var showAll by rememberSaveable { mutableStateOf(false) }
    // 예약에 묶인 이력만 누를 수 있다 — 그 예약 상세로
    fun onHistoryClick(entry: PointHistoryEntry): (() -> Unit)? =
        entry.relatedReservationId?.let { id -> reservations.firstOrNull { it.id == id } }?.let { r -> { onReservationClick(r) } }

    Column(
        Modifier
            .fillMaxWidth()
            // 웹: 위 1px 점선(옅은 회색)
            .drawBehind {
                drawLine(
                    TasColors.LightGray, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                )
            }
            .padding(top = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (settings.enableRecharge) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, itemVerticalAlignment = Alignment.CenterVertically) {
                Text("적립금 충전", color = TasColors.DarkGray, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() })
                Text("현재 잔액 ${formatPrice(customer.points ?: 0)}", color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont)
            }
            val isCustom = selected == options.size
            TasSelect(
                label = "충전 금액 선택",
                options = labels,
                selectedIndex = selected,
                onSelect = { selectedLabel = labels[it] },
                showLabel = false,
                compact = true,
            )
            TasSelect(
                label = "충전 결제수단",
                options = RECEIVED_PAYMENT_METHODS,
                selectedIndex = RECEIVED_PAYMENT_METHODS.indexOf(method),
                onSelect = { method = RECEIVED_PAYMENT_METHODS[it] },
                showLabel = false,
                compact = true,
            )
            if (isCustom) {
                TasTextField(
                    label = "직접 입력 충전금액",
                    value = customAmount,
                    onValueChange = { v -> customAmount = v.filter { it.isDigit() }.take(9) },
                    placeholder = "충전금액",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    showLabel = false,
                    compact = true,
                )
            }
            ActionButton(
                "충전",
                onClick = {
                    // 적립 = 기준 + 보너스, 받은 돈 = 기준(직접입력은 둘 다 친 금액). 0원이면 아무것도 안 함(웹과 같다).
                    val option = options.getOrNull(selected)
                    val amount = if (isCustom) customAmount.toIntOrNull() ?: 0 else option?.total ?: 0
                    if (amount > 0) {
                        onCharge(amount, if (isCustom) amount else option?.paid ?: 0, method, isCustom)
                        customAmount = ""
                        selectedLabel = null
                    }
                },
                tone = ActionTone.Blue,
                modifier = Modifier.width(84.dp),
            )
        }
        if (histories.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("적립금 이력 (${histories.size})", color = TasColors.DarkGray, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).semantics { heading() })
                if (histories.size > 1) {
                    Text(
                        "더보기",
                        color = TasColors.Blue,
                        fontSize = TasSizes.SmallFont,
                        fontWeight = FontWeight.SemiBold,
                        // 누르는 영역 높이 24 이상
                        modifier = Modifier.heightIn(min = 24.dp).wrapContentHeight(Alignment.CenterVertically)
                            .clickable(role = Role.Button, onClickLabel = "적립금 이력 전체 보기") { showAll = true },
                    )
                }
            }
            PointHistoryCard(histories.first(), onHistoryClick(histories.first()))
        }
    }

    if (showAll) {
        DetailSheet(title = "적립금 이력 (${histories.size})", onDismiss = { showAll = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                histories.forEach { PointHistoryCard(it, onHistoryClick(it)) }
            }
        }
    }
}

private const val CUSTOM_LABEL = "직접입력"

/** 웹 StyledPointItem — 종류 · ±금액(파랑) / 설명 · 잔액 · 시각. 1px 옅은 회색 테두리, 8 둥글게. */
@Composable
private fun PointHistoryCard(entry: PointHistoryEntry, onClick: (() -> Unit)?) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TasColors.White)
            .border(1.dp, TasColors.LightGray, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = "예약 보기", onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(POINT_HISTORY_LABELS[entry.type] ?: entry.type, color = TasColors.Black, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            Text("${if (entry.delta > 0) "+" else ""}${formatPrice(entry.delta)}", color = TasColors.Blue, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.Bold)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // 웹은 저장된 UTC 를 잘라 9시간 이르게 보여 준다 — 여기선 기기 시간대로
            listOf(entry.description, "잔액 ${formatPrice(entry.balance)}", formatHistoryTimestamp(entry.createdAt)).forEach {
                Text(it, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont)
            }
        }
    }
}
