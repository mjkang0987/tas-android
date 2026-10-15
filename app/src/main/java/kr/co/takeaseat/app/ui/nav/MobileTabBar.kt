package kr.co.takeaseat.app.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

/** 웹 `MobileTabBar.tsx` 의 `TABS` — 순서·라벨·아이콘을 웹과 같게. */
enum class TasTab(val label: String, val icon: TabIconKind) {
    Calendar("캘린더", TabIconKind.CalendarManage),
    Customers("고객", TabIconKind.Customers),
    Revenue("매출", TabIconKind.Revenue),
    Settings("설정", TabIconKind.Settings),
}

/**
 * 웹 `MobileTabBar` 이식 — 화면 아래에 떠 있는 알약 모양 탭바.
 * 콘텐츠 위에 겹치지 않고 흐름 자식으로 놓여 여백으로 띄운다(웹 주석과 같은 이유).
 */
@Composable
fun MobileTabBar(selected: TasTab, onSelect: (TasTab) -> Unit, modifier: Modifier = Modifier) {
    // 웹: margin 0 12px max(env(safe-area-inset-bottom), 10px)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Row(
        modifier
            .padding(start = 12.dp, end = 12.dp, bottom = maxOf(navBottom, 10.dp))
            .dropShadow(
                shape = TasShapes.Chip,
                shadow = Shadow(radius = TasSizes.ShadowMdBlur, color = TasColors.ShadowMd, offset = DpOffset(0.dp, TasSizes.ShadowMdOffsetY)),
            )
            .clip(TasShapes.Chip)
            .background(TasColors.GlassBg)
            .border(1.dp, TasColors.White60, TasShapes.Chip)
            .padding(2.dp)
            .selectableGroup(),
    ) {
        TasTab.entries.forEach { tab ->
            TabItem(tab = tab, active = tab == selected, onClick = { onSelect(tab) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TabItem(tab: TasTab, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val color = if (active) TasColors.Brand else TasColors.DarkGray2
    // 웹은 선택 알약 배경 + :active opacity .6 으로 눌림을 보인다 — 머티리얼 리플 대신 웹을 따른다.
    Column(
        modifier
            .alpha(if (pressed) 0.6f else 1f)
            .clip(TasShapes.Chip)
            .background(if (active) TasColors.BrandBg else Color.Transparent)
            .selectable(selected = active, onClick = onClick, role = Role.Tab, interactionSource = interaction, indication = null)
            // 7 + 아이콘 16 + 간격 4 + 라벨 10 + 7 = 44dp(터치 타깃 최소) — 웹과 같은 계산.
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val strokeWidth = if (active) 1.7f else 1.5f
        Icon(
            imageVector = remember(tab, strokeWidth) { tab.icon.toImageVector(strokeWidth) },
            contentDescription = null, // 라벨 텍스트를 읽는다
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = tab.label,
            color = color,
            fontSize = TasSizes.TinyFont,
            lineHeight = TasSizes.TinyFont,
            fontWeight = FontWeight.Bold,
        )
    }
}
