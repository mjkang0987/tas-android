package kr.co.takeaseat.app.domain

// 웹 `address/CustomerAddModal.tsx`(handleSubmit) + calendarStore.addCustomer(게스트) 이식.

/** 웹 TEL_PATTERN — 휴대폰 번호(숫자만). */
internal val KOREAN_MOBILE_PHONE = Regex("^01[016789]\\d{7,8}$")

/** 웹 CustomerAddModal 검사 — 이름은 필수, 연락처는 비워도 되지만 넣으면 휴대폰 번호. 통과하면 null. */
fun validateCustomerAdd(name: String, tel: String): String? {
    if (name.trim().isEmpty()) return "고객명을 입력해 주세요."
    val digits = normalizeTel(tel)
    if (digits.isNotEmpty() && !KOREAN_MOBILE_PHONE.matches(digits)) return "올바른 연락처를 입력해 주세요."
    return null
}

/** 새 고객 — 번호는 최댓값 + 1(삭제·익명 고객도 세야 그 번호를 덮어쓰지 않는다), 적립금 0. */
fun newCustomer(customers: List<Customer>, name: String, tel: String) = Customer(
    id = (customers.maxOfOrNull { it.id } ?: 0).coerceAtLeast(0) + 1,
    name = name.trim(),
    tel = normalizeTel(tel),
    points = 0,
    pointHistories = emptyList(),
)

/** 검사를 통과한 이름·연락처로 고객을 더한다. */
fun addCustomer(snapshot: LocalDbSnapshot, name: String, tel: String): LocalDbSnapshot =
    snapshot.copy(customers = snapshot.customers + newCustomer(snapshot.customers, name, tel))
