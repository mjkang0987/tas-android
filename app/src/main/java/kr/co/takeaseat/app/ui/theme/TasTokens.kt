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
    val ShadowSm = Color(0, 0, 0).copy(alpha = 0.08f)          // --shadow-sm 색
    val DarkGray = Color.hsl(0f, 0f, 0.32f)                    // --dark-gray-color
    val Gray = Color.hsl(0f, 0f, 0.86f)                        // --gray-color
    val Gray2 = Color.hsl(0f, 0f, 0.96f)                       // --gray-color2
    val Border = Color.hsl(0f, 0f, 0.80f)                      // --border-color
    val Orange = Color.hsl(27f, 1f, 0.5f)                      // --orange-color
    val CalendarGridLine = Color.hsl(0f, 0f, 0.96f)            // --calendar-grid-line
    val NewCustomer = Color(0x00, 0xA8, 0x96)                  // --new-customer-color #00A896
    val NewCustomerBg = Color(0, 168, 150).copy(alpha = 0.15f) // --new-customer-bg
    val WarningText = Color(0x92, 0x40, 0x0E)                  // --warning-text #92400E
    val WarningBgSoft = Color(0xFE, 0xF3, 0xC7)                // --warning-bg-soft #FEF3C7
    val WarningBorderSoft = Color(0xFC, 0xD3, 0x4D)            // --warning-border-soft #FCD34D
    val ClosedDateBg = Color(201, 58, 48).copy(alpha = 0.07f)  // --closed-date-bg
    val ClosedWeekdayBg = Color(0, 0, 0).copy(alpha = 0.05f)   // --closed-weekday-bg
    val Icon = Color(0x11, 0x18, 0x27)                         // DirectionIcon stroke #111827
}

object TasSizes {
    val LargeFont = 16.sp                                      // --large-font
    val SmallFont = 12.sp                                      // --small-font
    val XSmallFont = 11.sp                                     // --xsmall-font
    val TinyFont = 10.sp                                       // --tiny-font
    val TimelineCol = 44.dp                                    // --timeline-col
    val ShadowSmOffsetY = 1.dp                                 // --shadow-sm: 0 1px 4px
    val ShadowSmBlur = 4.dp
    val ShadowMdOffsetY = 4.dp                                 // --shadow-md: 0 4px 16px
    val ShadowMdBlur = 16.dp
}

object TasShapes {
    val Chip = RoundedCornerShape(percent = 50)                // --chip-radius: 999px
    val RadiusSm = RoundedCornerShape(4.dp)                    // --radius-sm
}
