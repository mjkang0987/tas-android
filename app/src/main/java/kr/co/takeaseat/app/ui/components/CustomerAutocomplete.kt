package kr.co.takeaseat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kr.co.takeaseat.app.domain.Customer
import kr.co.takeaseat.app.domain.formatTel
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

/**
 * 웹 `customers/CustomerAutocomplete.tsx` — 이름·연락처로 찾아 고르는 입력. 추천은 [showSuggestions] 일 때 아래에 뜬다(최대 160).
 * 웹은 추천을 아래 칸 **위에 겹쳐** 띄우지만 여기선 아래 칸을 밀어낸다 — 겹친 목록은 시트 스크롤·터치 영역 밖으로 나가 눌리지 않을 수 있다.
 */
@Composable
fun CustomerAutocomplete(
    label: String,
    placeholder: String,
    query: String,
    customers: List<Customer>,
    selectedId: Int,
    showSuggestions: Boolean,
    onQueryChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onSelect: (Customer) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        TasTextField(label = label, value = query, onValueChange = onQueryChange, placeholder = placeholder, onFocusChange = onFocusChange)
        val empty = customers.isEmpty()
        if (showSuggestions && (!empty || query.isNotBlank())) {
            Column(
                Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .dropShadow(TasShapes.RadiusSm, Shadow(radius = 12.dp, color = Color.Black.copy(alpha = 0.1f), offset = DpOffset(0.dp, 4.dp)))
                    .clip(TasShapes.RadiusSm)
                    .background(TasColors.White)
                    .border(1.dp, TasColors.LightGray, TasShapes.RadiusSm)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
            ) {
                if (empty) {
                    Text(
                        "검색 결과 없음",
                        style = TextStyle(color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
                customers.forEach { customer ->
                    val isSelected = customer.id == selectedId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) TasColors.Black10 else Color.Transparent)
                            .clickable(role = Role.Button) { onSelect(customer) }
                            .semantics(mergeDescendants = true) { selected = isSelected }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(customer.name, color = TasColors.Black, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(formatTel(customer.tel), color = TasColors.DarkGray2, fontSize = TasSizes.SmallFont, style = TextStyle(fontFeatureSettings = "tnum"))
                    }
                }
            }
        }
    }
}
