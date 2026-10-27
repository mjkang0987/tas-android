package kr.co.takeaseat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.co.takeaseat.app.domain.ServiceItem
import kr.co.takeaseat.app.domain.formatDuration
import kr.co.takeaseat.app.domain.formatPrice
import kr.co.takeaseat.app.domain.groupedCatalog
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.drawBehindThemed
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 `calendar/service/ServiceFields.tsx` — 카테고리별 체크 목록(최대 200 스크롤) + 고른 서비스 요약(칩·가격, 총 시간 / 총 가격).
 * shortcut: 웹 카테고리 머리글은 스크롤해도 위에 붙어 있다(sticky). 여기선 같이 흘러간다 — 카테고리가 길어 헷갈리면 LazyColumn stickyHeader 로.
 */
@Composable
fun ServiceFields(
    catalog: List<ServiceItem>,
    catalogMap: Map<String, ServiceItem>,
    serviceColorMap: Map<String, String>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    totalDuration: Int,
    totalPrice: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .clip(TasShapes.RadiusSm)
                .border(1.dp, TasColors.LightGray, TasShapes.RadiusSm)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val groups = groupedCatalog(catalog)
            if (groups.isEmpty()) {
                Text(
                    "등록된 데이터가 없습니다",
                    color = TasColors.DarkGray2,
                    fontSize = TasSizes.MediumFont,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                )
            }
            groups.forEach { (category, items) ->
                Column {
                    Text(
                        category,
                        color = TasColors.DarkGray,
                        fontSize = TasSizes.XSmallFont,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehindThemed { colors -> drawRect(colors.LightGray, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx())) }
                            .padding(8.dp)
                            .semantics { heading() },
                    )
                    items.forEach { item -> ServiceRow(item, item.name in selected, serviceColorMap) { onToggle(item.name) } }
                }
            }
        }
        if (selected.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(TasColors.Black10, TasShapes.RadiusSm)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    selected.forEach { name ->
                        val price = catalogMap[name]?.price ?: 0
                        Row(
                            Modifier
                                .background(TasColors.Surface, TasShapes.RadiusSm)
                                .border(1.dp, TasColors.LightGray, TasShapes.RadiusSm)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ServiceChipList(name, serviceColorMap)
                            if (price > 0) Text(formatPrice(price), color = TasColors.Blue, fontSize = TasSizes.TinyFont)
                        }
                    }
                }
                Text(
                    formatDuration(totalDuration) + if (totalPrice > 0) " / ${formatPrice(totalPrice)}" else "",
                    color = TasColors.DarkGray,
                    fontSize = TasSizes.XSmallFont,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ServiceRow(item: ServiceItem, checked: Boolean, serviceColorMap: Map<String, String>, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = TasColors.BrandFill),
            modifier = Modifier.size(20.dp),
        )
        ServiceChipList(item.name, serviceColorMap)
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.price > 0) Text(formatPrice(item.price), color = TasColors.DarkGray, fontSize = TasSizes.XSmallFont)
            Text(formatDuration(item.durationMinutes), color = TasColors.DarkGray, fontSize = TasSizes.XSmallFont)
        }
    }
}
