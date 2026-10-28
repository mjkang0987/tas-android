package kr.co.takeaseat.app

import kr.co.takeaseat.app.ui.components.ToastState
import kr.co.takeaseat.app.ui.components.ToastHost
import kr.co.takeaseat.app.ui.components.LocalToast
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import android.Manifest
import android.content.Intent
import android.graphics.Color
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import kr.co.takeaseat.app.notify.ReminderScheduler
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.data.seedDebugData
import kr.co.takeaseat.app.ui.calendar.CalendarScreen
import kr.co.takeaseat.app.ui.customers.CustomersScreen
import kr.co.takeaseat.app.ui.revenue.RevenueScreen
import kr.co.takeaseat.app.ui.settings.SettingsScreen
import kr.co.takeaseat.app.ui.nav.MobileTabBar
import kr.co.takeaseat.app.ui.nav.TasTab
import kr.co.takeaseat.app.ui.settings.SettingsPage
import kr.co.takeaseat.app.ui.nav.WIDE_MIN
import kr.co.takeaseat.app.ui.nav.LocalAsideToggle
import kr.co.takeaseat.app.ui.nav.AsideToggle
import kr.co.takeaseat.app.ui.nav.AsidePrefs
import kr.co.takeaseat.app.ui.nav.AsideMenu
import kr.co.takeaseat.app.ui.nav.AsideDestination
import kr.co.takeaseat.app.ui.calendar.CalendarCommand
import kr.co.takeaseat.app.domain.storeLabels
import kr.co.takeaseat.app.domain.CalendarView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasTheme

class MainActivity : ComponentActivity() {
    // 예약 알림을 눌러 열었을 때 띄울 예약 — 상세를 연 뒤 비운다
    private var openReservationId by mutableStateOf<Int?>(null)

    private val askNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 시스템 바 아이콘도 테마를 따른다(밝은 화면 = 어두운 아이콘, 어두운 화면 = 밝은 아이콘).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        // shortcut: 첫 실행 때 메인 스레드에서 작은 JSON 파일 하나를 읽는다. 데이터가 커지면 백그라운드 로드로 바꾼다.
        val store = GuestStore.forApp(this).also(::seedDebugData)
        ReminderScheduler.ensureChannel(this)
        if (ReminderScheduler.shouldAskPermissionOnce(this)) askNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        // 회전으로 다시 만들어질 때 같은 알림 인텐트로 상세를 또 열지 않게 처음 만들 때만
        if (savedInstanceState == null) openReservationId = reminderTarget(intent)
        setContent { TasTheme { TasApp(store, openReservationId, onReservationOpened = { openReservationId = null }) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        reminderTarget(intent)?.let { openReservationId = it }
    }

    // 최근 앱에서 다시 열면 시스템이 처음 인텐트(알림)를 또 주므로 그때는 무시한다
    private fun reminderTarget(intent: Intent?): Int? =
        intent?.takeIf { it.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0 }
            ?.getIntExtra(ReminderScheduler.EXTRA_RESERVATION_ID, -1)?.takeIf { it >= 0 }
}

@Composable
private fun TasApp(store: GuestStore, openReservationId: Int?, onReservationOpened: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(TasTab.Calendar) }
    // 예약 알림 — 데이터가 바뀔 때마다(추가·변경·취소) 알람을 다시 맞춘다
    val context = LocalContext.current
    val snapshot by store.snapshot.collectAsState()
    LaunchedEffect(snapshot) { ReminderScheduler.sync(context) }
    // 알림을 눌러 들어오면 캘린더 탭에서 그 예약 상세
    LaunchedEffect(openReservationId) { if (openReservationId != null) tab = TasTab.Calendar }
    // 설정 탭 안의 하위 화면(서비스 관리 등) — 설정 탭을 누르면 늘 메뉴부터(웹 탭이 /menu 로 간다)
    var settingsPage by rememberSaveable { mutableStateOf<String?>(null) }
    // 웹 ToastContainer — 어느 화면에서든 LocalToast.current.show(…)
    val toast = remember { ToastState() }
    // 넓은 화면 왼쪽 메뉴가 화면에 보내는 요청(보기 바꾸기·예약추가·고객추가)
    var calendarCommand by remember { mutableStateOf<CalendarCommand?>(null) }
    var calendarView by rememberSaveable { mutableStateOf(CalendarView.Day) }
    var customerAdd by remember { mutableStateOf(false) }
    // 웹 aside-visible(localStorage) — 다시 켜도 열려 있던 그대로
    var asideOpen by rememberSaveable { mutableStateOf(AsidePrefs.open(context)) }
    CompositionLocalProvider(LocalToast provides toast) {
        // 웹 641px 이상(펼친 폴드·태블릿) = 아래 탭바 대신 왼쪽 메뉴(웹 Aside). 640 이하 = 지금 그대로.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > WIDE_MIN
            Column(
                Modifier
                    .fillMaxSize()
                    // 상태 표시줄 자리는 늘 바탕색 — 어두운 메뉴 위면 밝은 화면의 어두운 시계·배터리 아이콘이 안 보인다
                    .background(TasColors.Surface)
                    .statusBarsPadding()
                    // 키보드가 뜨면 화면을 키보드 위까지로 줄인다(웹 모바일처럼 탭바는 키보드 위에) — 아래쪽 입력칸(고객 메모 태그 등)이 가리지 않게.
                    // 내비게이션 바 몫은 탭바가 이미 띄우므로 뺀다. 시트는 따로 창이라 각자 키보드를 피한다.
                    .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
            ) {
                // 화면은 한 자리에서만 그린다 — 좁/넓에 따라 부르는 자리가 다르면 Compose 가 다른 화면으로 보고
                // 새로 만들어, 접고 펼 때(640dp 넘나듦) 날짜·보기·열린 상세 같은 rememberSaveable 값이 처음으로 돌아간다.
                val aside = wide && asideOpen
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        // 좁을 땐 탭바가 내비게이션 바 몫을 띄운다
                        .then(if (wide) Modifier.background(if (asideOpen) TasColors.AsideBg else TasColors.Surface).navigationBarsPadding() else Modifier),
                ) {
                    if (aside) {
                        val labels = remember(snapshot.shopType) { storeLabels(snapshot.shopType) }
                        AsideMenu(
                            storeName = snapshot.storeName,
                            tab = tab,
                            calendarView = calendarView,
                            settingsPage = settingsPage,
                            usePointSystem = snapshot.usePointSystem == true,
                            serviceLabel = labels.service,
                            assigneeLabel = labels.assignee,
                            onSelectView = { tab = TasTab.Calendar; calendarCommand = CalendarCommand.SelectView(it) },
                            onCreateReservation = { tab = TasTab.Calendar; calendarCommand = CalendarCommand.Create },
                            onCreateCustomer = { tab = TasTab.Customers; customerAdd = true },
                            onOpen = { dest ->
                                when (dest) {
                                    AsideDestination.Revenue -> tab = TasTab.Revenue
                                    AsideDestination.Customers -> tab = TasTab.Customers
                                    else -> {
                                        tab = TasTab.Settings
                                        settingsPage = when (dest) {
                                            AsideDestination.Store -> SettingsPage.STORE
                                            AsideDestination.Point -> SettingsPage.POINT
                                            AsideDestination.Service -> SettingsPage.SERVICE
                                            AsideDestination.Assignee -> SettingsPage.ASSIGNEE
                                            else -> null
                                        }
                                    }
                                }
                            },
                        )
                    }
                    // 웹: 메뉴를 펴면 본문이 둥근 카드로 떠 보인다(위·오른쪽·아래 6)
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(if (aside) Modifier.padding(top = 6.dp, end = 6.dp, bottom = 6.dp).clip(RoundedCornerShape(10.dp)).background(TasColors.Surface) else Modifier),
                    ) {
                        // 넓은 화면에서만 — 화면들은 이 값이 있으면 넓은 화면 배치(보기 탭·'+ 예약' 감춤 등)
                        CompositionLocalProvider(LocalAsideToggle provides if (wide) AsideToggle(asideOpen) {
                            asideOpen = !asideOpen
                            AsidePrefs.setOpen(context, asideOpen)
                        } else null) {
                            when (tab) {
                                TasTab.Calendar -> CalendarScreen(
                                    store, openReservationId = openReservationId, onReservationOpened = onReservationOpened,
                                    command = calendarCommand, onCommandHandled = { calendarCommand = null }, onViewShown = { calendarView = it },
                                )
                                TasTab.Customers -> CustomersScreen(store, addRequested = customerAdd, onAddHandled = { customerAdd = false })
                                TasTab.Revenue -> RevenueScreen(store)
                                TasTab.Settings -> SettingsScreen(store, settingsPage, onPage = { settingsPage = it })
                            }
                        }
                    }
                }
                if (!wide) MobileTabBar(selected = tab, onSelect = {
                    if (it == TasTab.Settings) settingsPage = null
                    tab = it
                })
            }
        }
        ToastHost(toast, topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    }
}
