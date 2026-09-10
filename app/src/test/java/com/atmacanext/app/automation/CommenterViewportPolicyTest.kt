package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommenterViewportPolicyTest {
    @Test fun handledTopCommenterDoesNotForceScrollPastOtherVisibleUsers() {
        val visible = listOf("first", "second", "third")
        val excluded = setOf("first")
        assertEquals("second", CommenterViewportPolicy.nextVisible(visible, excluded))
        assertFalse(CommenterViewportPolicy.shouldScroll(visible, excluded))
    }

    @Test fun viewportScrollsOnlyAfterEveryVisibleCommenterIsHandledOrExcluded() {
        val visible = listOf("first", "second", "third")
        val excluded = setOf("first", "second", "third")
        assertNull(CommenterViewportPolicy.nextVisible(visible, excluded))
        assertTrue(CommenterViewportPolicy.shouldScroll(visible, excluded))
    }

    @Test fun ownAccountAndTargetCanBeExcludedWithoutSkippingOtherCommenters() {
        val visible = listOf("target", "own", "commenter")
        assertEquals("commenter", CommenterViewportPolicy.nextVisible(visible, setOf("target", "own")))
    }
}
