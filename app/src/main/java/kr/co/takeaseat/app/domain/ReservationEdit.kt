package kr.co.takeaseat.app.domain

import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

// 웹 `ReservationDetail.tsx` 변경 모드 + `reservationDetailUtils.ts`(buildReservationFormState·buildDraftReservation·getChangedFields·
// buildSyncedPaidReservation) + `calendarStore.updateReservation`(게스트) 이식. 바꾸기 규칙은 예약 추가와 같은 [ReservationForm] 을 쓴다.

/**
 * 웹 buildReservationFormState — 예약을 폼으로. 서비스는 아는 이름(옛 이름 포함)으로 나눈다.
 * 가격이 없으면 [catalog](매장 카탈로그) 합 — 상세 화면과 같은 값(웹은 기본 카탈로그라 매장 가격을 바꾸면 화면마다 달랐다).
 */
fun ReservationForm.Companion.fromReservation(reservation: Reservation, knownServiceNames: Set<String>, catalog: List<ServiceItem>) = ReservationForm(
    customerId = reservation.customerId,
    selectedServices = parseServiceString(reservation.service, knownServiceNames),
    price = reservation.price ?: servicePriceTotal(reservation.service, catalog),
    assigneeId = reservation.assigneeId ?: 0,
    date = reservation.date,
    startTime = reservation.startTime,
    endTime = reservation.endTime,
    channel = reservation.channel ?: "전화예약",
    memo = reservation.memo ?: "",
)

/** 서비스 칸을 건드리지 않았으면 원래 문자열 그대로(나눴다 다시 이으면 띄어쓰기·모르는 이름이 바뀌어 '변경됨'으로 잡힌다). */
private fun serviceOf(form: ReservationForm, original: Reservation, knownServiceNames: Set<String>): String =
    if (form.selectedServices == parseServiceString(original.service, knownServiceNames)) original.service
    else form.selectedServices.joinToString("+")

/** 웹 buildDraftReservation(+ 예약경로 — 웹은 빠뜨려 저장이 안 됐다, 웹 PR #294). */
fun draftReservation(original: Reservation, form: ReservationForm, knownServiceNames: Set<String>): Reservation = original.copy(
    date = form.date,
    startTime = form.startTime,
    endTime = form.endTime,
    service = serviceOf(form, original, knownServiceNames),
    price = form.price,
    memo = form.memo.ifEmpty { null },
    channel = form.channel,
    assigneeId = form.assigneeId.takeIf { it != 0 },
)

data class ReservationDiff(val label: String, val before: String, val after: String)

/** 웹 getChangedFields — 차례: 서비스·담당자·날짜·시작시간·종료시간·가격·요청사항·예약경로. */
fun changedFields(
    original: Reservation,
    form: ReservationForm,
    assignees: List<Assignee>,
    labels: StoreLabels,
    knownServiceNames: Set<String>,
    catalog: List<ServiceItem>,
): List<ReservationDiff> {
    val draft = draftReservation(original, form, knownServiceNames)
    fun name(id: Int?) = id?.takeIf { it != 0 }?.let { a -> assignees.firstOrNull { it.id == a }?.name } ?: "미지정"
    val beforePrice = original.price ?: servicePriceTotal(original.service, catalog)
    return buildList {
        if (original.service != draft.service) add(ReservationDiff(labels.service, original.service, draft.service))
        if ((original.assigneeId ?: 0) != form.assigneeId) add(ReservationDiff(labels.assignee, name(original.assigneeId), name(form.assigneeId)))
        if (original.date != draft.date) add(ReservationDiff("날짜", original.date, draft.date))
        if (original.startTime != draft.startTime) add(ReservationDiff("시작시간", original.startTime, draft.startTime))
        if (original.endTime != draft.endTime) add(ReservationDiff("종료시간", original.endTime, draft.endTime))
        if (beforePrice != form.price) add(ReservationDiff("가격", formatPrice(beforePrice), formatPrice(form.price)))
        if ((original.memo ?: "") != form.memo) add(ReservationDiff("요청사항", original.memo ?: "", form.memo))
        // 경로가 없던 옛 예약은 전화예약으로 본다(상세·폼과 같게) — 빈 값과 비교하면 안 바꿔도 '변경'으로 잡혔다(웹도 고침).
        val beforeChannel = original.channel ?: "전화예약"
        if (beforeChannel != form.channel) add(ReservationDiff("예약경로", beforeChannel, form.channel))
    }
}

/** 웹 validateForm(변경) — 고객 칸이 없고, 겹침에서 자기 자신은 뺀다. */
fun validateEdit(original: Reservation, form: ReservationForm, snapshot: LocalDbSnapshot, labels: StoreLabels): FormError? {
    if (activeAssignees(snapshot.assignees).isNotEmpty() && form.assigneeId == 0) {
        return FormError(FormField.Assignee, "${labels.assignee}를 선택해주세요.")
    }
    if (form.selectedServices.isEmpty()) return FormError(FormField.Service, "${labels.service}를 선택해주세요.")
    if (form.date.isEmpty()) return FormError(FormField.Date, "날짜를 선택해주세요.")
    if (form.startTime.isEmpty()) return FormError(FormField.Time, "시작 시간을 입력해주세요.")
    if (form.endTime.isEmpty()) return FormError(FormField.Time, "종료 시간을 입력해주세요.")
    if (form.startTime >= form.endTime) return FormError(FormField.Time, "시작 시간은 종료 시간보다 앞서야 합니다.")
    when (val a = assigneeAvailability(snapshot.assignees, form.assigneeId, form.date, form.startTime, form.endTime)) {
        is AssigneeAvailability.OffDay -> return FormError(FormField.Date, a.message)
        is AssigneeAvailability.OutsideHours -> return FormError(FormField.Time, a.message)
        AssigneeAvailability.Available -> Unit
    }
    findOverlap(snapshot.reservations, form.date, form.startTime, form.endTime, excludeId = original.id)?.let { overlap ->
        val name = snapshot.customers.firstOrNull { it.id == overlap.customerId }?.name ?: "-"
        return FormError(FormField.Time, "$name 예약(${overlap.startTime}~${overlap.endTime})과 시간이 겹칩니다.")
    }
    return null
}

/** 웹 isPastTime — 바꾼 시작이 지금보다 앞이면 '과거' 확인 문구를 낸다. */
fun isPastStart(form: ReservationForm, now: LocalDateTime): Boolean = try {
    LocalDateTime.parse("${form.date}T${form.startTime}").isBefore(now)
} catch (_: DateTimeParseException) {
    false
}

/**
 * 변경 저장. 결제한 예약은 결제 줄이 하나면 그 줄 금액을 새 가격에 맞춘다(웹 buildSyncedPaidReservation — 여러 줄이면 그대로).
 * 이력 한 줄을 남긴다.
 */
fun applyEdit(snapshot: LocalDbSnapshot, original: Reservation, form: ReservationForm, knownServiceNames: Set<String>, now: Instant = Instant.now()): LocalDbSnapshot {
    val current = snapshot.reservations.firstOrNull { it.id == original.id } ?: return snapshot
    var next = draftReservation(current, form, knownServiceNames)
    if (hasCompletedPayment(current)) {
        val entries = paymentEntriesOf(current)
        if (entries.size == 1) {
            next = next.copy(
                paymentEntries = listOf(entries[0].copy(amount = next.price ?: 0)),
                paymentMethod = entries[0].method,
                paymentCompleted = true,
            )
        }
    }
    return snapshot.copy(
        reservations = snapshot.reservations.map { if (it.id == current.id) next else it },
        history = snapshot.history + ReservationHistoryEntry(current.id, current, next, now.toString()),
    )
}
