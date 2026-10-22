package kr.co.takeaseat.app.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PointManageTest {
    private val now = Instant.parse("2026-10-22T01:00:00Z")
    private val snap = LocalDbSnapshot(customers = listOf(
        Customer(1, "나", "010", points = 3000),
        Customer(2, "가", "011", points = 0),
        Customer(3, "다", "012", points = 9000),
        Customer(4, "삭제", "013", points = 500, deleted = true),
    ))

    @Test fun `목록 - 지운 고객 빼고 이름순, 잔액 있는 고객은 잔액 큰 순, 검색은 이름·연락처`() {
        assertEquals(listOf("가", "나", "다"), pointCustomers(snap.customers).map { it.name })
        assertEquals(listOf("다", "나"), customersWithPoints(snap.customers).map { it.name })
        assertEquals(listOf("가"), searchPointCustomers(pointCustomers(snap.customers), " 011 ").map { it.name })
        assertEquals(3, searchPointCustomers(pointCustomers(snap.customers), "  ").size)
    }

    @Test fun `수동 적립·차감 - 이력과 잔액, 차감은 실제로 뺀 만큼만, 뺄 게 없거나 0 이하는 그대로`() {
        val added = adjustPoints(snap, 1, 2000, add = true, now = now).customers.first { it.id == 1 }
        assertEquals(5000, added.points)
        assertEquals(listOf<Any>("manual_add", 2000, 5000, "수동 적립"), added.pointHistories!!.single().let { listOf(it.type, it.delta, it.balance, it.description) })
        // 웹은 -5000 · 잔액 -2000 으로 적었다
        val sub = adjustPoints(snap, 1, 5000, add = false, now = now).customers.first { it.id == 1 }
        assertEquals(0, sub.points)
        assertEquals(listOf<Any>("manual_subtract", -3000, 0, "수동 차감"), sub.pointHistories!!.single().let { listOf(it.type, it.delta, it.balance, it.description) })
        assertSame(snap, adjustPoints(snap, 2, 1000, add = false, now = now))
        assertSame(snap, adjustPoints(snap, 1, 0, add = true, now = now))
        assertSame(snap, adjustPoints(snap, 99, 1000, add = true, now = now))
        val rich = snap.copy(customers = listOf(Customer(1, "나", "010", points = Int.MAX_VALUE - 1)))
        assertSame(rich, adjustPoints(rich, 1, 2, add = true, now = now))
    }

    @Test fun `정책 칸 - 퍼센트 100 상한, 충전 기준 삭제는 하나 남으면 0, 추가 적립 퍼센트 반올림`() {
        assertEquals(100.0, parseServiceRate("150"), 0.0)
        assertEquals(5.0, parseServiceRate("5%"), 0.0)
        assertEquals(0.0, parseServiceRate(""), 0.0)
        assertEquals(listOf(PointRechargeRule(0, 0)), removeRechargeRule(listOf(PointRechargeRule(100, 10)), 0))
        assertEquals(listOf(PointRechargeRule(200, 30)), removeRechargeRule(listOf(PointRechargeRule(100, 10), PointRechargeRule(200, 30)), 0))
        assertEquals("15%", rechargeBonusPercent(PointRechargeRule(200, 30)))
        assertEquals("13%", rechargeBonusPercent(PointRechargeRule(1000, 125)))
        assertEquals("0%", rechargeBonusPercent(PointRechargeRule(0, 30)))
    }
}
