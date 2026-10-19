package kr.co.takeaseat.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class StripLineBreaksTest {
    @Test fun `줄바꿈은 빼고, 한 글자로 들어온 줄바꿈만 엔터`() {
        assertEquals("abc" to false, stripLineBreaks("ab", "abc"))
        assertEquals("abc" to true, stripLineBreaks("abc", "abc\n"))
        assertEquals("abc" to true, stripLineBreaks("abc", "ab\nc")) // 가운데 커서에서 엔터
        assertEquals("ab" to false, stripLineBreaks("", "a\r\nb")) // 붙여넣기
    }
}
