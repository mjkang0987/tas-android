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

/** shopType 은 콤마로 이은 여러 업종일 수 있다 — 첫 번째로 아는 업종의 표시어. 모르면 기타. */
fun storeLabels(shopType: String?): StoreLabels {
    // 업종 목록은 StoreManage.kt SHOP_INDUSTRIES 하나 — 매장 관리 화면과 같은 출처
    return CATEGORY_LABELS.getValue(primaryIndustry(shopType)?.category ?: "etc")
}
