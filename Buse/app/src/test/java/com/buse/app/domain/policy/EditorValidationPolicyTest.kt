package com.buse.app.domain.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorValidationPolicyTest {
    @Test fun usernameValidation() {
        assertEquals("@buse_next", EditorValidationPolicy.normalizeXUsername("buse_next"))
        assertTrue(EditorValidationPolicy.isValidXUsername("@buse_next"))
        assertFalse(EditorValidationPolicy.isValidXUsername("@yanlış kullanıcı"))
        assertFalse(EditorValidationPolicy.isValidXUsername("@1234567890123456"))
    }

    @Test fun clockAndLimitValidation() {
        assertTrue(EditorValidationPolicy.isValidClock(23, 59))
        assertFalse(EditorValidationPolicy.isValidClock(24, 0))
        assertTrue(EditorValidationPolicy.isValidTaskLimit(35))
        assertFalse(EditorValidationPolicy.isValidTaskLimit(36))
    }
}
