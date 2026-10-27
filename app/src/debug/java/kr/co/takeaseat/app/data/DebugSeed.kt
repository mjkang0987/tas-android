package kr.co.takeaseat.app.data

import java.time.LocalDate
import kr.co.takeaseat.app.domain.Assignee
import kr.co.takeaseat.app.domain.CATEGORY_BASE_COLOR_MAP
import kr.co.takeaseat.app.domain.Customer
import kr.co.takeaseat.app.domain.PaymentEntry
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.ReservationStatus
import kr.co.takeaseat.app.domain.SERVICE_CATALOG

/**
 * 디버그 빌드 전용 예시 데이터 — 예약 추가 화면이 아직 없어 타임라인을 확인할 방법이 없다.
 * 빈 매장일 때만 채운다. shortcut: 예약 추가 화면이 생기면 이 파일을 지운다.
 */
fun seedDebugData(store: GuestStore) {
    if (store.snapshot.value.onboarded || store.snapshot.value.reservations.isNotEmpty()) return
    val today = LocalDate.now().toString()
    val tomorrow = LocalDate.now().plusDays(1).toString()
    fun r(id: Int, date: String, start: String, end: String, service: String, customer: Int, assignee: Int?, status: String? = null, paid: Int? = null) =
        Reservation(
            id = id, date = date, startTime = start, endTime = end, service = service, customerId = customer, assigneeId = assignee,
            status = status, price = paid, paymentEntries = paid?.let { listOf(PaymentEntry("카드", it)) },
        )
    store.update {
        it.copy(
            onboarded = true,
            storeName = "미리보기 매장",
            services = SERVICE_CATALOG,
            categoryBaseColors = CATEGORY_BASE_COLOR_MAP,
            assignees = listOf(Assignee(id = 1, name = "지수", status = "재직"), Assignee(id = 2, name = "민호", status = "재직")),
            customers = listOf(
                Customer(1, "김민지", "01012345678"),
                Customer(2, "이서연", "01023456789"),
                Customer(3, "박준호", "01034567890"),
                Customer(4, "최유나", "01045678901"),
                Customer(5, "정하늘", "01056789012"),
                Customer(6, "한지민", "01067890123"),
            ),
            reservations = listOf(
                // 지난 방문이 있어 신규가 아닌 고객
                r(1, LocalDate.now().minusDays(20).toString(), "11:00", "11:30", "여성커트", 2, 1, paid = 20000),
                // 30분 = 세 줄, 겹침 2칸
                r(2, today, "10:00", "10:30", "남성커트", 1, 1, ReservationStatus.COMPLETED, 18000),
                r(3, today, "10:00", "11:30", "일반펌", 2, 2),
                // 20분 = 한 줄, 10분 = 이름만
                r(4, today, "11:30", "11:50", "남자 드라이", 3, 1),
                r(5, today, "12:00", "12:10", "기장추가", 4, null),
                // 3중 겹침
                r(6, today, "13:00", "15:00", "디자인펌+크리닉", 5, 1),
                r(7, today, "13:30", "14:30", "전체염색", 6, 2),
                r(8, today, "14:00", "15:00", "크리닉 3단계", 3, null),
                // 취소·노쇼·확정대기
                r(9, today, "16:00", "17:00", "여성커트", 4, 1, ReservationStatus.CANCELLED),
                r(10, today, "17:00", "18:00", "뿌리염색", 5, 2, ReservationStatus.NOSHOW),
                r(11, today, "18:00", "19:00", "매직", 6, 1, ReservationStatus.REQUESTED),
                r(12, tomorrow, "10:00", "12:00", "볼륨매직", 1, 2),
            ),
        )
    }
}
