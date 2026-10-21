package kr.co.takeaseat.app.ui

import kr.co.takeaseat.app.ui.components.ToastState
import kr.co.takeaseat.app.ui.components.ToastType
import org.junit.Assert.assertEquals
import org.junit.Test

class ToastStateTest {
    @Test fun `알림 - 같은 문구·종류는 겹쳐 쌓지 않고, 종류가 다르면 쌓인다, 닫으면 빠진다`() {
        val s = ToastState()
        s.show("저장되었습니다.")
        s.show("저장되었습니다.")
        s.show("저장되었습니다.", ToastType.Error)
        assertEquals(listOf(ToastType.Success, ToastType.Error), s.toasts.map { it.type })
        s.dismiss(s.toasts.first().id)
        assertEquals(listOf(ToastType.Error), s.toasts.map { it.type })
    }
}
