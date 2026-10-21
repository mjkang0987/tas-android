package kr.co.takeaseat.app

import android.graphics.Color
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kr.co.takeaseat.app.data.GuestStore
import kr.co.takeaseat.app.data.seedDebugData
import kr.co.takeaseat.app.ui.calendar.CalendarScreen
import kr.co.takeaseat.app.ui.customers.CustomersScreen
import kr.co.takeaseat.app.ui.revenue.RevenueScreen
import kr.co.takeaseat.app.ui.settings.SettingsScreen
import kr.co.takeaseat.app.ui.nav.MobileTabBar
import kr.co.takeaseat.app.ui.nav.TasTab
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 라이트 전용(웹에 다크 모드 없음) — 시스템 바 아이콘도 항상 어둡게.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        // shortcut: 첫 실행 때 메인 스레드에서 작은 JSON 파일 하나를 읽는다. 데이터가 커지면 백그라운드 로드로 바꾼다.
        val store = GuestStore.forApp(this).also(::seedDebugData)
        setContent { TasTheme { TasApp(store) } }
    }
}

@Composable
private fun TasApp(store: GuestStore) {
    var tab by rememberSaveable { mutableStateOf(TasTab.Calendar) }
    // 설정 탭 안의 하위 화면(서비스 관리 등) — 설정 탭을 누르면 늘 메뉴부터(웹 탭이 /menu 로 간다)
    var settingsPage by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(TasColors.White)
            .statusBarsPadding()
            // 키보드가 뜨면 화면을 키보드 위까지로 줄인다(웹 모바일처럼 탭바는 키보드 위에) — 아래쪽 입력칸(고객 메모 태그 등)이 가리지 않게.
            // 내비게이션 바 몫은 탭바가 이미 띄우므로 뺀다. 시트는 따로 창이라 각자 키보드를 피한다.
            .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                TasTab.Calendar -> CalendarScreen(store)
                TasTab.Customers -> CustomersScreen(store)
                TasTab.Revenue -> RevenueScreen(store)
                TasTab.Settings -> SettingsScreen(store, settingsPage, onPage = { settingsPage = it })
            }
        }
        MobileTabBar(selected = tab, onSelect = {
            if (it == TasTab.Settings) settingsPage = null
            tab = it
        })
    }
}
