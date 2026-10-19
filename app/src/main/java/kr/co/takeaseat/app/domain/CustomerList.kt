package kr.co.takeaseat.app.domain

// 웹 `pages/address.tsx`(정렬·검색·고객별 통계) · `features/customers/chosung.ts` · `AddressCustomerReservations`(상태 묶음) 이식.

private const val HANGUL_START = 0xAC00
private const val HANGUL_END = 0xD7A3
private const val JUNG_JONG = 588 // 중성 21 × 종성 28
private val CHOSUNG = listOf('ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ')
private val CHOSUNG_SET = CHOSUNG.toSet()

/** 한글 음절은 초성으로, 그 밖의 글자는 그대로("김민수" → "ㄱㅁㅅ"). */
fun chosungOf(text: String): String = text.map { ch ->
    if (ch.code in HANGUL_START..HANGUL_END) CHOSUNG[(ch.code - HANGUL_START) / JUNG_JONG] else ch
}.joinToString("")

/** 질의가 모두 초성 자모일 때만 초성열에 부분일치(웹 matchesChosung). */
fun matchesChosung(target: String, query: String): Boolean =
    query.isNotEmpty() && query.all { it in CHOSUNG_SET } && chosungOf(target).contains(query)

/** 고객 목록에서 예약 상태 — 지난 날짜의 진행 예약은 완료로 센다(웹 getEffectiveStatus). */
enum class CustomerReservationGroup(val label: String) { Booked("예약"), Completed("완료"), Cancelled("취소"), Noshow("노쇼") }

fun customerReservationGroup(r: Reservation, today: String): CustomerReservationGroup = when {
    r.status == ReservationStatus.CANCELLED -> CustomerReservationGroup.Cancelled
    r.status == ReservationStatus.NOSHOW -> CustomerReservationGroup.Noshow
    r.status == ReservationStatus.COMPLETED || r.date < today -> CustomerReservationGroup.Completed
    else -> CustomerReservationGroup.Booked
}

data class CustomerStats(val recentService: String?, val counts: Map<CustomerReservationGroup, Int>) {
    fun count(group: CustomerReservationGroup) = counts[group] ?: 0
}

/** 웹 customerStats — 최근 서비스(취소·노쇼 빼고 가장 늦은 예약)와 상태별 건수. */
fun customerStats(reservations: List<Reservation>, today: String): CustomerStats {
    val recent = reservations
        .filter { it.status != ReservationStatus.CANCELLED && it.status != ReservationStatus.NOSHOW }
        .maxWithOrNull(compareBy({ it.date }, { it.startTime }))
    return CustomerStats(recent?.service, reservations.groupingBy { customerReservationGroup(it, today) }.eachCount())
}

/** 고객 행을 펼쳤을 때 예약 묶음 — 예약·완료·취소·노쇼 차례, 묶음 안은 미래 → 과거. 빈 묶음은 뺀다. */
fun groupedCustomerReservations(reservations: List<Reservation>, today: String): List<Pair<CustomerReservationGroup, List<Reservation>>> {
    val byGroup = reservations.groupBy { customerReservationGroup(it, today) }
    return CustomerReservationGroup.entries.mapNotNull { g ->
        byGroup[g]?.sortedWith(compareByDescending<Reservation> { it.date }.thenByDescending { it.startTime })?.let { g to it }
    }
}

private val koCollator = java.text.Collator.getInstance(java.util.Locale.KOREAN)
private val enCollator = java.text.Collator.getInstance(java.util.Locale.ENGLISH)

/** 웹 sortedCustomerList — 한글로 시작하지 않는 이름이 먼저, 그다음 가나다. 삭제(익명) 고객은 뺀다. */
fun customerListOrder(customers: List<Customer>): List<Customer> {
    fun isKorean(name: String) = name.firstOrNull()?.code in HANGUL_START..HANGUL_END
    return customers.filter(::isSelectableCustomer).sortedWith { a, b ->
        val (ak, bk) = isKorean(a.name) to isKorean(b.name)
        if (ak != bk) (if (ak) 1 else -1) else (if (ak) koCollator else enCollator).compare(a.name, b.name)
    }
}

/** 검색 결과 한 줄 — 걸린 메모 태그는 접힌 행에 근거로 보인다. */
data class CustomerMatch(val customer: Customer, val matchedTags: List<CustomerMemoTag>)

/** 웹 filteredCustomers — 이름(대소문자 무시)·초성·연락처(하이픈 무시)·메모 태그. 빈 검색어면 전부. */
fun searchCustomers(ordered: List<Customer>, query: String): List<CustomerMatch> {
    val term = query.trim().lowercase()
    if (term.isEmpty()) return ordered.map { CustomerMatch(it, emptyList()) }
    val telTerm = term.replace("-", "")
    return ordered.mapNotNull { c ->
        val tags = c.memoTags.orEmpty().filter { it.text.lowercase().contains(term) }
        val hit = c.name.lowercase().contains(term) || matchesChosung(c.name, term) || c.tel.contains(telTerm) || tags.isNotEmpty()
        if (hit) CustomerMatch(c, tags) else null
    }
}
