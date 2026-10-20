package kr.co.takeaseat.app.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CustomerPointsTest {
    private val now = Instant.parse("2026-10-20T05:00:00Z")
    private val snap = LocalDbSnapshot(customers = listOf(Customer(id = 3, name = "김", tel = "010", points = 1000)))

    @Test fun `충전 선택지 - 기준 + 보너스, 적립은 합, 받은 돈은 기준`() {
        val o = rechargeOptions(PointSettings(rechargeRules = listOf(PointRechargeRule(100000, 10000)))).single()
        assertEquals(RechargeOption("100,000원 + 10,000원", 110000, 100000), o)
    }

    @Test fun `충전 - 잔액에 더하고 이력에 받은 돈·결제수단, 0원은 그대로`() {
        val c = rechargePoints(snap, 3, 110000, 100000, "카드", custom = false, now = now).customers.single()
        assertEquals(111000, c.points)
        val h = c.pointHistories!!.single()
        assertEquals(listOf("recharge", "적립금 기준 충전", "카드"), listOf(h.type, h.description, h.paymentMethod))
        assertEquals(listOf(110000, 111000, 100000), listOf(h.delta, h.balance, h.paidAmount))
        assertEquals("2026-10-20T05:00:00Z", h.createdAt)
        assertEquals("적립금 직접 충전", rechargePoints(snap, 3, 5000, 5000, "현금", custom = true, now = now).customers.single().pointHistories!!.single().description)
        assertSame(snap, rechargePoints(snap, 3, 0, 0, "카드", custom = true, now = now))
        // 잔액이 Int 를 넘는 충전은 받지 않는다(음수로 뒤집히지 않게)
        val rich = snap.copy(customers = listOf(snap.customers.single().copy(points = Int.MAX_VALUE - 10)))
        assertSame(rich, rechargePoints(rich, 3, 11, 11, "카드", custom = true, now = now))
    }
}
