package kr.co.takeaseat.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 웹 `client/styles/globalStyle.ts` `:root` 토큰 이식. 값은 웹 표기(hsl·rgba)를 그대로 옮긴다 —
// 웹에서 바뀌면 여기도 같이 바꾼다. 쓰는 것만 옮기고, 화면이 늘 때 필요한 토큰을 더한다.
// 밝은/어두운 두 벌 — 시스템 테마를 따른다(TasTheme). 어두운 값은 웹 `prefers-color-scheme: dark`·iOS Theme.swift 와 같다
// (글자 7:1 이상, 배지 바탕이 카드 #1C1C1E 와 구분). 화면 코드는 `TasColors.X` 그대로 쓴다.
@Immutable
class TasPalette(
    /** 어두운 화면 벌인지 — 색 표(배지 등)를 고를 때. */
    val isDark: Boolean,
    val Black: Color,
    val DarkGray2: Color,
    val White: Color,
    val White60: Color,
    val Brand: Color,
    val BrandBg: Color,
    val BrandBorder: Color,
    val GlassBg: Color,
    val ShadowMd: Color,
    val ShadowSm: Color,
    val DarkGray: Color,
    val Gray: Color,
    val Gray2: Color,
    val Border: Color,
    val Orange: Color,
    val CalendarGridLine: Color,
    val NewCustomer: Color,
    val NewCustomerBg: Color,
    val WarningText: Color,
    val WarningBgSoft: Color,
    val WarningBorderSoft: Color,
    val ClosedDateBg: Color,
    val ClosedWeekdayBg: Color,
    val Icon: Color,
    val LightGray: Color,
    val Info: Color,
    val InfoBg: Color,
    val InfoBorder: Color,
    val Link: Color,
    val Scrim: Color,
    val ModalBorder: Color,
    val ModalHeaderBorder: Color,
    val BgSubtle92: Color,
    val BgSubtle98: Color,
    val NotificationUnreadBg: Color,
    val Success: Color,
    val Ink: Color,
    val BgSubtle90: Color,
    val Danger: Color,
    val DangerBorder: Color,
    val DangerBg: Color,
    val PointPillBg: Color,
    val PointPillText: Color,
    val DepositPillBg: Color,
    val DepositPillText: Color,
    val ExpectedPillBg: Color,
    val ExpectedPillText: Color,
    val Warning: Color,
    val Caution: Color,
    val Blue: Color,
    val Black10: Color,
    val Placeholder: Color,
    val FocusRing: Color,
    val BrandShadow: Color,
    val Surface: Color,
    val InverseBg: Color,
    val InverseText: Color,
    val BrandFill: Color,
    val BlueFill: Color,
    val DangerFill: Color,
    val NaverFill: Color,
    val NaverBorder: Color,
    val NeutralText: Color,
    val NeutralBg: Color,
    val NeutralBorder: Color,
    val SuccessText: Color,
    val SuccessBg: Color,
    val SuccessBorder: Color,
    val CardInk: Color,
    val Toast: Color,
    val KpiGradientEnd: Color,
    val AsideBg: Color,
    val AsideText: Color,
    val AsideHover: Color,
    val AsideDivider: Color,
    val HintBg: Color,
)

private fun hex(rgb: Long) = Color(0xFF000000 or rgb)

val LightPalette = TasPalette(
    isDark = false,
    Black = Color.hsl(0f, 0f, 0.13f), // --black-color
    DarkGray2 = Color.hsl(0f, 0f, 0.43f), // --dark-gray-color2 (옅은 회색 바탕 4.8, 예전 45% 4.43)
    White = Color.hsl(0f, 0f, 1f), // --white-color
    White60 = Color.hsl(0f, 0f, 1f, alpha = 0.6f), // --white-color-60
    Brand = Color(0x65, 0x26, 0xD9), // --brand-color #6526d9
    BrandBg = Color(101, 38, 217).copy(alpha = 0.08f), // --brand-color-bg
    BrandBorder = Color(101, 38, 217).copy(alpha = 0.2f), // --brand-color-border
    GlassBg = Color(255, 255, 255).copy(alpha = 0.72f), // --glass-bg
    ShadowMd = Color(0, 0, 0).copy(alpha = 0.12f), // --shadow-md 색
    ShadowSm = Color(0, 0, 0).copy(alpha = 0.08f), // --shadow-sm 색
    DarkGray = Color.hsl(0f, 0f, 0.32f), // --dark-gray-color
    Gray = Color.hsl(0f, 0f, 0.86f), // --gray-color
    Gray2 = Color.hsl(0f, 0f, 0.96f), // --gray-color2
    Border = Color.hsl(0f, 0f, 0.80f), // --border-color
    Orange = Color.hsl(27f, 1f, 0.5f), // --orange-color
    CalendarGridLine = Color.hsl(0f, 0f, 0.96f), // --calendar-grid-line
    NewCustomer = Color(0x00, 0xA8, 0x96), // --new-customer-color #00A896
    NewCustomerBg = Color(0, 168, 150).copy(alpha = 0.15f), // --new-customer-bg
    WarningText = Color(0x92, 0x40, 0x0E), // --warning-text #92400E
    WarningBgSoft = Color(0xFE, 0xF3, 0xC7), // --warning-bg-soft #FEF3C7
    WarningBorderSoft = Color(0xFC, 0xD3, 0x4D), // --warning-border-soft #FCD34D
    ClosedDateBg = Color(201, 58, 48).copy(alpha = 0.07f), // --closed-date-bg
    ClosedWeekdayBg = Color(0, 0, 0).copy(alpha = 0.05f), // --closed-weekday-bg
    Icon = Color(0x11, 0x18, 0x27), // DirectionIcon stroke #111827
    LightGray = Color.hsl(0f, 0f, 0.89f), // --light-gray-color
    Info = Color(0x16, 0x60, 0xCC), // --info-color #1660cc (대비 4.5 이상, 예전 #2d7ff9)
    InfoBg = Color(45, 127, 249).copy(alpha = 0.1f), // --info-bg (배경은 그대로)
    InfoBorder = Color(45, 127, 249).copy(alpha = 0.2f), // --info-border
    Link = Color(0x19, 0x67, 0xD2), // 상세 고객명 #1967D2 (대비 5.3, 예전 #4285F4 3.6)
    Scrim = Color(15, 23, 42).copy(alpha = 0.4f), // StyledOverlay 배경
    ModalBorder = Color(148, 163, 184).copy(alpha = 0.22f), // --modal-border
    ModalHeaderBorder = Color(148, 163, 184).copy(alpha = 0.18f), // --modal-header-border
    BgSubtle92 = Color(248, 250, 252).copy(alpha = 0.92f), // --bg-subtle-92
    BgSubtle98 = Color(248, 250, 252).copy(alpha = 0.98f), // --bg-subtle-98
    NotificationUnreadBg = Color(0xF0, 0xF8, 0xFF), // --notification-unread-bg
    Success = Color(0x24, 0x75, 0x3A), // --success-color #24753a
    Ink = Color(0x11, 0x18, 0x27), // StyledModalMessage 기본 #111827
    BgSubtle90 = Color(248, 250, 252).copy(alpha = 0.9f), // 변경 화면 고객 메모 상자 rgba(248,250,252,.9)
    Danger = Color(0xC9, 0x3A, 0x30), // --danger-color #c93a30
    DangerBorder = Color(0xFE, 0xCA, 0xCA), // --danger-border #fecaca
    DangerBg = Color(0xFE, 0xF2, 0xF2), // --danger-bg #fef2f2
    PointPillBg = Color(0xF5, 0xEF, 0xE3), // 결제 '보유 적립금' 알약 #f5efe3 / #8a5a00 (5.2)
    PointPillText = Color(0x8A, 0x5A, 0x00),
    DepositPillBg = Color(0xE8, 0xF5, 0xE9), // 결제 '네이버 예약금' 알약 #e8f5e9 / #2e7d32 (4.6)
    DepositPillText = Color(0x2E, 0x7D, 0x32),
    ExpectedPillBg = Color(0xE3, 0xF2, 0xFD), // 결제 '실결제' 알약 #e3f2fd / #1565c0 (5.0)
    ExpectedPillText = Color(0x15, 0x65, 0xC0),
    Warning = Color(0xC5, 0x22, 0x1F), // --warning-color #C5221F (대비 5.8, 예전 #EA4335 3.9)
    Caution = Color(0x80, 0x66, 0x00), // --caution-color #806600
    Blue = Color.hsl(196f, 1f, 0.32f), // --blue-color (대비 5.0, 예전 45% 2.7)
    Black10 = Color.hsl(0f, 0f, 0f, alpha = 0.03f), // --black-color-10
    Placeholder = Color(0x75, 0x75, 0x75), // 브라우저 기본 placeholder 실측 #757575
    FocusRing = Color(101, 38, 217).copy(alpha = 0.12f), // formControlStyle :focus 3px 링
    BrandShadow = Color(101, 38, 217).copy(alpha = 0.28f), // 모바일 '+ 예약' 그림자
    Surface = Color.hsl(0f, 0f, 1f), // 바탕(카드·시트·입력칸) — 예전 White 의 바탕 쓰임. White 는 이제 색 채움 위 흰 글자 전용
    InverseBg = Color.hsl(0f, 0f, 0.13f), // 검정 바탕(선택된 검정 칩·말풍선) — 어두운 화면에선 뒤집는다
    InverseText = Color.hsl(0f, 0f, 1f), // InverseBg 위 글자
    BrandFill = Color(0x65, 0x26, 0xD9), // 흰 글자를 얹는 보라 채움 — 모드 상관없이 같은 색(Brand 는 글자색이라 어두운 화면에서 밝아진다)
    BlueFill = Color.hsl(196f, 1f, 0.32f), // 흰 글자를 얹는 파랑 채움
    DangerFill = Color(0xC9, 0x3A, 0x30), // 흰 글자를 얹는 위험 채움
    NaverFill = Color(0x20, 0x82, 0x00), // --naver-color 흰 글자 바탕
    NaverBorder = Color(0x1A, 0x6B, 0x00), // --naver-color-dark
    NeutralText = Color(0x47, 0x55, 0x69), // --neutral-text
    NeutralBg = Color(241, 245, 249).copy(alpha = 0.92f), // --neutral-bg
    NeutralBorder = Color(203, 213, 225).copy(alpha = 0.95f), // --neutral-border
    SuccessText = Color(0x15, 0x80, 0x3D), // --success-text
    SuccessBg = Color(34, 197, 94).copy(alpha = 0.12f), // --success-bg
    SuccessBorder = Color(34, 197, 94).copy(alpha = 0.2f), // --success-border
    CardInk = Color(0xFF0F172A), // 예약 카드 시간·가격 #0f172a
    Toast = Color(0xFF1E293B), // --toast-bg(안내 알림 바탕)
    KpiGradientEnd = Color(0xF9, 0xFB, 0xFF), // 매출 숫자 카드 아래 #f9fbff
    AsideBg = Color(0xFF1C1C1E), // --aside-bg — 넓은 화면 왼쪽 메뉴(웹도 두 테마 같은 값)
    AsideText = Color(0xFFF5F5F7), // --aside-text
    AsideHover = Color.White.copy(alpha = 0.08f), // --aside-hover
    AsideDivider = Color.White.copy(alpha = 0.12f), // --aside-divider
    HintBg = Color(0xF6, 0xF8, 0xFC), // 매출 집계기준 설명 #f6f8fc
)

val DarkPalette = TasPalette(
    isDark = true,
    Black = hex(0xF5F5F7),
    DarkGray2 = hex(0xAEAEB2),
    White = Color.hsl(0f, 0f, 1f),
    White60 = Color(28, 28, 30).copy(alpha = 0.6f),
    Brand = hex(0xA57BFF),
    BrandBg = Color(165, 123, 255).copy(alpha = 0.16f),
    BrandBorder = Color(165, 123, 255).copy(alpha = 0.35f),
    GlassBg = Color(28, 28, 30).copy(alpha = 0.72f),
    ShadowMd = Color(0, 0, 0).copy(alpha = 0.6f),
    ShadowSm = Color(0, 0, 0).copy(alpha = 0.5f),
    DarkGray = hex(0xD1D1D6),
    Gray = hex(0x48484A),
    Gray2 = hex(0x2C2C2E),
    Border = hex(0x545458),
    Orange = Color.hsl(27f, 1f, 0.5f),
    CalendarGridLine = hex(0x2C2C2E),
    NewCustomer = hex(0x8EE6DA),
    NewCustomerBg = Color(0, 168, 150).copy(alpha = 0.25f),
    WarningText = hex(0xFDE68A),
    WarningBgSoft = Color(253, 230, 138).copy(alpha = 0.16f),
    WarningBorderSoft = Color(253, 230, 138).copy(alpha = 0.4f),
    ClosedDateBg = Color(255, 138, 128).copy(alpha = 0.1f),
    ClosedWeekdayBg = Color(255, 255, 255).copy(alpha = 0.05f),
    Icon = hex(0xF5F5F7),
    LightGray = hex(0x3A3A3C),
    Info = hex(0xAECBFA),
    InfoBg = Color(45, 127, 249).copy(alpha = 0.22f),
    InfoBorder = Color(45, 127, 249).copy(alpha = 0.4f),
    Link = hex(0xAECBFA),
    Scrim = Color(0, 0, 0).copy(alpha = 0.55f),
    ModalBorder = Color(255, 255, 255).copy(alpha = 0.12f),
    ModalHeaderBorder = Color(255, 255, 255).copy(alpha = 0.1f),
    BgSubtle92 = Color(44, 44, 46).copy(alpha = 0.92f),
    BgSubtle98 = Color(44, 44, 46).copy(alpha = 0.98f),
    NotificationUnreadBg = Color(45, 127, 249).copy(alpha = 0.14f),
    Success = hex(0xA8DAB5),
    Ink = hex(0xF5F5F7),
    BgSubtle90 = Color(44, 44, 46).copy(alpha = 0.9f),
    Danger = hex(0xFF8A80),
    DangerBorder = Color(255, 138, 128).copy(alpha = 0.4f),
    DangerBg = Color(201, 58, 48).copy(alpha = 0.22f),
    PointPillBg = Color(253, 230, 138).copy(alpha = 0.16f),
    PointPillText = hex(0xFDE68A),
    DepositPillBg = Color(34, 197, 94).copy(alpha = 0.2f),
    DepositPillText = hex(0xA8DAB5),
    ExpectedPillBg = Color(45, 127, 249).copy(alpha = 0.22f),
    ExpectedPillText = hex(0xAECBFA),
    Warning = hex(0xFFA19A),
    Caution = hex(0xF6D860),
    Blue = hex(0x7FD4F0),
    Black10 = Color(255, 255, 255).copy(alpha = 0.06f),
    Placeholder = hex(0x8E8E93),
    FocusRing = Color(165, 123, 255).copy(alpha = 0.25f),
    BrandShadow = Color(101, 38, 217).copy(alpha = 0.5f),
    Surface = hex(0x1C1C1E),
    InverseBg = hex(0xF5F5F7),
    InverseText = hex(0x1C1C1E),
    BrandFill = Color(0x65, 0x26, 0xD9),
    BlueFill = Color.hsl(196f, 1f, 0.32f),
    DangerFill = Color(0xC9, 0x3A, 0x30),
    NaverFill = Color(0x20, 0x82, 0x00),
    NaverBorder = Color(0x1A, 0x6B, 0x00),
    NeutralText = hex(0xC7CDD6),
    NeutralBg = Color(255, 255, 255).copy(alpha = 0.08f),
    NeutralBorder = Color(255, 255, 255).copy(alpha = 0.18f),
    SuccessText = hex(0xA8DAB5),
    SuccessBg = Color(34, 197, 94).copy(alpha = 0.2f),
    SuccessBorder = Color(34, 197, 94).copy(alpha = 0.35f),
    CardInk = hex(0xF5F5F7),
    Toast = hex(0x2C2C2E),
    KpiGradientEnd = hex(0x1C1C1E),
    AsideBg = Color(0xFF111113), // 어두운 화면 바탕(#1C1C1E)과 같으면 경계가 사라져 한 단계 어둡게
    AsideText = Color(0xFFF5F5F7),
    AsideHover = Color.White.copy(alpha = 0.08f),
    AsideDivider = Color.White.copy(alpha = 0.12f),
    HintBg = hex(0x2C2C2E),
)

val LocalTasPalette = staticCompositionLocalOf { LightPalette }

/** 지금 테마의 색 — `TasColors.Black` 처럼 쓴다(컴포저블 안에서만). */
val TasColors: TasPalette
    @Composable @ReadOnlyComposable get() = LocalTasPalette.current

/**
 * 사장님이 고른 색(서비스 카테고리 등)을 글자로 — 밝은 화면은 그대로, 어두운 화면은 흰색 쪽으로 55% 섞는다(iOS `tintText`·웹 `tintChip`).
 * [tintFill] 은 같은 색의 옅은 바탕 — 밝은 화면 9.4%(웹 `${color}18`), 어두운 화면 28%(카드와 구분).
 */
fun TasPalette.tintText(color: Color): Color = if (isDark) lerp(color, Color.White, 0.55f) else color
fun TasPalette.tintFill(color: Color): Color = color.copy(alpha = if (isDark) 0.28f else 0x18 / 255f)

/** `drawBehind` 안(컴포저블 밖)에서 지금 테마 색을 쓴다 — `Modifier.drawBehindThemed { colors -> drawRect(colors.LightGray) }`. */
@Composable
fun Modifier.drawBehindThemed(onDraw: DrawScope.(TasPalette) -> Unit): Modifier {
    val colors = TasColors
    return drawBehind { onDraw(colors) }
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
