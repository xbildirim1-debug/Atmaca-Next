package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class PublicProfileFlowTest {
    private fun n(text: String, x: Int, y: Int, w: Int = 120, selected: Boolean = false) =
        NodeSnapshot(text, null, null, "TextView", false, true,
            Rect().apply { left = x; top = y; right = x + w; bottom = y + 25 }, selected = selected)
    private fun profile(handle: String = "source") = listOf(
        n("@$handle", 10, 150), n("Contact @advertiser", 10, 200), n("Joined May 2020", 10, 250),
        n("1,204", 10, 300, 65), n("Following", 80, 300),
        n("52.4K", 230, 300, 65), n("Followers", 300, 300),
        n("@tweet_author", 10, 450))
    @Test fun publicProfileWithSplitCountersIsRecognizedWithoutEditProfile() {
        val nodes = profile()
        assertEquals(XScreen.PROFILE, ScreenDetector.detect(nodes))
        val evidence = ProfileSurfaceEvidence.read(nodes)!!
        assertEquals("source", evidence.handle)
        assertEquals("Followers", nodes[evidence.followersIndex].text)
        assertEquals("Following", nodes[evidence.followingIndex].text)
    }
    @Test fun combinedTurkishCountersAlsoUseTheSameEvidence() {
        val nodes = listOf(n("@dynamic", 10, 100), n("3.201 Takip ediliyor", 10, 300), n("81,2 B Takipçi", 250, 300))
        assertEquals("dynamic", ProfileSurfaceEvidence.read(nodes)?.handle)
        assertEquals(XScreen.PROFILE, ScreenDetector.detect(nodes))
    }
    @Test fun stackedCountersAndScaledScreensAreSupported() {
        val nodes = listOf(n("@dynamic", 10, 100), n("201", 20, 270), n("Following", 20, 300),
            n("8K", 250, 270), n("Followers", 250, 300))
        for (scale in listOf(1, 2, 3)) {
            val scaled = nodes.map { n -> n.copy(bounds = Rect().apply {
                left = n.bounds.left * scale; top = n.bounds.top * scale
                right = n.bounds.right * scale; bottom = n.bounds.bottom * scale
            }) }
            assertEquals("dynamic", ProfileSurfaceEvidence.read(scaled)?.handle)
        }
    }
    @Test fun listTabsAndTheirUsersAreNeverAPublicProfile() {
        val nodes = profile() + n("Followers", 10, 50, selected = true)
        assertNull(ProfileSurfaceEvidence.read(nodes))
        assertEquals(XScreen.FOLLOWERS_LIST, ScreenDetector.detect(nodes))
    }
    @Test fun distantNumbersAndHiddenCountersCannotQualify() {
        val missing = profile().filterNot { it.text == "52.4K" }
        assertNull(ProfileSurfaceEvidence.read(missing + n("52.4K", 230, 700)))
        assertNull(ProfileSurfaceEvidence.read(profile().map { if (it.text == "Followers") it.copy(visible = false) else it }))
    }
    @Test fun expectedNameInBioOrTweetCannotReplaceHeaderIdentity() {
        val nodes = profile("actual") + n("@expected", 10, 500)
        assertEquals("actual", ProfileSurfaceEvidence.read(nodes)?.handle)
    }
    @Test fun sourceToVerifiedListToLimitAndNextSourceContract() {
        val ownFollowers = listOf(n("Followers", 10, 50, selected = true), n("@changing", 10, 150))
        val chosen = RecentFollowerSelector.orderedHandles(ownFollowers, setOf("own")).first()
        assertNotNull(SourceProfileTarget.index(ownFollowers, chosen))
        val source = profile(chosen)
        assertEquals(XScreen.PROFILE, ScreenDetector.detect(source))
        assertEquals(chosen, ProfileSurfaceEvidence.read(source)?.handle)
        val known = listOf(n("Followers you know", 10, 50, selected = true), n("Verified followers", 200, 50))
        assertEquals(XScreen.UNKNOWN, ScreenDetector.detect(known))
        val verified = listOf(n("Verified followers", 10, 50, selected = true), n("@next", 10, 150))
        assertEquals(XScreen.VERIFIED_FOLLOWERS_LIST, ScreenDetector.detect(verified))
        assertTrue(VerifiedFollowPolicy.isPlainFollow(listOf("Follow")))
        assertFalse(VerifiedFollowPolicy.isPlainFollow(listOf("Follow back")))
        assertEquals(VerifiedFollowOutcome.SUCCESS, VerifiedFollowPolicy.outcome(true, true, false, 2000, 2500))
        val next = VerifiedFollowPolicy.nextSource(listOf("changing", "next"), "own", setOf(chosen))!!
        assertNotNull(SourceProfileTarget.index(verified, next))
        assertEquals(next, ProfileSurfaceEvidence.read(profile(next))?.handle)
        var streak = 0
        repeat(3) { streak = VerifiedFollowPolicy.nextStreak(streak, VerifiedFollowPolicy.outcome(true, false, true, 0, 900)) }
        assertTrue(VerifiedFollowPolicy.stopAccount(streak))
    }
}
