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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kr.co.takeaseat.app.domain.parseServiceString
import kr.co.takeaseat.app.domain.serviceColor
import kr.co.takeaseat.app.domain.statusBadgeColors
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
            .lineBox(10.sp, TasSizes.TinyFont), // 웹 높이 14 = 줄 10(line-height 1) + 2·2
    )
}

enum class LabelTone(val text: Color, val bg: Color, val border: Color) {
    Warning(TasColors.WarningText, TasColors.WarningBgSoft, TasColors.WarningBorderSoft),
    Info(TasColors.Info, TasColors.InfoBg, TasColors.InfoBorder),
    Brand(TasColors.White, Color(0xFF208200), Color(0xFF1A6B00)), // --naver-color / --naver-color-dark (흰 글자 대비 4.5 이상)
}

/**
 * 웹 `ui/LabelBadge.tsx`(size sm) — 연한 배경 + 진한 글자 + 1px 테두리. pill 은 알약, soft 는 6 둥글게.
 * 확정대기(warning pill), 예약경로(info soft, 글자 10) 등. 쓰는 톤만 옮겼다.
 */
@Composable
fun LabelBadge(text: String, tone: LabelTone, modifier: Modifier = Modifier, pill: Boolean = false, fontSize: TextUnit = TasSizes.XSmallFont) {
    val shape = if (pill) TasShapes.Chip else TasShapes.RadiusMd
    Text(
        text = text,
        color = tone.text,
        fontSize = fontSize,
        lineHeight = fontSize,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .background(tone.bg, shape)
            .border(1.dp, tone.border, shape)
            // 웹은 1px 테두리가 패딩 바깥에 붙는다(높이 = 1 + 3 + 줄 + 3 + 1). 안드로이드 border 는 안쪽에 그려 1dp 를 더한다.
            .padding(horizontal = 7.dp, vertical = 4.dp)
            .lineBox((fontSize.value * 1.2f).sp, fontSize),
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
    gap: Dp = 6.dp, // 웹 StyledServiceList 6 — 모바일 타임라인 카드만 4
) {
    val names = parseServiceString(service, serviceColorMap.keys)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        names.forEach { name ->
            val color = hexColor(serviceColor(name, serviceColorMap))
            Text(
                text = name,
                color = if (muted) TasColors.DarkGray2 else color,
                fontSize = TasSizes.XSmallFont,
                lineHeight = TasSizes.XSmallFont,
                fontWeight = FontWeight.SemiBold,
                // 한 줄 상자(lineBox)라 줄이 넘치면 알약 밖으로 그려진다 — 한 줄로 두고 넘치면 말줄임.
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .background(color.copy(alpha = 0x18 / 255f), TasShapes.Chip)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
                    .lineBox(13.2.sp, TasSizes.XSmallFont), // 웹 칩 19.2 = 11 × 1.2 + 3·2
            )
        }
    }
}

/** CSS 색 "#RGB" / "#RRGGBB" / "#RRGGBBAA"(알파가 **뒤**) → Color. 망가진 값은 웹 기본 회색(#999). */
fun hexColor(hex: String): Color {
    val raw = hex.removePrefix("#")
    val full = if (raw.length == 3) raw.map { "$it$it" }.joinToString("") else raw
    return when (full.length) {
        8 -> full.toLongOrNull(16)?.let { Color(((it and 0xFF) shl 24) or (it ushr 8)) }
        6 -> full.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
        else -> null
    } ?: Color(0xFF999999)
}

/** 웹 `ReservationStatusBadge` — 결제완료·미결제·취소·노쇼·완료·확정대기. 색은 RESERVATION_STATUS_BADGE_STYLES. */
@Composable
fun StatusBadge(type: String, text: String, modifier: Modifier = Modifier) {
    val colors = statusBadgeColors(type)
    Text(
        text = text,
        color = hexColor(colors.color),
        fontSize = TasSizes.XSmallFont,
        lineHeight = TasSizes.XSmallFont, // 줄높이를 글자 크기로 — 1.2em 이면 한글이 아래로 2dp 처져 보였다(신규 배지와 같은 방식)
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = modifier
            .background(hexColor(colors.bg), TasShapes.RadiusSm)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .lineBox(14.sp, TasSizes.XSmallFont), // 웹 높이 20 = 3 + 14 + 3
    )
}

/** 웹 `AssigneeLabel`(ColorTag) — 담당자 색 알약, 흰 글자. */
@Composable
fun AssigneeLabel(name: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = name,
        color = Color.White,
        fontSize = TasSizes.XSmallFont,
        lineHeight = TasSizes.XSmallFont,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = modifier
            .background(color, TasShapes.Chip)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .lineBox(14.sp, TasSizes.XSmallFont), // 웹 높이 18 = 2 + 14 + 2
    )
}
