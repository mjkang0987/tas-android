package kr.co.takeaseat.app.domain

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// 웹 `features/services/model.ts` 이식 — 기본 카탈로그·카테고리 색·서비스별 색·서비스 문자열 분해.

val SERVICE_CATALOG: List<ServiceItem> = listOf(
    ServiceItem("남성커트", 30, "커트", 18000),
    ServiceItem("여성커트", 30, "커트", 20000),
    ServiceItem("주니어커트", 30, "커트", 15000),
    ServiceItem("일반펌", 90, "펌", 50000),
    ServiceItem("디자인펌", 120, "펌", 70000),
    ServiceItem("디지털/셋팅", 150, "펌", 80000),
    ServiceItem("매직", 120, "펌", 100000),
    ServiceItem("매직셋팅", 180, "펌", 150000),
    ServiceItem("볼륨매직", 180, "펌", 150000),
    ServiceItem("뿌리염색", 90, "컬러", 45000),
    ServiceItem("전체염색", 90, "컬러", 65000),
    ServiceItem("탈색", 120, "컬러", 80000),
    ServiceItem("크리닉", 30, "크리닉", 30000),
    ServiceItem("크리닉 3단계", 60, "크리닉", 50000),
    ServiceItem("하오니코", 60, "크리닉", 100000),
    ServiceItem("남자 드라이", 20, "드라이", 18000),
    ServiceItem("여자 드라이", 30, "드라이", 20000),
    ServiceItem("기장추가", 0, "기타", 0),
    ServiceItem("숱추가", 0, "기타", 10000),
)

val CATEGORY_BASE_COLOR_MAP: Map<String, String> = linkedMapOf(
    "커트" to "#2D7FF9",
    "펌" to "#E53935",
    "컬러" to "#FB8C00",
    "크리닉" to "#00A896",
    "드라이" to "#6D6F78",
    "기타" to "#8E8E93",
)

// 하위호환: 기존 예약 데이터의 옛 서비스명 → 현재 카탈로그
private val LEGACY_NAME_MAP: Map<String, String> = linkedMapOf(
    "남자 일반펌" to "일반펌",
    "남자 디자인펌" to "디자인펌",
    "여자 일반펌" to "일반펌",
    "여자 디자인펌" to "디자인펌",
    "셋팅펌" to "디지털/셋팅",
    "남자 매직" to "매직",
    "여자 매직" to "매직",
    "다운펌+커트" to "디자인펌",
    "펌 롤" to "일반펌",
    "펌 매직" to "매직",
    "뿌리/전체(멋내기)" to "전체염색",
    "뿌리/전체(새치)" to "뿌리염색",
)

const val FALLBACK_SERVICE_COLOR = "#999"
private val SHADE_STEPS = intArrayOf(0, 14, -14, 26, -26, 36, -36, 46, -46)

private fun adjustHexColor(hex: String, delta: Int): String {
    val normalized = hex.removePrefix("#")
    if (normalized.length != 6) return FALLBACK_SERVICE_COLOR
    val channels = (0 until 3).map { normalized.substring(it * 2, it * 2 + 2).toIntOrNull(16) ?: return FALLBACK_SERVICE_COLOR }
    return "#" + channels.joinToString("") { "%02x".format((it + delta).coerceIn(0, 255)) }
}

private fun shadeDelta(index: Int): Int {
    val base = SHADE_STEPS[index % SHADE_STEPS.size]
    val cycle = index / SHADE_STEPS.size
    return base + cycle * 8 * (if (cycle % 2 == 0) 1 else -1)
}

private fun hslToHex(h: Int, s: Double, l: Double): String {
    val a = s * min(l, 1 - l)
    fun f(n: Int): String {
        val k = (n + h / 30.0) % 12
        val color = l - a * max(min(min(k - 3, 9 - k), 1.0), -1.0)
        // JS Math.round 와 같은 반올림(.5 는 올림)
        return "%02x".format(floor(255 * color + 0.5).toInt())
    }
    return "#${f(0)}${f(8)}${f(4)}"
}

private fun generateCategoryBaseColor(category: String): String {
    var hash = 0L
    for (ch in category) hash = (hash * 31 + ch.code) and 0xFFFFFFFFL // JS `>>> 0`
    return hslToHex((hash % 360).toInt(), 0.62, 0.5)
}

fun categoryBaseColor(category: String, categoryBaseColorMap: Map<String, String> = CATEGORY_BASE_COLOR_MAP): String =
    categoryBaseColorMap[category]?.takeIf { it.isNotEmpty() }
        ?: CATEGORY_BASE_COLOR_MAP[category]
        ?: generateCategoryBaseColor(category)

/** 카테고리 순서(처음 나온 순) 그대로 묶는다. */
fun groupedCatalog(catalog: List<ServiceItem>): Map<String, List<ServiceItem>> = catalog.groupBy { it.category }

/** 서비스 이름 → 색. 같은 카테고리 안에서 순서대로 밝기를 바꾼다. */
fun buildServiceColorMap(
    catalog: List<ServiceItem> = SERVICE_CATALOG,
    categoryBaseColorMap: Map<String, String> = CATEGORY_BASE_COLOR_MAP,
): Map<String, String> {
    val colorMap = linkedMapOf<String, String>()
    for ((category, items) in groupedCatalog(catalog)) {
        val baseColor = categoryBaseColor(category, categoryBaseColorMap)
        items.forEachIndexed { index, item -> colorMap[item.name] = adjustHexColor(baseColor, shadeDelta(index)) }
    }
    for ((legacy, current) in LEGACY_NAME_MAP) {
        val currentColor = colorMap[current]
        if (currentColor != null && legacy !in colorMap) colorMap[legacy] = currentColor
    }
    return colorMap
}

fun serviceColor(service: String, colorMap: Map<String, String>): String {
    colorMap[service]?.let { return it }
    // 이름이 긴 것부터 — 부분 일치가 짧은 이름에 먼저 걸리지 않게
    return colorMap.keys.sortedByDescending { it.length }.firstOrNull { service.contains(it) }
        ?.let { colorMap.getValue(it) }
        ?: FALLBACK_SERVICE_COLOR
}

/** "커트+크리닉" → ["커트", "크리닉"]. '+' 가 든 서비스명(knownNames 에 있으면)은 가장 긴 조합을 살린다. */
fun parseServiceString(str: String, knownNames: Set<String>? = null): List<String> {
    if (str.isBlank()) return emptyList()
    val parts = str.split('+').map { it.trim() }.filter { it.isNotEmpty() }
    if (knownNames == null || parts.size <= 1) return parts

    val result = mutableListOf<String>()
    var i = 0
    while (i < parts.size) {
        val j = (parts.size downTo i + 2).firstOrNull { parts.subList(i, it).joinToString("+") in knownNames }
        if (j != null) {
            result += parts.subList(i, j).joinToString("+")
            i = j
        } else {
            result += parts[i]
            i++
        }
    }
    return result
}

/** 웹 sumPrice — 서비스 문자열의 각 서비스 가격 합(카탈로그에 없는 이름은 0). 예약에 가격이 없을 때 쓴다. */
fun servicePriceTotal(service: String, catalog: List<ServiceItem>): Int {
    val byName = catalog.associateBy { it.name }
    return parseServiceString(service, byName.keys).sumOf { byName[it]?.price ?: 0 }
}

/** 웹 buildCatalogMap — 이름 → 서비스. 옛 이름도 현재 서비스로 찾힌다. */
fun buildCatalogMap(catalog: List<ServiceItem>): Map<String, ServiceItem> {
    val map = LinkedHashMap(catalog.associateBy { it.name })
    for ((legacy, current) in LEGACY_NAME_MAP) {
        val item = map[current]
        if (item != null && legacy !in map) map[legacy] = item
    }
    return map
}

fun sumDurationMinutes(names: List<String>, catalogMap: Map<String, ServiceItem>): Int = names.sumOf { catalogMap[it]?.durationMinutes ?: 0 }

fun sumPrice(names: List<String>, catalogMap: Map<String, ServiceItem>): Int = names.sumOf { catalogMap[it]?.price ?: 0 }

/** "HH:MM" + 분. 웹처럼 24시를 넘으면 0시부터 다시 돈다. */
fun calcEndTime(startTime: String, durationMinutes: Int): String {
    val total = toMinutes(startTime) + durationMinutes
    return "%02d:%02d".format(total / 60 % 24, total % 60)
}

/** 90 → "1시간30분", 30 → "30분", 0 이하 → "". */
fun formatDuration(minutes: Int): String {
    if (minutes <= 0) return ""
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}시간${m}분"
        h > 0 -> "${h}시간"
        else -> "${m}분"
    }
}
