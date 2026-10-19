package kr.co.takeaseat.app.domain

// 웹 `features/store-settings/labels.ts` — 업종(shopType)에 따라 '담당자'·'서비스' 자리에 쓰는 말.

data class StoreLabels(val assignee: String, val service: String)

private val CATEGORY_LABELS = mapOf(
    "beauty" to StoreLabels("담당자", "서비스"),
    "food" to StoreLabels("테이블", "메뉴"),
    "medical" to StoreLabels("담당의", "진료"),
    "fitness" to StoreLabels("강사", "수업"),
    "class" to StoreLabels("선생님", "수업"),
    "pet" to StoreLabels("담당자", "서비스"),
    "repair" to StoreLabels("기사", "작업"),
    "space" to StoreLabels("룸", "이용"),
    "counsel" to StoreLabels("상담사", "상담"),
    "etc" to StoreLabels("담당자", "서비스"),
)

// 웹 SHOP_INDUSTRIES 의 value → category (표시 이름·이모지는 아직 쓰는 화면이 없어 옮기지 않았다)
private val INDUSTRY_CATEGORY = mapOf(
    "hair" to "beauty", "nail" to "beauty", "waxing" to "beauty", "lash" to "beauty", "skin" to "beauty",
    "makeup" to "beauty", "tattoo" to "beauty",
    "restaurant" to "food", "cafe" to "food", "bar" to "food",
    "vet" to "medical",
    "gym" to "fitness", "yoga" to "fitness", "golf" to "fitness", "dance" to "fitness",
    "academy" to "class", "craft" to "class", "tutoring" to "class",
    "petgroom" to "pet",
    "carwash" to "repair", "repair" to "repair",
    "rentalspace" to "space", "practice" to "space",
    "counseling" to "counsel",
    "etc" to "etc",
)

/** shopType 은 콤마로 이은 여러 업종일 수 있다 — 첫 번째로 아는 업종의 표시어. 모르면 기타. */
fun storeLabels(shopType: String?): StoreLabels {
    val category = shopType?.split(',')?.map { it.trim() }?.firstNotNullOfOrNull { INDUSTRY_CATEGORY[it] } ?: "etc"
    return CATEGORY_LABELS.getValue(category)
}
