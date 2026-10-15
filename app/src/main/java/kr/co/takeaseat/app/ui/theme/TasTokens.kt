package kr.co.takeaseat.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 웹 `client/styles/globalStyle.ts` `:root` 토큰 이식. 값은 웹 표기(hsl·rgba)를 그대로 옮긴다 —
// 웹에서 바뀌면 여기도 같이 바꾼다. 쓰는 것만 옮기고, 화면이 늘 때 필요한 토큰을 더한다.
object TasColors {
    val Black = Color.hsl(0f, 0f, 0.13f)                       // --black-color
    val DarkGray2 = Color.hsl(0f, 0f, 0.45f)                   // --dark-gray-color2
    val White = Color.hsl(0f, 0f, 1f)                          // --white-color
    val White60 = Color.hsl(0f, 0f, 1f, alpha = 0.6f)          // --white-color-60
    val Brand = Color(0x65, 0x26, 0xD9)                        // --brand-color #6526d9
    val BrandBg = Color(101, 38, 217).copy(alpha = 0.08f)      // --brand-color-bg
    val GlassBg = Color(255, 255, 255).copy(alpha = 0.72f)     // --glass-bg
    val ShadowMd = Color(0, 0, 0).copy(alpha = 0.12f)          // --shadow-md 색
}

object TasSizes {
    val TinyFont = 10.sp                                       // --tiny-font
    val ShadowMdOffsetY = 4.dp                                 // --shadow-md: 0 4px 16px
    val ShadowMdBlur = 16.dp
}

object TasShapes {
    val Chip = RoundedCornerShape(percent = 50)                // --chip-radius: 999px
}
