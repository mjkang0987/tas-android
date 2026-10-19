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

/** 웹: 네이버 예약이고 예약금이 있는데, 결제 줄의 '네이버 예약금' 합이 그 금액과 다를 때만 예약금 줄을 따로 보인다. */
fun showsNaverDepositLine(reservation: Reservation, entries: List<PaymentEntry>): Boolean {
    val deposit = reservation.naverDeposit ?: 0
    return !reservation.naverBookingId.isNullOrEmpty() && deposit > 0 &&
        entries.filter { it.method == "네이버 예약금" }.sumOf { it.amount } != deposit
}

/** 배지 색(배경, 글자). 웹 값 그대로 — rgba 는 CSS "#RRGGBBAA" 로 옮겼다. 모르는 종류는 웹 기본 회색. */
data class BadgeColors(val bg: String, val color: String)

// 글자색은 배경 위 대비 4.5:1 이상(WCAG AA) — 2026-10-19 웹·iOS 와 함께 진하게 바꿈(BadgeContrastTest 가 지킨다).
val RESERVATION_STATUS_BADGE_STYLES: Map<String, BadgeColors> = mapOf(
    "booked" to BadgeColors("#E8F0FE", "#1967D2"),
    "cancelled" to BadgeColors("#F1F1F1", "#666666"),
    "completed" to BadgeColors("#E6F4EA", "#137333"),
    "noshow" to BadgeColors("#FCE8E6", "#C5221F"),
    "paid" to BadgeColors("#6526D914", "#6526d9"), // rgba(101, 38, 217, 0.08) — CSS #RRGGBBAA
    "unpaid" to BadgeColors("#FEF3C7", "#92400E"),
    "requested" to BadgeColors("#FEF7E0", "#806600"),
)

fun statusBadgeColors(type: String): BadgeColors = RESERVATION_STATUS_BADGE_STYLES[type] ?: BadgeColors("#F1F1F1", "#999999")

/** 웹 pointRefundOnCancel — 취소·삭제 때 고객 적립금에 더할 값(결제에 쓴 적립금 반환 − 그 결제로 준 적립 회수). 이미 취소면 0. */
fun pointRefundOnCancel(reservation: Reservation): Int {
    if (reservation.status == ReservationStatus.CANCELLED) return 0
    if (!hasCompletedPayment(reservation)) return 0
    val used = reservation.paymentEntries.orEmpty().filter { it.method == "적립금" }.sumOf { it.amount }
    return used - (reservation.pointEarned ?: 0)
}
