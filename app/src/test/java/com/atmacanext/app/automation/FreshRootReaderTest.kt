package com.atmacanext.app.automation

import org.junit.Assert.*
import org.junit.Test

class FreshRootReaderTest {
    @Test fun cacheIsInvalidatedBeforeAcquiringTheRoot() {
        var cached = "Followers"
        val steps = mutableListOf<String>()
        val root = FreshRootReader.read(
            invalidate = { steps += "clear"; cached = "Onaylanmış takipçiler" },
            acquire = { steps += "acquire"; cached },
            refresh = { steps += "refresh"; true },
        )
        assertEquals("Onaylanmış takipçiler", root)
        assertEquals(listOf("clear", "acquire", "refresh"), steps)
    }

    @Test fun detachedRootCannotAuthorizeAnAction() {
        assertNull(FreshRootReader.read({}, { "old Followers" }, { false }))
    }

    @Test fun missingRootDoesNotReuseThePreviousRead() {
        assertEquals("first", FreshRootReader.read({}, { "first" }, { true }))
        assertNull(FreshRootReader.read<String>({}, { null }, { error("No root to refresh") }))
    }

    @Test fun eachReadGetsNewRelationshipState() {
        var button = "Takip et"
        fun read() = FreshRootReader.read({}, { button }, { true })
        assertEquals("Takip et", read())
        button = "Takip ediliyor"
        assertEquals("Takip ediliyor", read())
        button = "Takip et"
        assertEquals("Takip et", read())
    }
}
