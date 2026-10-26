package kr.co.takeaseat.app.ui.nav

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kr.co.takeaseat.app.R
import kr.co.takeaseat.app.domain.CalendarView
import kr.co.takeaseat.app.ui.components.ChevronDownIcon
import kr.co.takeaseat.app.ui.settings.SettingsPage
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes
import kr.co.takeaseat.app.ui.theme.TasSizes

/** 넓은 화면이면 헤더 왼쪽 메뉴 버튼 상태 — 좁은 화면(아래 탭바)은 null. */
class AsideToggle(val open: Boolean, val toggle: () -> Unit)

val LocalAsideToggle = compositionLocalOf<AsideToggle?> { null }

/** 왼쪽 메뉴에서 고를 수 있는 곳 — 웹 `SETTINGS_SUBMENU`(매출·매장·적립금·서비스·담당자·고객 명단) + 앱 전용 예약 알림(설정 메뉴 화면). */
enum class AsideDestination { Revenue, Store, Point, Service, Assignee, Customers, AppSettings }

/**
 * 웹 `layout/Aside.tsx` — 넓은 화면(웹 641px 이상, 접은 폰을 편 폴드 등)에서 아래 탭바 대신 쓰는 왼쪽 메뉴(200dp, 어두운 바탕).
 * 차례: 로고 · 매장명 · 게스트 / 예약관리(일·3일·주·월·년) / 예약추가 · 고객추가 / 설정(매출 … 고객 명단).
 * 웹에 있는 계정 관리·멤버·네이버·SNS·고객센터는 앱에 아직 화면이 없어 뺀다(설정 메뉴 화면과 같은 규칙).
 */
@Composable
fun AsideMenu(
    storeName: String?,
    tab: TasTab,
    calendarView: CalendarView,
    settingsPage: String?,
    usePointSystem: Boolean,
    serviceLabel: String,
    assigneeLabel: String,
    onSelectView: (CalendarView) -> Unit,
    onCreateReservation: () -> Unit,
    onCreateCustomer: () -> Unit,
    onOpen: (AsideDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    var reservationOpen by rememberSaveable { mutableStateOf(true) }
    var settingsOpen by rememberSaveable { mutableStateOf(true) }
    Column(modifier.width(AsideWidth).fillMaxHeight().background(TasColors.AsideBg)) {
        // 웹 StyledBrandLink(높이 48, 로고 30) · 매장명(11) · 게스트(12 굵게)
        Box(Modifier.height(48.dp).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
            Image(painterResource(R.drawable.ic_logo), contentDescription = "TAS", modifier = Modifier.size(30.dp))
        }
        if (!storeName.isNullOrBlank()) {
            Text(storeName, color = TasColors.AsideText, fontSize = TasSizes.XSmallFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 6.dp))
        }
        Text("게스트", color = TasColors.AsideText, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp))

        // 웹 StyledNav — padding 8 10 0 0, 줄 사이 2
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp, end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AccordionToggle(TabIconKind.CalendarManage, "예약관리", reservationOpen) { reservationOpen = !reservationOpen }
            if (reservationOpen) {
                CalendarView.entries.forEach { view ->
                    SubItem(view.asideIcon, view.asideLabel, active = tab == TasTab.Calendar && calendarView == view) { onSelectView(view) }
                }
            }
            Divider()
            Item(TabIconKind.Create, "예약추가", onClick = onCreateReservation)
            Item(TabIconKind.CustomerAdd, "고객추가", onClick = onCreateCustomer)
            Divider()
            AccordionToggle(TabIconKind.Settings, "설정", settingsOpen) { settingsOpen = !settingsOpen }
            if (settingsOpen) {
                SubItem(TabIconKind.Revenue, "매출", active = tab == TasTab.Revenue) { onOpen(AsideDestination.Revenue) }
                SubItem(TabIconKind.Store, "매장 관리", active = tab == TasTab.Settings && settingsPage == SettingsPage.STORE) { onOpen(AsideDestination.Store) }
                if (usePointSystem) {
                    SubItem(TabIconKind.Point, "적립금 관리", active = tab == TasTab.Settings && settingsPage == SettingsPage.POINT) { onOpen(AsideDestination.Point) }
                }
                SubItem(TabIconKind.Service, "$serviceLabel 관리", active = tab == TasTab.Settings && settingsPage == SettingsPage.SERVICE) { onOpen(AsideDestination.Service) }
                SubItem(TabIconKind.Assignee, "$assigneeLabel 관리", active = tab == TasTab.Settings && settingsPage == SettingsPage.ASSIGNEE) { onOpen(AsideDestination.Assignee) }
                SubItem(TabIconKind.Customers, "고객 명단", active = tab == TasTab.Customers) { onOpen(AsideDestination.Customers) }
                // 앱 전용(예약 알림)·약관 링크는 설정 메뉴 화면에 있다 — 웹엔 없는 줄
                SubItem(TabIconKind.Bell, "알림·약관", active = tab == TasTab.Settings && settingsPage == null) { onOpen(AsideDestination.AppSettings) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 웹 Header StyledAsideToggle — 32 사각 버튼, 펼침/접힘 화살촉. 누르는 자리는 44. */
@Composable
fun AsideToggleButton(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp)
            .clickable(role = Role.Button, onClickLabel = if (open) "사이드바 접기" else "사이드바 펼치기", onClick = onClick)
            .semantics { stateDescription = if (open) "펼침" else "접힘" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            (if (open) TabIconKind.SidebarClose else TabIconKind.SidebarOpen).toImageVector(2f),
            contentDescription = if (open) "사이드바 접기" else "사이드바 펼치기",
            tint = TasColors.DarkGray,
            modifier = Modifier.size(18.dp),
        )
    }
}

private val AsideWidth = 200.dp // --aside-width

/** 이 폭을 넘으면 아래 탭바 대신 왼쪽 메뉴 — 웹 `@media (max-width: 640px)` 의 반대편(641px 이상). */
val WIDE_MIN = 640.dp

/** 웹 localStorage 'aside-visible' — 메뉴를 펴 둔 채로 끄면 다음에도 펴져 있다. 처음엔 접힘(웹 기본). */
object AsidePrefs {
    private fun prefs(context: Context) = context.getSharedPreferences("layout", Context.MODE_PRIVATE)
    fun open(context: Context): Boolean = prefs(context).getBoolean("aside-visible", false)
    fun setOpen(context: Context, open: Boolean) = prefs(context).edit { putBoolean("aside-visible", open) }
}

private val CalendarView.asideLabel: String
    get() = when (this) {
        CalendarView.Day -> "일별"
        CalendarView.Three -> "3일"
        CalendarView.Week -> "주별"
        CalendarView.Month -> "월별"
        CalendarView.Year -> "연별"
    }

private val CalendarView.asideIcon: TabIconKind
    get() = when (this) {
        CalendarView.Day -> TabIconKind.Day
        CalendarView.Three -> TabIconKind.Three
        CalendarView.Week -> TabIconKind.Week
        CalendarView.Month -> TabIconKind.Month
        CalendarView.Year -> TabIconKind.Year
    }

/** 웹 StyledCreateButton — 높이 36, 좌우 8, 아이콘 16 + 8 + 12 글자. */
@Composable
private fun Item(icon: TabIconKind, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .clip(TasShapes.RadiusMd)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon.toImageVector(1.8f), contentDescription = null, tint = TasColors.AsideText, modifier = Modifier.size(16.dp))
        Text(label, color = TasColors.AsideText, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold)
    }
}

/** 웹 StyledAccordionToggle — 묶음 머리(예약관리·설정), 오른쪽 꺾쇠. */
@Composable
private fun AccordionToggle(icon: TabIconKind, label: String, open: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .clip(TasShapes.RadiusMd)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { heading(); stateDescription = if (open) "펼침" else "접힘" }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon.toImageVector(1.8f), contentDescription = null, tint = TasColors.AsideText, modifier = Modifier.size(16.dp))
        Text(label, color = TasColors.AsideText, fontSize = TasSizes.SmallFont, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(ChevronDownIcon, contentDescription = null, tint = TasColors.AsideText, modifier = Modifier.size(14.dp).rotate(if (open) 180f else 0f))
    }
}

/** 웹 StyledSubNavLink — 높이 32, 왼쪽 20 들여쓰기, 고른 곳은 보라 채움 + 흰 글자. */
@Composable
private fun SubItem(icon: TabIconKind, label: String, active: Boolean, onClick: () -> Unit) {
    val fg = if (active) TasColors.White else TasColors.AsideText
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .clip(TasShapes.RadiusMd)
            .background(if (active) TasColors.BrandFill else TasColors.AsideBg)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { selected = active }
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon.toImageVector(1.8f), contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text(label, color = fg, fontSize = TasSizes.SmallFont, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
    }
}

/** 웹 StyledDivider — 위아래 8, 1px. */
@Composable
private fun Divider() {
    Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(TasColors.AsideDivider))
}
