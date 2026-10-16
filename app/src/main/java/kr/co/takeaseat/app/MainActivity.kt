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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
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
    Column(
        Modifier
            .fillMaxSize()
            .background(TasColors.White)
            .statusBarsPadding(),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                TasTab.Calendar -> CalendarScreen(store)
                else -> PlaceholderScreen(tab.label)
            }
        }
        MobileTabBar(selected = tab, onSelect = { tab = it })
    }
}

// shortcut: 탭 화면이 아직 없어 이름만 띄운다. 각 탭 화면을 옮길 때 이 자리를 그 화면으로 바꾼다.
@Composable
private fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, color = TasColors.DarkGray2)
    }
}
