package kr.co.takeaseat.app.domain

// 웹 `store/calendarStoreAssigneeHelpers.ts`(추가·고치기·요일·삭제) + `features/assignees/model.ts`(summarizeSchedule·
// splitAssigneesByStatus) 이식. 웹처럼 고치는 즉시 저장한다(저장 버튼 없음).

/** 웹 ASSIGNEE_STATUS_OPTIONS. */
val ASSIGNEE_STATUSES = listOf("재직", "휴직", "퇴직")

/** 웹 buildAddedAssigneeState — 이름이 비면 그대로. 번호는 가장 큰 번호 + 1(서버 Int 칸), 색은 안 고르면 번호의 기본 색. 근무는 기본(평일 10~20). */
fun addAssignee(snapshot: LocalDbSnapshot, name: String, status: String, phone: String, note: String, color: String?): LocalDbSnapshot {
    val clean = name.trim()
    if (clean.isEmpty()) return snapshot
    val id = (snapshot.assignees.maxOfOrNull { it.id } ?: 0) + 1
    val added = Assignee(id = id, name = clean, status = status, phone = phone.trim(), note = note.trim())
    return snapshot.copy(assignees = snapshot.assignees + added.copy(color = color?.ifEmpty { null } ?: assigneeColor(added)))
}

/** 웹 updateAssignee — 이름·상태·연락처·메모·색·다국어를 고친다(바뀐 칸만 [change] 로). */
fun updateAssignee(snapshot: LocalDbSnapshot, id: Int, change: (Assignee) -> Assignee): LocalDbSnapshot =
    snapshot.copy(assignees = snapshot.assignees.map { if (it.id == id) change(it) else it })

/** 웹 updateAssigneeDay — 0=월 … 6=일 하루의 근무·시간. 범위 밖이면 그대로. */
fun updateAssigneeDay(snapshot: LocalDbSnapshot, id: Int, dayIndex: Int, change: (DaySchedule) -> DaySchedule): LocalDbSnapshot {
    if (dayIndex !in 0..6) return snapshot
    return updateAssignee(snapshot, id) { a -> a.copy(schedule = a.schedule.mapIndexed { i, d -> if (i == dayIndex) change(d) else d }) }
}

/** 웹 deleteAssignee(영구 삭제) — 담당자를 지우고 그 예약은 남기되 미지정으로. */
fun deleteAssignee(snapshot: LocalDbSnapshot, id: Int): LocalDbSnapshot = snapshot.copy(
    assignees = snapshot.assignees.filterNot { it.id == id },
    reservations = snapshot.reservations.map { if (it.assigneeId == id) it.copy(assigneeId = null) else it },
)

/** 웹 setNameI18n — 언어 칸 하나를 바꾼다. 빈 칸은 빼고 다 비면 null. */
fun withNameI18n(current: NameI18n?, code: String, value: String): NameI18n? {
    val v = value.takeIf { it.isNotBlank() }
    val next = when (code) {
        "en" -> (current ?: NameI18n()).copy(en = v)
        "ja" -> (current ?: NameI18n()).copy(ja = v)
        else -> (current ?: NameI18n()).copy(zh = v)
    }
    return if (next.en == null && next.ja == null && next.zh == null) null else next
}

/** 웹 splitAssigneesByStatus — 정렬해서 재직 · 휴직 · 퇴직. */
fun splitAssigneesByStatus(assignees: List<Assignee>): Triple<List<Assignee>, List<Assignee>, List<Assignee>> {
    val sorted = sortAssignees(assignees)
    return Triple(
        sorted.filter { assigneeStatus(it) == "재직" },
        sorted.filter { assigneeStatus(it) == "휴직" },
        sorted.filter { assigneeStatus(it) == "퇴직" },
    )
}

private val WEEKDAY_LABELS = listOf("월", "화", "수", "목", "금", "토", "일")

/** 웹 summarizeSchedule — 이웃한 같은 근무끼리 묶는다. 예: (월~금, 10:00~20:00), (토, 10:00~18:00), (일, 휴무). */
fun summarizeSchedule(schedule: List<DaySchedule>): List<Pair<String, String>> {
    fun key(i: Int) = schedule.getOrNull(i)?.takeIf { it.enabled }?.let { "${it.start}~${it.end}" } ?: "휴무"
    val segments = mutableListOf<Pair<String, String>>()
    var start = 0
    for (i in WEEKDAY_LABELS.indices) {
        if (i < WEEKDAY_LABELS.lastIndex && key(i) == key(i + 1)) continue
        segments += (if (start == i) WEEKDAY_LABELS[i] else "${WEEKDAY_LABELS[start]}~${WEEKDAY_LABELS[i]}") to key(start)
        start = i + 1
    }
    return segments
}
