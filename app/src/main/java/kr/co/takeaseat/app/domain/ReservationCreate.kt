package kr.co.takeaseat.app.domain

import java.time.LocalDateTime
import kotlinx.serialization.Serializable

// 웹 `components/calendar/overlays/useReservationCreateForm.ts` 의 상태·바꾸기 규칙·검사·저장을 순수 함수로 옮긴 것.
// 화면(ReservationCreateSheet)은 이걸 부르기만 한다 — 규칙은 여기서 단위 테스트한다.

private val KOREAN_MOBILE_PHONE = Regex("^01[016789]\\d{7,8}$")

/** 웹 roundToHalfHour — 15분 전은 내림, 45분부터 다음 정시. 23:45 이후는 23:30 (웹은 "24:00" 이 돼 시간 칸이 비었다). */
fun roundedStartTime(now: LocalDateTime): String {
    val m = now.minute
    val total = now.hour * 60 + when {
        m < 15 -> 0
        m < 45 -> 30
        else -> 60
    }
    val capped = minOf(total, 23 * 60 + 30)
    return "%02d:%02d".format(capped / 60, capped % 60)
}

/** 화면 회전에도 살아남게 직렬화할 수 있는 한 덩어리로 둔다. customerId 0 = 새 고객. */
@Serializable
data class ReservationForm(
    val customerId: Int = 0,
    val customerQuery: String = "",
    val customerTel: String = "",
    val selectedServices: List<String> = emptyList(),
    val price: Int = 0,
    val isPriceManual: Boolean = false,
    val assigneeId: Int = 0,
    val date: String,
    val startTime: String,
    val endTime: String,
    val isEndTimeManual: Boolean = false,
    val channel: String = "전화예약",
    val memo: String = "",
) {
    /** 추천에서 고객을 골랐다 — 이름·연락처를 채운다. */
    fun selectCustomer(customer: Customer) = copy(customerId = customer.id, customerQuery = customer.name, customerTel = customer.tel)

    /** 이름을 다시 쳤다 — 고른 고객이 있었으면 새 고객으로 바꾸고 채워 둔 연락처를 비운다. 이미 새 고객이면 친 연락처는 둔다. */
    fun changeCustomerQuery(value: String) =
        if (customerId != 0) copy(customerQuery = value, customerId = 0, customerTel = "") else copy(customerQuery = value)

    fun toggleService(name: String, catalogMap: Map<String, ServiceItem>): ReservationForm {
        val next = if (name in selectedServices) selectedServices - name else selectedServices + name
        val duration = sumDurationMinutes(next, catalogMap)
        return copy(
            selectedServices = next,
            endTime = if (duration > 0) calcEndTime(startTime, duration) else endTime,
            price = if (isPriceManual) price else sumPrice(next, catalogMap),
            isEndTimeManual = false,
        )
    }

    fun changeStartTime(value: String, catalogMap: Map<String, ServiceItem>): ReservationForm {
        val duration = sumDurationMinutes(selectedServices, catalogMap)
        val end = if (!isEndTimeManual && selectedServices.isNotEmpty() && duration > 0) calcEndTime(value, duration) else endTime
        return copy(startTime = value, endTime = end)
    }

    fun changeEndTime(value: String) = copy(endTime = value, isEndTimeManual = true)

    /** 숫자만 남긴다. 비우면 0. */
    fun changePrice(raw: String) = copy(price = raw.filter { it.isDigit() }.take(9).toIntOrNull() ?: 0, isPriceManual = true)

    companion object {
        fun initial(date: String, startTime: String, assignees: List<Assignee>) = ReservationForm(
            date = date,
            startTime = startTime,
            endTime = calcEndTime(startTime, 30),
            assigneeId = activeAssignees(assignees).firstOrNull()?.id ?: 0,
        )
    }
}

enum class CreateField { Customer, Service, Assignee, Date, Time }

data class CreateError(val field: CreateField, val message: String)

/** 웹 validate() — 같은 순서·같은 문구. */
fun validateCreate(form: ReservationForm, snapshot: LocalDbSnapshot, labels: StoreLabels): CreateError? {
    val isNewCustomer = form.customerId == 0
    if (activeAssignees(snapshot.assignees).isNotEmpty() && form.assigneeId == 0) {
        return CreateError(CreateField.Assignee, "${labels.assignee}를 선택해주세요.")
    }
    if (form.customerQuery.isBlank()) return CreateError(CreateField.Customer, "고객명을 입력해주세요.")
    if (isNewCustomer && form.customerTel.isBlank()) return CreateError(CreateField.Customer, "연락처를 입력해주세요.")
    if (isNewCustomer && !KOREAN_MOBILE_PHONE.matches(normalizeTel(form.customerTel))) {
        return CreateError(CreateField.Customer, "연락처 형식을 확인해주세요.")
    }
    if (form.selectedServices.isEmpty()) return CreateError(CreateField.Service, "${labels.service}를 선택해주세요.")
    if (form.date.isEmpty()) return CreateError(CreateField.Date, "날짜를 선택해주세요.")
    if (form.startTime.isEmpty()) return CreateError(CreateField.Time, "시작 시간을 입력해주세요.")
    if (form.endTime.isEmpty()) return CreateError(CreateField.Time, "종료 시간을 입력해주세요.")
    if (form.startTime >= form.endTime) return CreateError(CreateField.Time, "시작 시간은 종료 시간보다 앞서야 합니다.")

    when (val a = assigneeAvailability(snapshot.assignees, form.assigneeId, form.date, form.startTime, form.endTime)) {
        is AssigneeAvailability.OffDay -> return CreateError(CreateField.Date, a.message)
        is AssigneeAvailability.OutsideHours -> return CreateError(CreateField.Time, a.message)
        AssigneeAvailability.Available -> Unit
    }

    findOverlap(snapshot.reservations, form.date, form.startTime, form.endTime)?.let { overlap ->
        val name = snapshot.customers.firstOrNull { it.id == overlap.customerId }?.name ?: "-"
        return CreateError(CreateField.Time, "$name 예약(${overlap.startTime}~${overlap.endTime})과 시간이 겹칩니다.")
    }
    return null
}

/**
 * 웹 handleSave + addCustomer + addReservation(게스트) — 검사를 통과한 폼을 스냅샷에 넣는다.
 * 추천에서 안 골랐어도 이름·연락처가 같은 기존 고객이 있으면 그 고객에 붙인다(중복 고객 방지).
 * 새 번호는 최댓값 + 1 — 삭제(익명)된 고객도 세야 그 번호를 새 고객이 덮어쓰지 않는다.
 */
fun applyCreate(form: ReservationForm, snapshot: LocalDbSnapshot): LocalDbSnapshot {
    var customers = snapshot.customers
    val customerId = if (form.customerId != 0) {
        form.customerId
    } else {
        findExactCustomer(customers, form.customerQuery, form.customerTel)?.id ?: run {
            val id = (customers.maxOfOrNull { it.id } ?: 0).coerceAtLeast(0) + 1
            customers = customers + Customer(id = id, name = form.customerQuery.trim(), tel = normalizeTel(form.customerTel), points = 0, pointHistories = emptyList())
            id
        }
    }
    val reservation = Reservation(
        id = (snapshot.reservations.maxOfOrNull { it.id } ?: 0).coerceAtLeast(0) + 1,
        date = form.date,
        startTime = form.startTime,
        endTime = form.endTime,
        service = form.selectedServices.joinToString("+"),
        customerId = customerId,
        assigneeId = form.assigneeId.takeIf { it != 0 },
        status = ReservationStatus.ACTIVE,
        price = form.price,
        memo = form.memo.trim().takeIf { it.isNotEmpty() },
        channel = form.channel,
    )
    return snapshot.copy(customers = customers, reservations = snapshot.reservations + reservation)
}

/** 웹 예약경로 select 의 선택지. */
val RESERVATION_CHANNELS = listOf("전화예약", "현장방문", "네이버예약")
