package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowedTweetDayPolicyTest {
    @Test fun followedTweetIsSkippedForTheRestOfThatDayOnly() {
        val today = FollowedTweetDayPolicy.add(emptyList(), "2026-10-09", "status-1")
        assertTrue("status-1" in FollowedTweetDayPolicy.keysFor(today, "2026-10-09"))
        assertFalse("status-1" in FollowedTweetDayPolicy.keysFor(today, "2026-10-10"))
    }

    @Test fun aSecondFollowDoesNotDuplicateTheSameTweet() {
        val once = FollowedTweetDayPolicy.add(emptyList(), "2026-10-09", "status-1")
        val twice = FollowedTweetDayPolicy.add(once, "2026-10-09", "status-1")
        assertEquals(listOf("2026-10-09\tstatus-1"), twice)
    }

    @Test fun yesterdayIsDroppedWhenTodayIsRecorded() {
        val yesterday = listOf("2026-10-08\told")
        val today = FollowedTweetDayPolicy.add(yesterday, "2026-10-09", "status-2")
        assertEquals(setOf("status-2"), FollowedTweetDayPolicy.keysFor(today, "2026-10-09"))
        assertTrue(FollowedTweetDayPolicy.keysFor(today, "2026-10-08").isEmpty())
    }
}
