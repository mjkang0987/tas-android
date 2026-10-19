package kr.co.takeaseat.app.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// 웹 `reservationDetailUtils.ts` getHistoryDiffs·formatTimestamp + `ReservationHistoryLayer` 의 항목 종류 판정 이식.

/** 이력 한 줄의 종류 — 배지 글자·색과 카드 바탕이 갈린다. */
enum class HistoryKind(val label: String) { Cancelled("취소"), Noshow("노쇼"), Completed("예약완료"), Restored("예약전환"), Edit("변경") }

fun historyKind(entry: ReservationHistoryEntry): HistoryKind {
    val (b, a) = entry.before.status to entry.after.status
    return when {
        a == ReservationStatus.CANCELLED && b != ReservationStatus.CANCELLED -> HistoryKind.Cancelled
        a == ReservationStatus.NOSHOW && b != ReservationStatus.NOSHOW -> HistoryKind.Noshow
        a == ReservationStatus.COMPLETED && b != ReservationStatus.COMPLETED -> HistoryKind.Completed
        a == ReservationStatus.ACTIVE && (b == ReservationStatus.CANCELLED || b == ReservationStatus.NOSHOW) -> HistoryKind.Restored
        else -> HistoryKind.Edit
    }
}

private fun paymentLines(reservation: Reservation): String =
    paymentEntriesOf(reservation).takeIf { it.isNotEmpty() }?.joinToString(", ") { "${it.method} · ${formatPrice(it.amount)}" } ?: "미입력"

/**
 * 웹 getHistoryDiffs — 상태가 바뀐 줄은 상태 한 줄만. 아니면 결제상태 · 결제수단 · 칸별(서비스·담당자·날짜·시작시간·종료시간·가격·요청사항·예약경로).
 * 예약경로가 없던 옛 예약은 전화예약으로 본다(웹 #294 와 같다).
 */
fun historyDiffs(entry: ReservationHistoryEntry, assignees: List<Assignee>, labels: StoreLabels): List<ReservationDiff> {
    val (before, after) = entry.before to entry.after
    when (historyKind(entry)) {
        HistoryKind.Cancelled -> return listOf(ReservationDiff("상태", "예약", "취소"))
        HistoryKind.Noshow -> return listOf(ReservationDiff("상태", "예약", "노쇼"))
        HistoryKind.Completed -> return listOf(ReservationDiff("상태", "예약", "완료"))
        HistoryKind.Restored -> return listOf(ReservationDiff("상태", if (before.status == ReservationStatus.CANCELLED) "취소" else "노쇼", "예약"))
        HistoryKind.Edit -> Unit
    }
    fun name(id: Int?) = id?.takeIf { it != 0 }?.let { a -> assignees.firstOrNull { it.id == a }?.name } ?: "미지정"
    // 웹은 가격이 없으면 기본 카탈로그 합으로 본다
    fun price(r: Reservation) = r.price ?: servicePriceTotal(r.service, SERVICE_CATALOG)
    return buildList {
        if ((before.paymentCompleted ?: false) != (after.paymentCompleted ?: false)) {
            add(ReservationDiff("결제상태", if (before.paymentCompleted == true) "결제완료" else "미결제", if (after.paymentCompleted == true) "결제완료" else "미결제"))
        }
        val (bp, ap) = paymentLines(before) to paymentLines(after)
        if (bp != ap) add(ReservationDiff("결제수단", bp, ap))
        if (before.service != after.service) add(ReservationDiff(labels.service, before.service, after.service))
        if ((before.assigneeId ?: 0) != (after.assigneeId ?: 0)) add(ReservationDiff(labels.assignee, name(before.assigneeId), name(after.assigneeId)))
        if (before.date != after.date) add(ReservationDiff("날짜", before.date, after.date))
        if (before.startTime != after.startTime) add(ReservationDiff("시작시간", before.startTime, after.startTime))
        if (before.endTime != after.endTime) add(ReservationDiff("종료시간", before.endTime, after.endTime))
        if (price(before) != price(after)) add(ReservationDiff("가격", formatPrice(price(before)), formatPrice(price(after))))
        if ((before.memo ?: "") != (after.memo ?: "")) add(ReservationDiff("요청사항", before.memo ?: "", after.memo ?: ""))
        val (bc, ac) = (before.channel ?: "전화예약") to (after.channel ?: "전화예약")
        if (bc != ac) add(ReservationDiff("예약경로", bc, ac))
    }
}

private val TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/** 웹 formatTimestamp — 기기 시간대로 "2026-10-19 15:51". 못 읽으면 원문. */
fun formatHistoryTimestamp(iso: String, zone: ZoneId = ZoneId.systemDefault()): String = try {
    Instant.parse(iso).atZone(zone).format(TIMESTAMP)
} catch (_: DateTimeParseException) {
    iso
}
