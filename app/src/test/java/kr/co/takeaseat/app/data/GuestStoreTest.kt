package kr.co.takeaseat.app.data

import java.io.File
import java.nio.file.Files
import kr.co.takeaseat.app.domain.DEFAULT_ASSIGNEES
import kr.co.takeaseat.app.domain.Reservation
import kr.co.takeaseat.app.domain.SERVICE_CATALOG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuestStoreTest {
    private fun tempFile(): File = File(Files.createTempDirectory("guest").toFile(), GuestStore.FILE_NAME)

    @Test fun `파일이 없으면 빈 매장으로 만들고 저장한다`() {
        val file = tempFile()
        val snapshot = GuestStore(file).snapshot.value
        assertTrue(file.exists())
        assertFalse(snapshot.onboarded)
        assertTrue(snapshot.services.isEmpty())
    }

    @Test fun `웹 localStorage JSON 을 그대로 읽는다 - 빠진 키는 웹 기본값, 모르는 키는 무시`() {
        val file = tempFile()
        // 웹에서 저장된 옛 형태 — onboarded·services·assignees·storeSettings 일부가 없다
        file.writeText(
            """{"customers":[{"id":1,"name":"김민지","tel":"01012345678","points":3000,"firstVisitDate":null}],
               "reservations":[{"id":5,"date":"2026-10-16","startTime":"14:00","endTime":"15:00","service":"커트","customerId":1,"status":"active","price":20000,"paymentEntries":[{"method":"카드","amount":20000}]}],
               "history":[],"storeSettings":{"businessHours":{"start":"09:00"}},"futureKey":{"x":1}}""",
        )
        val snapshot = GuestStore(file).snapshot.value
        assertTrue(snapshot.onboarded)
        assertEquals(SERVICE_CATALOG, snapshot.services)
        assertEquals(DEFAULT_ASSIGNEES, snapshot.assignees)
        assertEquals("09:00", snapshot.storeSettings.businessHours.start)
        assertEquals("20:00", snapshot.storeSettings.businessHours.end)
        assertEquals(20000, snapshot.reservations.single().paymentEntries!!.single().amount)
        assertEquals("김민지", snapshot.customers.single().name)
    }

    @Test fun `저장한 것을 다시 읽으면 같다`() {
        val file = tempFile()
        val store = GuestStore(file)
        val reservation = Reservation(1, "2026-10-17", "10:00", "10:30", "남성커트", customerId = 2)
        store.update { it.copy(reservations = listOf(reservation)) }
        assertEquals(listOf(reservation), GuestStore(file).snapshot.value.reservations)
        assertFalse(file.readText().contains("null")) // 없는 값은 키를 쓰지 않는다
    }

    @Test fun `망가진 파일은 빈 매장으로 시작하되 원본을 남긴다`() {
        val file = tempFile()
        file.writeText("{not json")
        val snapshot = GuestStore(file).snapshot.value
        assertTrue(snapshot.reservations.isEmpty())
        assertEquals("{not json", File(file.parentFile, "${file.name}.broken").readText())
    }
}
