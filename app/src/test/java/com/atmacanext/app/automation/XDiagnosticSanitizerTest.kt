package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XDiagnosticSanitizerTest {
    @Test fun arbitraryUserContentIsRedacted() {
        assertEquals("<redacted>", XDiagnosticSanitizer.sanitizeLabel("Bugün harika bir gün ve bu bir gönderi metni"))
    }

    @Test fun structuralLabelsArePreservedWithoutCounts() {
        assertEquals("#followers", XDiagnosticSanitizer.sanitizeLabel("123 Followers"))
    }

    @Test fun handlesAreHashed() {
        val sanitized = XDiagnosticSanitizer.sanitizeLabel("@private_user").orEmpty()
        assertTrue(sanitized.startsWith("@<handle:"))
        assertNotEquals("@private_user", sanitized)
    }
}
