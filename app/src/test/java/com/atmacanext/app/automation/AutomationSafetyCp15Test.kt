package com.atmacanext.app.automation

import org.junit.Assert.*
import org.junit.Test

class UnfollowSafetyGateCp15Test {
    @Test
    fun rejects_bio_and_requires_depth() {
        val bad = UnfollowSafetyGate.RowEvidence(
            handle = "@u",
            rowText = "@u • Doğum tarihi: 1990 • Daha fazla",
            buttonText = "Takip ediliyor",
            distinctHandleCountSeen = 100,
            stableEndObservations = 0,
        )
        assertFalse(UnfollowSafetyGate.mayClickFollowing(bad))

        val shallow = bad.copy(
            rowText = "@u User",
            distinctHandleCountSeen = 99,
        )
        assertFalse(UnfollowSafetyGate.mayClickFollowing(shallow))

        val good = shallow.copy(distinctHandleCountSeen = 100)
        assertTrue(UnfollowSafetyGate.mayClickFollowing(good))
    }

    @Test
    fun success_requires_same_handle_and_follow_state() {
        assertTrue(UnfollowSafetyGate.successVerified("@u", "@u", "Takip et"))
        assertFalse(UnfollowSafetyGate.successVerified("@u", "@x", "Takip et"))
        assertFalse(UnfollowSafetyGate.successVerified("@u", "@u", "Takip ediliyor"))
    }
}

class VerifiedFollowersPlannerCp15Test {
    @Test
    fun next_source_is_used_when_verified_list_is_empty() {
        val cursor = VerifiedSourceCursor(
            processedSourceHandles = setOf("@a"),
            currentSourceHandle = null,
            successfulFollows = 10,
            limit = 30,
        )
        val result = VerifiedFollowersPlanner.next(
            cursor = cursor,
            ownFollowersStable = true,
            candidateSourceHandles = listOf("@a", "@b", "@c"),
            sourceProfileVerified = false,
            sourceFollowersOpen = false,
            verifiedTabOpen = false,
            followableHandles = emptyList(),
            pendingVerificationHandle = null,
            pendingRowNowFollowing = null,
        )
        assertEquals(VerifiedDecision.OpenSource("@b"), result)
    }

    @Test
    fun completed_at_limit() {
        val result = VerifiedFollowersPlanner.next(
            cursor = VerifiedSourceCursor(successfulFollows = 30, limit = 30),
            ownFollowersStable = false,
            candidateSourceHandles = emptyList(),
            sourceProfileVerified = false,
            sourceFollowersOpen = false,
            verifiedTabOpen = false,
            followableHandles = emptyList(),
            pendingVerificationHandle = null,
            pendingRowNowFollowing = null,
        )
        assertEquals(VerifiedDecision.Completed, result)
    }
}
