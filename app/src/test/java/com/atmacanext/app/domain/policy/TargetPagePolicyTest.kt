package com.atmacanext.app.domain.policy

import org.junit.Assert.*
import org.junit.Test

class TargetPagePolicyTest {
    @Test fun acceptsAtPrefixAndNormalizesCase() {
        assertEquals("page_one", TargetPagePolicy.normalize(" @Page_One "))
    }
    @Test fun rejectsLinksAndInvalidHandlesInsteadOfSilentlyChangingThem() {
        listOf("https://x.com/page", "two names", "@", "1234567890123456", "çağrı").forEach { assertNull(TargetPagePolicy.normalize(it)) }
    }
    @Test fun fourthTargetIsRejectedButExistingOneCanBeUpdated() {
        val existing = listOf("one", "two", "three")
        assertFalse(TargetPagePolicy.canAdd(existing, "four"))
        assertTrue(TargetPagePolicy.canAdd(existing, "two"))
        assertTrue(TargetPagePolicy.canAdd(listOf("one", "two"), "three"))
    }
}
