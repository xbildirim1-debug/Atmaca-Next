package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class ReplyTextPoolPolicyTest {
    @Test fun additionFollowsThePreparedLineAfterOneSpace() {
        assertEquals("hazır metin eklediğim", ReplyTextPoolPolicy.comment("hazır metin", "eklediğim"))
        assertEquals("yalnız hazır", ReplyTextPoolPolicy.comment("yalnız hazır", "  "))
        assertEquals("yalnız ek", ReplyTextPoolPolicy.comment("", "yalnız ek"))
    }

    @Test fun blankLinesAreDroppedAndTheCursorWraps() {
        assertEquals(listOf("bir", "iki"), ReplyTextPoolPolicy.lines("bir\n\niki\r\n"))
        assertEquals(0, ReplyTextPoolPolicy.slot(0, 3))
        assertEquals(1, ReplyTextPoolPolicy.slot(4, 3))
    }
}
