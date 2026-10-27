package kr.co.takeaseat.app.ui.components

import kr.co.takeaseat.app.ui.nav.LocalAsideToggle
import kr.co.takeaseat.app.ui.nav.AsideToggleButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.drawBehindThemed
import kr.co.takeaseat.app.ui.theme.TasSizes

/** 웹 Header(모바일) — 높이 48, 제목 18 굵게 #525252, 아래 1px. 오른쪽 [actions]('+ 고객' 등). */
@Composable
fun ScreenHeader(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .drawBehindThemed { colors -> drawRect(colors.LightGray, topLeft = Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx())) }
            .padding(horizontal = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth().align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            // 넓은 화면 — 웹 헤더처럼 맨 왼쪽에 메뉴 펼치기
            LocalAsideToggle.current?.let { AsideToggleButton(it.open, it.toggle) }
            Text(title, color = TasColors.DarkGray, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).semantics { heading() })
            actions()
        }
    }
}

/** 웹 PageHero — 영문 eyebrow(11 굵게 파랑) + 설명(16). 여백은 화면마다 다르다(고객 20, 매출 10). */
@Composable
fun PageHero(eyebrow: String, subtitle: String, contentPadding: PaddingValues) {
    Column(Modifier.padding(contentPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(eyebrow, color = TasColors.Blue, fontSize = TasSizes.XSmallFont, fontWeight = FontWeight.Bold, letterSpacing = 0.18.em)
        Text(subtitle, color = TasColors.DarkGray2, fontSize = TasSizes.LargeFont)
    }
}
