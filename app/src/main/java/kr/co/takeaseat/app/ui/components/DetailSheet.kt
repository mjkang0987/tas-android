package kr.co.takeaseat.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kr.co.takeaseat.app.ui.theme.TasColors
import kr.co.takeaseat.app.ui.theme.TasShapes

private val SheetEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f) // 웹 layer 애니메이션 곡선

/**
 * 웹 `ModalStyles` 모바일 바텀시트(StyledOverlay + StyledDetail + StyledHeader) 이식.
 * 시스템 대화상자 창에 띄운다 — 뒤로가기·화면 낭독기 초점 가두기를 플랫폼이 해 준다(배경 어둡게는 끄고 웹 색으로 직접 칠한다).
 * 바깥(어두운 곳)을 누르거나 닫기·뒤로가기로 닫힌다. [footer] 는 웹 StyledFooter 자리(버튼 줄).
 */
@Composable
fun DetailSheet(
    title: String,
    onDismiss: () -> Unit,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }

        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val scrimAlpha by animateFloatAsState(if (shown) 1f else 0f, tween(180), label = "scrim")

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(TasColors.Scrim.copy(alpha = TasColors.Scrim.alpha * scrimAlpha))
                // 가로 내비게이션 막대·노치 아래로 닫기 버튼이 들어가지 않게(가로 화면 3버튼 내비게이션)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            // 바깥을 누르면 닫는다. 시트와 **형제**로 둔다 — 부모에 clickable 을 걸면 시트 안 글자가 전부 그 하나로 합쳐져
            // 화면 낭독기가 행을 하나씩 읽지 못했다. clickable 대신 손가락 탭만 받는다 — clickable 이면 보이지 않는 화면 전체가
            // 키보드·낭독기 초점이 된다. 그쪽은 닫기 버튼·뒤로가기가 같은 일을 한다.
            Box(Modifier.matchParentSize().pointerInput(onDismiss) { detectTapGestures { onDismiss() } })
            // 웹: 640px 이하 바텀시트, 넘으면 가운데 창(폭 min(360, 90%), 높이 80%, 10 둥글게).
            val wide = maxWidth > 640.dp
            val shape = if (wide) TasShapes.Modal else TasShapes.SheetTop
            val riseFrom = with(LocalDensity.current) { (if (wide) 12 else 32).dp.roundToPx() }
            AnimatedVisibility(
                visible = shown,
                modifier = Modifier.align(if (wide) Alignment.Center else Alignment.BottomCenter),
                // 웹 sheetSlideUp/layerPopIn — 32px 아래에서 올라오며 0.6 → 1 로 진해진다.
                enter = slideInVertically(tween(if (wide) 220 else 260, easing = SheetEasing)) { riseFrom } +
                    fadeIn(tween(if (wide) 220 else 260, easing = SheetEasing), initialAlpha = 0.6f),
            ) {
                Column(
                    Modifier
                        .then(if (wide) Modifier.width(minOf(360.dp, maxWidth * 0.9f)) else Modifier.fillMaxWidth())
                        .heightIn(max = maxHeight * (if (wide) 0.8f else 0.9f))
                        // 시트 안을 누른 건 바깥 닫기로 새지 않게 여기서 받는다(동작 없음).
                        .pointerInput(Unit) { detectTapGestures { } }
                        .clip(shape)
                        // 웹은 뒤를 흐리게(backdrop blur) 한 위에 98% 흰색이라 뒤 글자가 안 읽힌다. 흐림이 없으니 바닥을 불투명 흰색으로 깐다.
                        .background(Color.White)
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.98f), TasColors.BgSubtle98)))
                        .border(1.dp, TasColors.ModalBorder, shape)
                        .semantics { paneTitle = title },
                ) {
                    SheetHeader(title, onDismiss)
                    Column(
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
                        content = content,
                    )
                    if (footer != null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .drawBehind { drawRect(TasColors.ModalHeaderBorder, size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx())) }
                                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
                        ) { footer() }
                    }
                    // 제스처 내비게이션 막대 아래로 내용이 들어가지 않게 — 시트 배경은 끝까지 깔린다.
                    if (!wide) Box(Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, onDismiss: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.88f), TasColors.BgSubtle92)))
            .drawBehind {
                val h = 1.dp.toPx()
                drawRect(TasColors.ModalHeaderBorder, topLeft = Offset(0f, size.height - h), size = androidx.compose.ui.geometry.Size(size.width, h))
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Color.Black,
            fontSize = 16.sp, // --modal-title-font
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.02f * 16).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        // 웹 CloseIconButton(모바일 34) — 흰 바탕, 연회색 테두리, 6 둥글게, X 14
        Box(
            Modifier
                .size(34.dp)
                .clip(TasShapes.RadiusMd)
                .background(Color.White)
                .border(1.dp, TasColors.LightGray, TasShapes.RadiusMd)
                .clickable(role = Role.Button, onClick = onDismiss)
                .semantics { contentDescription = "닫기" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(CloseIcon, contentDescription = null, tint = TasColors.DarkGray, modifier = Modifier.size(14.dp))
        }
    }
}

private val CloseIcon = svgLineIcon(24f, listOf("M7 7L17 17M17 7L7 17"), strokeWidth = 2f, name = "close")
