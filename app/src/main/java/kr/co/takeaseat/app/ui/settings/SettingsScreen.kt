package kr.co.takeaseat.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kr.co.takeaseat.app.domain.REMINDER_LEAD_OPTIONS
import kr.co.takeaseat.app.domain.reminderLeadLabel
import kr.co.takeaseat.app.notify.ReminderScheduler
import kr.co.takeaseat.app.ui.components.LocalToast
import kr.co.takeaseat.app.ui.components.TasSelect
import kr.co.takeaseat.app.ui.components.ToastType
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.domain.storeLabels
import kr.co.takeaseat.app.ui.components.ScreenHeader
import kr.co.takeaseat.app.ui.components.svgLineIcon
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes

/** 설정 탭 안의 화면 — 메뉴(null) 또는 하위 화면. 글자로 둬 회전해도 남는다. */
object SettingsPage {
    const val STORE = "store"
    const val SERVICE = "service"
    const val ASSIGNEE = "assignee"
    const val POINT = "point"
}

/**
 * 웹 `/menu`(모바일 설정) — 위 카드(매장명·게스트) · 항목 묶음 · 이용약관·개인정보처리방침.
 * 아직 없는 화면의 줄은 숨긴다(계정·고객센터·사용 안내·로그아웃은 로그인/온보딩 때). 적립금 관리는 적립금 시스템을 켰을 때만.
 * [page] 가 있으면 그 하위 화면 — 뒤로 가기·설정 탭 다시 누르기로 메뉴로 돌아온다(웹은 탭을 다시 눌러 /menu 로).
 */
@Composable
fun SettingsScreen(store: GuestStore, page: String?, onPage: (String?) -> Unit, modifier: Modifier = Modifier) {
    val snapshot by store.snapshot.collectAsState()
    val labels = remember(snapshot.shopType) { storeLabels(snapshot.shopType) }
    if (page != null) BackHandler { onPage(null) }
    when (page) {
        SettingsPage.STORE -> StoreManageScreen(store, modifier)
        SettingsPage.SERVICE -> ServiceManageScreen(store, modifier)
        SettingsPage.ASSIGNEE -> AssigneeManageScreen(store, modifier)
        SettingsPage.POINT -> PointManageScreen(store, modifier)
        else -> Column(modifier.fillMaxSize()) {
            ScreenHeader("설정")
            SettingsMenu(storeName = snapshot.storeName, serviceLabel = labels.service, assigneeLabel = labels.assignee, usePointSystem = snapshot.usePointSystem == true, onPage = onPage)
        }
    }
}

@Composable
private fun SettingsMenu(storeName: String?, serviceLabel: String, assigneeLabel: String, usePointSystem: Boolean, onPage: (String) -> Unit) {
    val context = LocalContext.current
    fun openWeb(path: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$WEB_ORIGIN$path"))) }
    // 웹 StyledPage — 12 12 24, 사이 14
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 웹 StyledHeaderCard — 매장명(11 굵게 회색) · 게스트(18 굵게)
        Column(
            Modifier.fillMaxWidth().clip(GroupShape).background(TasColors.White).border(1.dp, TasColors.LightGray, GroupShape).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (!storeName.isNullOrBlank()) Text(storeName, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont, fontWeight = FontWeight.SemiBold)
            Text("게스트", color = TasColors.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        MenuGroup {
            MenuRow(StoreIcon, "매장 관리") { onPage(SettingsPage.STORE) }
            // 웹 settingsMenu — 적립금 시스템을 켠 매장만
            if (usePointSystem) {
                MenuDivider()
                MenuRow(PointIcon, "적립금 관리") { onPage(SettingsPage.POINT) }
            }
            MenuDivider()
            MenuRow(ServiceIcon, "$serviceLabel 관리") { onPage(SettingsPage.SERVICE) }
            MenuDivider()
            MenuRow(AssigneeIcon, "$assigneeLabel 관리") { onPage(SettingsPage.ASSIGNEE) }
        }
        // 웹에 없는 안드로이드 기능(지시자 2026-10-22) — 예약 시각 전 휴대폰 알림
        MenuGroup { ReminderRow() }
        // 웹 StyledLegalLinks — 11 회색, 사이 6 16
        FlowRow(Modifier.padding(start = 6.dp, end = 6.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LegalLink("이용약관") { openWeb("/terms") }
            LegalLink("개인정보처리방침") { openWeb("/privacy") }
        }
    }
}

/**
 * 예약 알림 줄 — 메뉴 줄과 같은 모양(아이콘 상자·이름)에 오른쪽은 › 대신 고르는 칸(끔·10분·30분·1시간 전).
 * 켤 때 알림 권한이 없으면(Android 13+) 묻고, 거절하면 휴대폰 설정에서 켜라고 알린다.
 */
@Composable
private fun ReminderRow() {
    val context = LocalContext.current
    val toast = LocalToast.current
    var lead by remember { mutableIntStateOf(ReminderScheduler.leadMinutes(context)) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) toast.show("알림 권한이 꺼져 있어 알림을 보낼 수 없습니다. 휴대폰 설정에서 켜 주세요.", ToastType.Info)
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(TasColors.BrandBg), contentAlignment = Alignment.Center) {
            Icon(BellIcon, contentDescription = null, tint = TasColors.Brand, modifier = Modifier.size(18.dp))
        }
        // 이름은 옆 고르는 칸이 "예약 알림, 30분 전" 으로 함께 읽는다 — 글자만 따로 한 번 더 읽히지 않게
        Text("예약 알림", color = TasColors.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).clearAndSetSemantics {})
        TasSelect(
            label = "예약 알림",
            options = REMINDER_LEAD_OPTIONS.map(::reminderLeadLabel),
            selectedIndex = REMINDER_LEAD_OPTIONS.indexOf(lead).coerceAtLeast(0),
            onSelect = { index ->
                lead = REMINDER_LEAD_OPTIONS[index]
                ReminderScheduler.setLeadMinutes(context, lead)
                if (lead > 0 && Build.VERSION.SDK_INT >= 33 && !ReminderScheduler.hasNotificationPermission(context)) {
                    askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            showLabel = false,
            compact = true,
            modifier = Modifier.width(104.dp),
        )
    }
}

/** 웹 rowBase border-top — 줄 사이 1px 옅은 회색(첫 줄 위는 없음). */
@Composable
private fun MenuDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(TasColors.LightGray))
}

/** 웹 StyledGroup — 1px 옅은 회색 테두리, 8 둥글게, 줄 사이 1px. */
@Composable
private fun MenuGroup(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(GroupShape).background(TasColors.White).border(1.dp, TasColors.LightGray, GroupShape)) { content() }
}

/** 웹 StyledRow — 52 높이, 좌우 14, 아이콘 상자 30(브랜드 옅은 배경) · 이름 14 굵게 · › . */
@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(TasColors.BrandBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = TasColors.Brand, modifier = Modifier.size(18.dp))
        }
        Text(label, color = TasColors.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(ChevronRightIcon, contentDescription = null, tint = TasColors.DarkGray2, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LegalLink(text: String, onClick: () -> Unit) {
    Text(text, color = TasColors.DarkGray2, fontSize = TasSizes.XSmallFont,
        modifier = Modifier.heightIn(min = 24.dp).wrapContentHeight(Alignment.CenterVertically).clickable(role = Role.Button, onClickLabel = "웹 페이지 열기", onClick = onClick))
}

private const val WEB_ORIGIN = "https://takeaseat.co.kr"
private val GroupShape = RoundedCornerShape(8.dp) // --radius-lg

// 웹 AsideMenuIcon(viewBox 24, 선 1.8) · 줄 끝 꺾쇠(선 2)
private val StoreIcon = svgLineIcon(24f, listOf("M5 10.5H19V19.5H5V10.5ZM6.5 10.5V7.5C6.5 5.6 8.1 4 10 4H14C15.9 4 17.5 5.6 17.5 7.5V10.5M9 14H15"), strokeWidth = 1.8f, name = "store")
// 웹 NaverSyncNotification 의 종(bell)
private val BellIcon = svgLineIcon(24f, listOf("M18 8A6 6 0 0 0 6 8C6 15 3 17 3 17H21C21 17 18 15 18 8", "M13.73 21A2 2 0 0 1 10.27 21"), strokeWidth = 1.8f, name = "bell")
private val PointIcon = svgLineIcon(24f, listOf("M20.5 12A8.5 8.5 0 1 1 3.5 12A8.5 8.5 0 1 1 20.5 12Z", "M12 8.5V15.5M9.5 10.5H14.5M9.5 13.5H14"), strokeWidth = 1.8f, name = "point")
private val AssigneeIcon = svgLineIcon(
    24f,
    listOf(
        "M7.5 4H16.5A2.5 2.5 0 0 1 19 6.5V17.5A2.5 2.5 0 0 1 16.5 20H7.5A2.5 2.5 0 0 1 5 17.5V6.5A2.5 2.5 0 0 1 7.5 4Z",
        "M9.5 4V2.6H14.5V4",
        "M14.2 10A2.2 2.2 0 1 1 9.8 10A2.2 2.2 0 1 1 14.2 10Z",
        "M8.5 16.6C8.5 14.6 10 13.6 12 13.6C14 13.6 15.5 14.6 15.5 16.6",
    ),
    strokeWidth = 1.8f,
    name = "assignee",
)
private val ServiceIcon = svgLineIcon(24f, listOf("M7 5.5H17M7 10.5H17M7 15.5H13M5 5.5H5.01M5 10.5H5.01M5 15.5H5.01"), strokeWidth = 1.8f, name = "service")
private val ChevronRightIcon = svgLineIcon(24f, listOf("M9 6l6 6-6 6"), strokeWidth = 2f, name = "chevron-right")
