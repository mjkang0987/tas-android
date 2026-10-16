package kr.co.takeaseat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kr.co.takeaseat.app.domain.parseServiceString
import kr.co.takeaseat.app.domain.serviceColor
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

/** 웹 `ui/NewCustomerBadge.tsx` — 높이 14, 좌우 4, 10px 굵게. */
@Composable
fun NewCustomerBadge(modifier: Modifier = Modifier) {
    Text(
        text = "신규",
        color = TasColors.NewCustomer,
        fontSize = TasSizes.TinyFont,
        lineHeight = TasSizes.TinyFont,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .heightIn(min = 14.dp)
            .widthIn(min = 14.dp)
            .background(TasColors.NewCustomerBg, TasShapes.RadiusSm)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .lineBox(10.sp), // 웹 높이 14 = 줄 10(line-height 1) + 2·2
    )
}

/** 웹 `LabelBadge` tone=warning shape=pill size=sm — 확정대기 표시. */
@Composable
fun WarningPillBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TasColors.WarningText,
        fontSize = TasSizes.XSmallFont,
        lineHeight = 1.2.em,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .background(TasColors.WarningBgSoft, TasShapes.Chip)
            .border(1.dp, TasColors.WarningBorderSoft, TasShapes.Chip)
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .lineBox(13.2.sp),
    )
}

/**
 * 웹 `ui/ServiceChip.tsx` `ServiceChipList` — 서비스마다 제 색 알약(배경 = 색 + 18 알파).
 * [muted] 는 취소·노쇼 카드 — 칩만 색이 남으면 빗금 위에 떠 끝난 예약이 살아 보인다(웹 같은 이유).
 */
@Composable
fun ServiceChipList(
    service: String,
    serviceColorMap: Map<String, String>,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    val names = parseServiceString(service, serviceColorMap.keys)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp), // 모바일 gap 4
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        names.forEach { name ->
            val color = hexColor(serviceColor(name, serviceColorMap))
            Text(
                text = name,
                color = if (muted) TasColors.DarkGray2 else color,
                fontSize = TasSizes.XSmallFont,
                lineHeight = 1.2.em,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(color.copy(alpha = 0x18 / 255f), TasShapes.Chip)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
                    .lineBox(13.2.sp), // 웹 칩 19.2 = 11 × 1.2 + 3·2
            )
        }
    }
}

/** "#RRGGBB" / "#RGB" → Color. 망가진 값은 웹 기본 회색(#999). */
fun hexColor(hex: String): Color {
    val raw = hex.removePrefix("#")
    val full = if (raw.length == 3) raw.map { "$it$it" }.joinToString("") else raw
    val value = full.takeIf { it.length == 6 }?.toLongOrNull(16) ?: 0x999999L
    return Color(0xFF000000 or value)
}
