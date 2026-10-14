package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class FollowerWindow26_84Test {
    @Test fun gainUsesTheCountFromBeforeTheWindow() {
        val points = listOf(1_000L to 100L, 5_000L to 104L)
        assertEquals(4L, FollowerWindow.gained(points, 2_000L))
        assertEquals(4L, FollowerWindow.gained(points, 0L))
        assertEquals(0L, FollowerWindow.gained(listOf(5_000L to 104L), 2_000L))
    }
}
