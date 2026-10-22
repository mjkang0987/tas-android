package kr.co.takeaseat.app.domain

import java.time.LocalDate
import java.time.format.DateTimeParseException

// 웹 `features/store-settings/labels.ts`(업종 목록) + `settings/StoreManageSection.tsx`(검사·휴업일 추가) 이식.

data class ShopIndustry(val value: String, val label: String, val emoji: String, val category: String)

/** 웹 CATEGORY_NAMES — 업종 고르기 묶음 이름(순서대로). */
val CATEGORY_NAMES: Map<String, String> = linkedMapOf(
    "beauty" to "뷰티", "food" to "음식", "medical" to "의료", "fitness" to "피트니스", "class" to "교육·클래스",
    "pet" to "반려", "repair" to "정비·수리", "space" to "공간대여", "counsel" to "상담", "etc" to "기타",
)

/** 웹 SHOP_INDUSTRIES — 온보딩·매장 관리·표시어(Labels.kt)의 단일 출처. 사람 진료(병원·치과·한의원)는 웹도 뺐다. */
val SHOP_INDUSTRIES: List<ShopIndustry> = listOf(
    ShopIndustry("hair", "헤어샵", "✂️", "beauty"),
    ShopIndustry("nail", "네일샵", "💅", "beauty"),
    ShopIndustry("waxing", "왁싱샵", "🪷", "beauty"),
    ShopIndustry("lash", "속눈썹샵", "👁️", "beauty"),
    ShopIndustry("skin", "피부관리실", "🧴", "beauty"),
    ShopIndustry("makeup", "메이크업", "💄", "beauty"),
    ShopIndustry("tattoo", "반영구·타투", "🖋️", "beauty"),
    ShopIndustry("restaurant", "음식점", "🍽️", "food"),
    ShopIndustry("cafe", "카페", "☕", "food"),
    ShopIndustry("bar", "주점·바", "🍺", "food"),
    ShopIndustry("vet", "동물병원", "🐾", "medical"),
    ShopIndustry("gym", "헬스·PT", "💪", "fitness"),
    ShopIndustry("yoga", "요가·필라테스", "🧘", "fitness"),
    ShopIndustry("golf", "골프", "⛳", "fitness"),
    ShopIndustry("dance", "댄스", "🕺", "fitness"),
    ShopIndustry("academy", "학원", "📚", "class"),
    ShopIndustry("craft", "공방·클래스", "🎨", "class"),
    ShopIndustry("tutoring", "과외", "✏️", "class"),
    ShopIndustry("petgroom", "애견미용", "🐩", "pet"),
    ShopIndustry("carwash", "세차", "🚗", "repair"),
    ShopIndustry("repair", "정비·수리", "🔧", "repair"),
    ShopIndustry("rentalspace", "공간대여", "🏢", "space"),
    ShopIndustry("practice", "연습실", "🎵", "space"),
    ShopIndustry("counseling", "상담", "💬", "counsel"),
    ShopIndustry("etc", "기타", "🏪", "etc"),
)

/** 웹 getPrimaryIndustry — shopType 은 콤마로 이은 여러 업종일 수 있다. 첫 번째로 아는 업종. */
fun primaryIndustry(shopType: String?): ShopIndustry? =
    shopType?.split(',')?.map { it.trim() }?.firstNotNullOfOrNull { v -> SHOP_INDUSTRIES.firstOrNull { it.value == v } }

/** 웹 handleSaveStoreInfo — 이름 공백 제거, 업종 '선택 안 함'(빈 값)은 null. 이름이 비면 오류 문구. */
fun saveStoreInfo(snapshot: LocalDbSnapshot, name: String, shopType: String): Pair<LocalDbSnapshot, String?> {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return snapshot to "매장 이름을 입력해 주세요."
    return snapshot.copy(storeName = trimmed, shopType = shopType.ifEmpty { null }) to null
}

/** 웹 handleAddClosedDate — 고른 날을 넣어 날짜순으로. 안 골랐거나 이미 있으면 오류 문구. */
fun addClosedDate(dates: List<String>, date: String): Pair<List<String>, String?> = when {
    date.isEmpty() -> dates to "휴업일을 선택해 주세요."
    date in dates -> dates to "이미 등록된 휴업일입니다."
    else -> (dates + date).sorted() to null
}

/** 요일 이름 — 0=월 … 6=일(`DayOfWeek.value - 1`). */
val WEEKDAYS = listOf("월", "화", "수", "목", "금", "토", "일")

/** 웹 정기 휴무 칸 이름 — 0=월 … 6=일. */
val CLOSED_WEEKDAY_LABELS: List<String> = WEEKDAYS

/** 웹 settings formatDateLabel — "2026년 10월 22일 (목)". 못 읽으면 받은 그대로. */
fun formatDateLabel(dateKey: String): String = try {
    val d = LocalDate.parse(dateKey)
    "${d.year}년 ${d.monthValue}월 ${d.dayOfMonth}일 (${WEEKDAYS[d.dayOfWeek.value - 1]})"
} catch (_: DateTimeParseException) {
    dateKey
}
