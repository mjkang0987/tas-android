package kr.co.takeaseat.app.domain

// 웹 `features/reservations/model.ts` hasCompletedPayment·RESERVATION_STATUS_BADGE_STYLES,
// `reservationDetailUtils.ts` getPaymentEntries 이식.

fun hasCompletedPayment(reservation: Reservation): Boolean =
    !reservation.paymentEntries.isNullOrEmpty() || reservation.paymentCompleted == true

/** 결제 줄. 예전 데이터는 줄 없이 결제수단 하나만 있어 그 수단 + 가격 한 줄로 본다. */
fun paymentEntriesOf(reservation: Reservation): List<PaymentEntry> = when {
    !reservation.paymentEntries.isNullOrEmpty() -> reservation.paymentEntries
    reservation.paymentCompleted == true && !reservation.paymentMethod.isNullOrEmpty() ->
        listOf(PaymentEntry(reservation.paymentMethod, reservation.price ?: 0))
    else -> emptyList()
}

/** 배지 색(배경, 글자). 웹 값 그대로 — rgba 는 CSS "#RRGGBBAA" 로 옮겼다. 모르는 종류는 웹 기본 회색. */
data class BadgeColors(val bg: String, val color: String)

val RESERVATION_STATUS_BADGE_STYLES: Map<String, BadgeColors> = mapOf(
    "booked" to BadgeColors("#E8F0FE", "#4285F4"),
    "cancelled" to BadgeColors("#F1F1F1", "#999999"),
    "completed" to BadgeColors("#E6F4EA", "#34A853"),
    "noshow" to BadgeColors("#FCE8E6", "#EA4335"),
    "paid" to BadgeColors("#6526D914", "#6526d9"), // rgba(101, 38, 217, 0.08) — CSS #RRGGBBAA
    "unpaid" to BadgeColors("#FEF3C7", "#92400E"),
    "requested" to BadgeColors("#FEF7E0", "#a88417"),
)

fun statusBadgeColors(type: String): BadgeColors = RESERVATION_STATUS_BADGE_STYLES[type] ?: BadgeColors("#F1F1F1", "#999999")
