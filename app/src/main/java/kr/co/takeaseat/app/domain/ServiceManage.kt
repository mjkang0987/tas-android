package kr.co.takeaseat.app.domain

import kotlinx.serialization.Serializable

// 웹 `settings/ServiceManageSection.tsx`(추가·수정 창 검사) + `store/calendarStoreServiceHelpers.ts`
// (추가·수정·삭제·카테고리 이름 바꾸기, 서비스 변경을 앞으로의 예약에 반영) 이식.

/** 추가 창에서 '+ 새 카테고리' 를 고른 값(웹 '__new'). */
const val NEW_CATEGORY = "__new"

/** 추가·수정 창 입력 — 친 그대로(숫자 칸도 글자), 회전에도 남게. */
@Serializable
data class ServiceForm(
    val category: String = "",
    val newCategory: String = "",
    val name: String = "",
    val nameEn: String = "",
    val nameJa: String = "",
    val nameZh: String = "",
    val duration: String = "",
    val price: String = "",
) {
    companion object {
        fun of(item: ServiceItem) = ServiceForm(
            category = item.category,
            name = item.name,
            nameEn = item.nameI18n?.en.orEmpty(),
            nameJa = item.nameI18n?.ja.orEmpty(),
            nameZh = item.nameI18n?.zh.orEmpty(),
            duration = item.durationMinutes.toString(),
            price = item.price.toString(),
        )
    }
}

/** 웹 buildNameI18n — 빈 칸은 빼고, 다 비면 null. */
fun buildNameI18n(en: String, ja: String, zh: String): NameI18n? {
    val e = en.trim().ifEmpty { null }
    val j = ja.trim().ifEmpty { null }
    val z = zh.trim().ifEmpty { null }
    return if (e == null && j == null && z == null) null else NameI18n(e, j, z)
}

/** 추가 창이 고른(또는 새로 친) 카테고리. */
fun ServiceForm.resolvedCategory(): String = if (category == NEW_CATEGORY) newCategory.trim() else category.trim()

/** 입력 → 서비스(웹 Number(x) || 0 — 숫자가 아니면 0). [category] 는 추가면 고른 것, 수정이면 원래 것. */
fun ServiceForm.toItem(category: String) = ServiceItem(
    name = name.trim(),
    durationMinutes = duration.toIntOrNull() ?: 0,
    category = category,
    price = price.toIntOrNull() ?: 0,
    nameI18n = buildNameI18n(nameEn, nameJa, nameZh),
)

/** 웹 ServiceAddModal.handleAdd 검사 — 카테고리 → 이름 → 같은 이름 순. 통과하면 null. */
fun validateServiceAdd(form: ServiceForm, catalog: List<ServiceItem>, serviceLabel: String): String? {
    if (form.resolvedCategory().isEmpty()) {
        return if (form.category == NEW_CATEGORY) "새 카테고리명을 입력해 주세요." else "카테고리를 선택하거나 새 카테고리를 추가해 주세요."
    }
    return validateServiceName(form.name.trim(), null, catalog, serviceLabel)
}

/** 웹 ServiceEditModal.handleSave 검사 — 자기 이름은 같아도 된다. */
fun validateServiceEdit(form: ServiceForm, originalName: String, catalog: List<ServiceItem>, serviceLabel: String): String? =
    validateServiceName(form.name.trim(), originalName, catalog, serviceLabel)

private fun validateServiceName(name: String, originalName: String?, catalog: List<ServiceItem>, label: String): String? = when {
    name.isEmpty() -> "${label}명을 입력해 주세요."
    catalog.any { it.name == name && it.name != originalName } -> "\"$name\" ${label}는 이미 등록되어 있습니다."
    else -> null
}

fun addService(snapshot: LocalDbSnapshot, item: ServiceItem) = snapshot.copy(services = snapshot.services + item)

fun deleteService(snapshot: LocalDbSnapshot, name: String) = snapshot.copy(services = snapshot.services.filterNot { it.name == name })

/** 수정 결과 — 함께 바뀐 예약 수(알림 '예정된 예약 n건에 반영'). */
data class ServiceUpdate(val snapshot: LocalDbSnapshot, val appliedCount: Int)

/**
 * 웹 updateService + buildServiceCatalogReservationUpdates — 서비스를 바꾸고, 오늘 이후 진행 중·미결제 예약 중 그 서비스를 쓰는 것의
 * 이름·가격·종료시간을 새 값으로. 가격이 옛 서비스 합과 다르면(손으로 바꿨으면) 가격, 길이가 다르면 종료시간은 그대로 둔다.
 */
fun updateService(snapshot: LocalDbSnapshot, originalName: String, updated: ServiceItem, today: String): ServiceUpdate {
    val prevCatalog = snapshot.services
    val nextCatalog = prevCatalog.map { if (it.name == originalName) updated else it }
    val oldMap = buildCatalogMap(prevCatalog)
    val newMap = buildCatalogMap(nextCatalog)
    val oldNames = prevCatalog.map { it.name }.toSet()
    var applied = 0
    val reservations = snapshot.reservations.map { r ->
        val active = (r.status ?: ReservationStatus.ACTIVE) == ReservationStatus.ACTIVE
        if (!active || r.date < today || hasCompletedPayment(r)) return@map r
        val names = parseServiceString(r.service, oldNames)
        if (originalName !in names) return@map r
        val priceManual = (r.price ?: 0) != sumPrice(names, oldMap)
        val durationManual = toMinutes(r.endTime) - toMinutes(r.startTime) != sumDurationMinutes(names, oldMap)
        val newNames = names.map { if (it == originalName) updated.name else it }
        val service = newNames.joinToString("+")
        val price = if (priceManual) r.price ?: 0 else sumPrice(newNames, newMap)
        val endTime = if (durationManual) r.endTime else calcEndTime(r.startTime, sumDurationMinutes(newNames, newMap))
        if (service == r.service && price == (r.price ?: 0) && endTime == r.endTime) return@map r
        applied++
        r.copy(service = service, price = price, endTime = endTime)
    }
    return ServiceUpdate(snapshot.copy(services = nextCatalog, reservations = reservations), applied)
}

/** 웹 saveCategoryEdit 검사. 통과하면 null. */
fun validateCategoryName(categories: Collection<String>, prev: String, next: String): String? {
    val name = next.trim()
    return when {
        name.isEmpty() -> "카테고리명을 입력해 주세요."
        categories.any { it == name && it != prev } -> "\"$name\" 카테고리는 이미 있습니다."
        else -> null
    }
}

/** 웹 buildRenamedCategoryState — 그 카테고리의 서비스와 대표색 열쇠를 새 이름으로. 같은 이름이면 그대로. */
fun renameCategory(snapshot: LocalDbSnapshot, prev: String, next: String): LocalDbSnapshot {
    val name = next.trim()
    if (name.isEmpty() || name == prev || snapshot.services.any { it.category == name }) return snapshot
    val colors = LinkedHashMap(snapshot.categoryBaseColors)
    colors.remove(prev)?.let { colors[name] = it }
    return snapshot.copy(
        services = snapshot.services.map { if (it.category == prev) it.copy(category = name) else it },
        categoryBaseColors = colors,
    )
}

/** 웹 updateCategoryBaseColor. */
fun setCategoryColor(snapshot: LocalDbSnapshot, category: String, color: String): LocalDbSnapshot =
    if (snapshot.categoryBaseColors[category] == color) snapshot
    else snapshot.copy(categoryBaseColors = snapshot.categoryBaseColors + (category to color))
