package kr.co.takeaseat.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 웹 `client/styles/globalStyle.ts` `:root` 토큰 이식. 값은 웹 표기(hsl·rgba)를 그대로 옮긴다 —
// 웹에서 바뀌면 여기도 같이 바꾼다. 쓰는 것만 옮기고, 화면이 늘 때 필요한 토큰을 더한다.
object TasColors {
    val Black = Color.hsl(0f, 0f, 0.13f)                       // --black-color
    val DarkGray2 = Color.hsl(0f, 0f, 0.43f)                   // --dark-gray-color2 (옅은 회색 바탕 4.8, 예전 45% 4.43)
    val White = Color.hsl(0f, 0f, 1f)                          // --white-color
    val White60 = Color.hsl(0f, 0f, 1f, alpha = 0.6f)          // --white-color-60
    val Brand = Color(0x65, 0x26, 0xD9)                        // --brand-color #6526d9
    val BrandBg = Color(101, 38, 217).copy(alpha = 0.08f)      // --brand-color-bg
    val BrandBorder = Color(101, 38, 217).copy(alpha = 0.2f)   // --brand-color-border
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
    val LightGray = Color.hsl(0f, 0f, 0.89f)                   // --light-gray-color
    val Info = Color(0x16, 0x60, 0xCC)                         // --info-color #1660cc (대비 4.5 이상, 예전 #2d7ff9)
    val InfoBg = Color(45, 127, 249).copy(alpha = 0.1f)        // --info-bg (배경은 그대로)
    val InfoBorder = Color(45, 127, 249).copy(alpha = 0.2f)    // --info-border
    val Link = Color(0x19, 0x67, 0xD2)                         // 상세 고객명 #1967D2 (대비 5.3, 예전 #4285F4 3.6)
    val Scrim = Color(15, 23, 42).copy(alpha = 0.4f)           // StyledOverlay 배경
    val ModalBorder = Color(148, 163, 184).copy(alpha = 0.22f) // --modal-border
    val ModalHeaderBorder = Color(148, 163, 184).copy(alpha = 0.18f) // --modal-header-border
    val BgSubtle92 = Color(248, 250, 252).copy(alpha = 0.92f)  // --bg-subtle-92
    val BgSubtle98 = Color(248, 250, 252).copy(alpha = 0.98f)  // --bg-subtle-98
    val Success = Color(0x24, 0x75, 0x3A)                      // --success-color #24753a
    val Ink = Color(0x11, 0x18, 0x27)                          // StyledModalMessage 기본 #111827
    val BgSubtle90 = Color(248, 250, 252).copy(alpha = 0.9f)   // 변경 화면 고객 메모 상자 rgba(248,250,252,.9)
    val Danger = Color(0xC9, 0x3A, 0x30)                       // --danger-color #c93a30
    val DangerBorder = Color(0xFE, 0xCA, 0xCA)                 // --danger-border #fecaca
    val DangerBg = Color(0xFE, 0xF2, 0xF2)                     // --danger-bg #fef2f2
    val PointPillBg = Color(0xF5, 0xEF, 0xE3)                  // 결제 '보유 적립금' 알약 #f5efe3 / #8a5a00 (5.2)
    val PointPillText = Color(0x8A, 0x5A, 0x00)
    val DepositPillBg = Color(0xE8, 0xF5, 0xE9)                // 결제 '네이버 예약금' 알약 #e8f5e9 / #2e7d32 (4.6)
    val DepositPillText = Color(0x2E, 0x7D, 0x32)
    val ExpectedPillBg = Color(0xE3, 0xF2, 0xFD)               // 결제 '실결제' 알약 #e3f2fd / #1565c0 (5.0)
    val ExpectedPillText = Color(0x15, 0x65, 0xC0)
    val Warning = Color(0xC5, 0x22, 0x1F)                      // --warning-color #C5221F (대비 5.8, 예전 #EA4335 3.9)
    val Caution = Color(0x80, 0x66, 0x00)                      // --caution-color #806600
    val Blue = Color.hsl(196f, 1f, 0.32f)                      // --blue-color (대비 5.0, 예전 45% 2.7)
    val Black10 = Color.hsl(0f, 0f, 0f, alpha = 0.03f)         // --black-color-10
    val Placeholder = Color(0x75, 0x75, 0x75)                  // 브라우저 기본 placeholder 실측 #757575
    val FocusRing = Color(101, 38, 217).copy(alpha = 0.12f)    // formControlStyle :focus 3px 링
    val BrandShadow = Color(101, 38, 217).copy(alpha = 0.28f)  // 모바일 '+ 예약' 그림자
}

object TasSizes {
    val LargeFont = 16.sp                                      // --large-font
    val MediumFont = 13.sp                                     // --medium-font
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
    val RadiusMd = RoundedCornerShape(6.dp)                    // --radius-md
    val SheetTop = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp) // --modal-radius-mobile, 바텀시트
    val Modal = RoundedCornerShape(10.dp)                      // --modal-radius, 넓은 화면 가운데 창
    val ConfirmMobile = RoundedCornerShape(12.dp)              // --modal-radius-mobile, 모바일 가운데 확인창
}
