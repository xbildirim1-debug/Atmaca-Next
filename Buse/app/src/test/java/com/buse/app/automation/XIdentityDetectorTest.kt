package com.buse.app.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XIdentityDetectorTest {
    @Test fun explicitHandleIsAccepted() {
        assertEquals("buse_1", XIdentityDetector.extractHandle("@Buse_1"))
        assertEquals("buse_1", XIdentityDetector.extractHandle("Merhaba @Buse_1 · Follow"))
    }

    @Test fun ordinaryXUiWordsAreNeverHandles() {
        assertNull(XIdentityDetector.extractHandle("Following"))
        assertNull(XIdentityDetector.extractHandle("Profile"))
        assertNull(XIdentityDetector.extractHandle("Followers"))
        assertNull(XIdentityDetector.extractHandle("Takip"))
    }

    @Test fun invalidLongHandleIsRejected() {
        assertNull(XIdentityDetector.extractHandle("@abcdefghijklmnop"))
    }
}
