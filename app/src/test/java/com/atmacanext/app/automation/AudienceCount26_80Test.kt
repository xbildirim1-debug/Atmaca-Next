package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class AudienceCount26_80Test {
    @Test fun readsPlainGroupedAndShortCounts() {
        assertEquals(1234L, AudienceCount.parse("1.234"))
        assertEquals(43500L, AudienceCount.parse("43,5 B"))
        assertEquals(12400L, AudienceCount.parse("12.4K"))
        assertEquals(1_200_000L, AudienceCount.parse("1,2 Mn"))
        assertEquals(null, AudienceCount.parse("—"))
    }
}
