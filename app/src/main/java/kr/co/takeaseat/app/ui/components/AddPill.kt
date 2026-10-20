package kr.co.takeaseat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

/** 웹 StyledMobileAddPill — 헤더 오른쪽 보라 알약 '+ 예약'(캘린더)·'+ 고객'(고객 명단). 32 높이, 좌 9·우 13, 보라 그림자 0 4 12. */
@Composable
fun AddPill(text: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(32.dp)
            .dropShadow(TasShapes.Chip, Shadow(radius = 12.dp, color = TasColors.BrandShadow, offset = DpOffset(0.dp, 4.dp)))
            .clip(TasShapes.Chip)
            .background(TasColors.Brand)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(start = 9.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(plusIcon, contentDescription = null, tint = TasColors.White, modifier = Modifier.size(15.dp))
        Text(text, color = TasColors.White, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.clearAndSetSemantics {})
    }
}

private val plusIcon: ImageVector = svgLineIcon(24f, listOf("M12 5v14M5 12h14"), strokeWidth = 2.3f)
