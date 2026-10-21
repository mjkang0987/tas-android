package kr.co.takeaseat.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasSizes

/** 웹 ToastType. */
enum class ToastType { Success, Error, Info }

/** [shownAt] — 띄운 시각(ms). 알림이 다른 창으로 옮겨 그려져도 3초를 처음부터 다시 세지 않게. */
data class ToastItem(val id: Long, val message: String, val type: ToastType, val shownAt: Long)

/** 웹 useToastStore — 띄운 알림 목록. 같은 문구·종류가 떠 있으면 겹쳐 쌓지 않는다(한 동작이 같은 알림을 여러 번 부를 때). */
@Stable
class ToastState {
    val toasts = mutableStateListOf<ToastItem>()
    /**
     * 열린 상세 창(대화상자 창) — 연 순서. 알림은 맨 위 창에만 그린다. 앱 화면에 띄운 팝업은 그 뒤에 열린 창 아래로 깔려
     * 시트를 열면 알림이 가려졌다(웹은 z-index 9999 로 늘 맨 위).
     */
    internal val windows = mutableStateListOf<Any>()
    private var nextId = 0L
    fun show(message: String, type: ToastType = ToastType.Success) {
        if (toasts.any { it.message == message && it.type == type }) return
        toasts += ToastItem(nextId++, message, type, System.currentTimeMillis())
    }
    fun dismiss(id: Long) { toasts.removeAll { it.id == id } }
}

/** 화면 어디서나 `LocalToast.current.show(…)`. 앱 맨 위(TasApp)에서 넣는다. */
val LocalToast = staticCompositionLocalOf { ToastState() } // 앱 밖(미리보기)에선 아무 데도 안 그리는 빈 상태

/**
 * 웹 `ui/ToastContainer` — 헤더(48) 아래 50, 가운데, 화면 폭 − 32 안, 8 간격으로 쌓고 3초 뒤 사라진다. ✕ 로 바로 닫는다.
 * 앱 화면과 상세 창(DetailSheet)이 각자 [ToastHost] 를 두고, 맨 위 창의 것만 팝업으로 그린다(웹 z-index 9999 처럼 늘 위).
 * 초점은 빼앗지 않고 라이브 영역으로 바로 읽힌다(웹 role=alert).
 */
@Composable
fun ToastHost(state: ToastState, topInset: androidx.compose.ui.unit.Dp, window: Any? = null) {
    // 맨 위 창만 그린다 — 앱 화면(window = null)은 열린 상세 창이 없을 때만
    if (state.toasts.isEmpty() || state.windows.lastOrNull() != window) return
    val offsetY = with(LocalDensity.current) { (topInset + 48.dp + 50.dp).roundToPx() }
    val maxWidth = LocalConfiguration.current.screenWidthDp.dp - 32.dp
    Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, offsetY), properties = PopupProperties(focusable = false, clippingEnabled = false)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            state.toasts.forEach { toast ->
                key(toast.id) {
                    LaunchedEffect(Unit) { delay(maxOf(0L, 3000L - (System.currentTimeMillis() - toast.shownAt))); state.dismiss(toast.id) }
                    ToastRow(toast, maxWidth) { state.dismiss(toast.id) }
                }
            }
        }
    }
}

@Composable
private fun ToastRow(toast: ToastItem, maxWidth: androidx.compose.ui.unit.Dp, onDismiss: () -> Unit) {
    // 웹 slideIn — 위에서 8 내려오며 나타난다(0.18초)
    // 다른 창으로 옮겨 다시 그릴 땐 움직이지 않는다
    val appear = remember { Animatable(if (System.currentTimeMillis() - toast.shownAt < 200) 0f else 1f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(180)) }
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .widthIn(max = maxWidth)
            .graphicsLayer { alpha = appear.value; translationY = (1 - appear.value) * -8.dp.toPx() }
            .dropShadow(shape, Shadow(radius = TasSizes.ShadowMdBlur, color = TasColors.ShadowMd, offset = DpOffset(0.dp, TasSizes.ShadowMdOffsetY)))
            .clip(shape)
            .background(toast.type.bg)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Assertive }
            .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(toast.type.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(toast.message, color = Color.White, fontSize = TasSizes.MediumFont, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
        // 웹 ✕(16, 70% 흰색) — 누르는 자리 32
        Box(
            Modifier.size(32.dp).clickable(role = Role.Button, onClickLabel = "알림 닫기", onClick = onDismiss).semantics { contentDescription = "닫기" },
            contentAlignment = Alignment.Center,
        ) { Text("×", color = Color.White.copy(alpha = 0.7f), fontSize = TasSizes.LargeFont) }
    }
}

// 웹 typeStyles 색 · TOAST_ICON_PATHS(동그라미 + 표시, 선 2)
private val ToastType.bg: Color
    get() = when (this) {
        ToastType.Success -> Color(0xFF1A7F3C)
        ToastType.Error -> TasColors.Danger
        ToastType.Info -> Color(0xFF1E293B)
    }

private val ToastType.icon
    get() = when (this) {
        ToastType.Success -> SuccessIcon
        ToastType.Error -> ErrorIcon
        ToastType.Info -> InfoIcon
    }

private const val CIRCLE = "M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12Z"
private val SuccessIcon = svgLineIcon(24f, listOf(CIRCLE, "M8 12.5L10.5 15L16 9.5"), strokeWidth = 2f, name = "toast-success")
private val ErrorIcon = svgLineIcon(24f, listOf(CIRCLE, "M12 7.5V13M12 16.5H12.01"), strokeWidth = 2f, name = "toast-error")
private val InfoIcon = svgLineIcon(24f, listOf(CIRCLE, "M12 11V16.5M12 7.5H12.01"), strokeWidth = 2f, name = "toast-info")
