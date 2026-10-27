package kr.co.takeaseat.app.domain

import kotlinx.serialization.Serializable

// 웹 tas `client/features/*/model.ts` 의 도메인 모델 이식. 키 이름·선택 여부를 웹과 같게 둔다 —
// 게스트 스냅샷(`LocalDbSnapshot`)은 나중에 로그인하면 그대로 `/api/migrate-local` 로 올라간다.
// 상태·결제수단 같은 문자열 열거는 String 으로 둔다. enum 으로 받으면 웹에 값이 하나 늘 때 파일 전체가 안 읽힌다.

object ReservationStatus {
    const val ACTIVE = "active"
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"
    const val NOSHOW = "noshow"
    const val REQUESTED = "requested"
}

@Serializable
data class PaymentEntry(val method: String, val amount: Int)

@Serializable
data class Reservation(
    val id: Int,
    val date: String,
    val startTime: String,
    val endTime: String,
    val service: String,
    val customerId: Int,
    val assigneeId: Int? = null,
    val status: String? = null,
    val price: Int? = null,
    val memo: String? = null,
    val paymentCompleted: Boolean? = null,
    val paymentMethod: String? = null,
    val paymentEntries: List<PaymentEntry>? = null,
    val pointEarned: Int? = null,
    val naverBookingId: String? = null,
    val naverBookingUrl: String? = null,
    val naverDeposit: Int? = null,
    val channel: String? = null,
)

@Serializable
data class ReservationHistoryEntry(
    val reservationId: Int,
    val before: Reservation,
    val after: Reservation,
    val timestamp: String,
)

@Serializable
data class PointHistoryEntry(
    val id: String,
    val type: String,
    val delta: Int,
    val balance: Int,
    val description: String,
    val createdAt: String,
    val relatedReservationId: Int? = null,
    /** 충전 때 받은 돈(보너스 붙은 적립 delta 와 다를 수 있다)·결제수단 — 매출로 잡힌다. 그 밖의 이력엔 없다(웹과 같다). */
    val paidAmount: Int? = null,
    val paymentMethod: String? = null,
)

@Serializable
data class CustomerMemoTag(val text: String, val color: String)

@Serializable
data class Customer(
    val id: Int,
    val name: String,
    val tel: String,
    val points: Int? = null,
    val firstVisitDate: String? = null,
    val pointHistories: List<PointHistoryEntry>? = null,
    val memoTags: List<CustomerMemoTag>? = null,
    val deleted: Boolean? = null,
)

@Serializable
data class NameI18n(val en: String? = null, val ja: String? = null, val zh: String? = null)

@Serializable
data class DaySchedule(val enabled: Boolean, val start: String, val end: String)

@Serializable
data class Assignee(
    val id: Int,
    val name: String,
    val nameI18n: NameI18n? = null,
    val schedule: List<DaySchedule> = createDefaultSchedule(),
    val status: String? = null,
    val phone: String? = null,
    val note: String? = null,
    val color: String? = null,
)

@Serializable
data class ServiceItem(
    val name: String,
    val durationMinutes: Int,
    val category: String,
    val price: Int,
    val nameI18n: NameI18n? = null,
)

@Serializable
data class StoreBusinessHours(val start: String = "10:00", val end: String = "20:00")

@Serializable
data class PointRechargeRule(val baseAmount: Int, val bonusAmount: Int)

@Serializable
data class PointSettings(
    val enableServiceRate: Boolean = false,
    val enableRecharge: Boolean = false,
    val serviceRate: Double = 0.0,
    val rechargeRules: List<PointRechargeRule> = listOf(PointRechargeRule(0, 0)),
)

// 기본값 = 웹 DEFAULT_STORE_SETTINGS. 키가 빠진 옛 스냅샷은 그 키만 기본값이 깔린다(웹 normalizeStoreSettings).
@Serializable
data class StoreSettings(
    val businessHours: StoreBusinessHours = StoreBusinessHours(),
    val closedDates: List<String> = emptyList(),
    /** 정기 휴무 요일. 0=월 … 6=일 */
    val closedWeekdays: List<Int> = emptyList(),
    val pointSettings: PointSettings = PointSettings(),
    /**
     * 지각 알림 기준(분) — 예약 시작 뒤 이만큼 지나도 완료·노쇼 처리가 안 되면 알린다. 0 = 끔, null = 기본 15분.
     * 앱에만 있는 매장 설정(지시자 2026-10-22 "매장에서 설정"). 웹·서버에는 아직 없다 — 서버 푸시 단계에서 같은 이름으로 넣는다.
     */
    val lateAlertMinutes: Int? = null,
)

/**
 * 웹 `features/local-db/storage.ts` `LocalDbSnapshot`.
 * 기본값은 **키가 없을 때** 웹 `loadLocalDbSnapshot` 이 까는 값이다(서비스=기본 카탈로그, 담당자=기본 1명,
 * onboarded=true). 처음 만드는 빈 스냅샷은 [emptyLocalDbSnapshot].
 */
@Serializable
data class LocalDbSnapshot(
    val customers: List<Customer> = emptyList(),
    val reservations: List<Reservation> = emptyList(),
    val history: List<ReservationHistoryEntry> = emptyList(),
    val services: List<ServiceItem> = SERVICE_CATALOG,
    val categoryBaseColors: Map<String, String> = CATEGORY_BASE_COLOR_MAP,
    val assignees: List<Assignee> = DEFAULT_ASSIGNEES,
    val storeSettings: StoreSettings = StoreSettings(),
    // 웹: onboarded 가 없는 기존 데이터는 이미 쓰던 것이라 true
    val onboarded: Boolean = true,
    val storeName: String? = null,
    val shopType: String? = null,
    val usePointSystem: Boolean? = null,
    val useMembershipSystem: Boolean? = null,
    val useCouponSystem: Boolean? = null,
    val useOnlineBooking: Boolean? = null,
)

/** 웹 `createDefaultLocalDbSnapshot` — 파일이 없을 때 새로 만드는 빈 매장. */
fun emptyLocalDbSnapshot(): LocalDbSnapshot = LocalDbSnapshot(
    services = emptyList(),
    categoryBaseColors = emptyMap(),
    assignees = emptyList(),
    onboarded = false,
)
