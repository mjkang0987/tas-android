package kr.co.takeaseat.app.domain

import kotlinx.serialization.Serializable

// 웹 `overlays/CustomerDetail.tsx` 수정 모드(handleStartEdit·handleAddTag·handleRemoveTag·handleSaveEdit·commitEdit) 이식.
// 게스트라 병합(같은 번호 → '같은 분이에요')은 없다 — 번호가 겹치면 '다른 고객으로 저장'만.

/** 수정 중인 값 — 저장 전까지 고객에 반영하지 않는다(취소하면 버린다). 회전에도 살린다. */
@Serializable
data class CustomerEditForm(val name: String, val tel: String, val memoTags: List<CustomerMemoTag>) {
    companion object {
        fun of(customer: Customer) = CustomerEditForm(customer.name, customer.tel, customer.memoTags.orEmpty())
    }
}

/** 웹 handleAddTag — 공백 빼고, 빈 글자는 그대로, 같은 글자가 있으면 오류 문구. 성공하면 (새 폼, null). */
fun CustomerEditForm.addTag(text: String, color: String): Pair<CustomerEditForm, String?> {
    val value = text.trim()
    if (value.isEmpty()) return this to null
    if (memoTags.any { it.text == value }) return this to "같은 메모 태그가 이미 있습니다."
    return copy(memoTags = memoTags + CustomerMemoTag(value, color)) to null
}

fun CustomerEditForm.removeTag(text: String) = copy(memoTags = memoTags.filter { it.text != text })

/** 웹 handleSaveEdit 검사 — 수정은 연락처도 필수(추가와 다르다). 통과하면 null. */
fun validateCustomerEdit(form: CustomerEditForm): String? = when {
    form.name.trim().isEmpty() -> "고객명을 입력해 주세요."
    normalizeTel(form.tel).isEmpty() -> "연락처를 입력해 주세요."
    else -> null
}

/** 웹 telDup — 다른 고객이 같은 번호(숫자만 비교)를 쓰면 그 고객. 가족 공유번호가 있어 막지는 않고 확인만 한다. */
fun findTelDuplicate(customers: List<Customer>, customerId: Int, tel: String): Customer? {
    val digits = normalizeTel(tel)
    return customers.firstOrNull { it.id != customerId && it.tel.isNotEmpty() && normalizeTel(it.tel) == digits }
}

/** 웹 commitEdit — 이름 공백 제거·연락처 숫자만·메모 태그 통째로. */
fun applyCustomerEdit(snapshot: LocalDbSnapshot, customerId: Int, form: CustomerEditForm): LocalDbSnapshot =
    snapshot.copy(customers = snapshot.customers.map { c ->
        if (c.id == customerId) c.copy(name = form.name.trim(), tel = normalizeTel(form.tel), memoTags = form.memoTags) else c
    })

/** 웹 noshowCount — 상태가 노쇼인 예약 수. */
fun noshowCount(reservations: List<Reservation>) = reservations.count { it.status == ReservationStatus.NOSHOW }
