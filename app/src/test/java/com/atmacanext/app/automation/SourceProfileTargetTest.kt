package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class SourceProfileTargetTest {
    private fun node(text: String, desc: String? = null, visible: Boolean = true, enabled: Boolean = true) =
        NodeSnapshot(text, desc, null, "TextView", false, enabled,
            Rect().apply { left = 30; top = 300; right = 230; bottom = 345 }, visible = visible)

    @Test fun opensUsernameBesideFollowingWithoutSelectingRelationshipButton() {
        val nodes = listOf(node("Takip ediliyor"), node("@dynamic_source"), node("Geri takip et"))
        assertEquals(1, SourceProfileTarget.index(nodes, "dynamic_source"))
    }
    @Test fun handleInRelationshipDescriptionCannotBecomeProfileTap() {
        assertNull(SourceProfileTarget.index(listOf(node("@someone", "Follow @someone")), "someone"))
        assertNull(SourceProfileTarget.index(listOf(node("@someone", "Follow back @someone")), "someone"))
    }
    @Test fun hiddenDisabledAndEmptyBoundsAreRejected() {
        assertNull(SourceProfileTarget.index(listOf(node("@someone", visible = false)), "someone"))
        assertNull(SourceProfileTarget.index(listOf(node("@someone", enabled = false)), "someone"))
        assertNull(SourceProfileTarget.index(listOf(node("@someone").copy(bounds = Rect())), "someone"))
    }
    @Test fun exactDynamicHandleDoesNotMatchBioOrPrefix() {
        val nodes = listOf(node("Contact @someone"), node("@someone_else"), node("@Someone"))
        assertEquals(2, SourceProfileTarget.index(nodes, "@someone"))
        assertNull(SourceProfileTarget.index(nodes, ""))
    }
    @Test fun offscreenRandomSourceMustBeLocatedBeforeTapping() {
        assertNull(SourceProfileTarget.index(listOf(node("@other")), "chosen"))
        assertEquals(0, SourceProfileTarget.index(listOf(node("@chosen")), "chosen"))
    }
}
