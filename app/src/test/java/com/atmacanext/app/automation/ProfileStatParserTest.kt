package com.atmacanext.app.automation

import org.junit.Assert.*
import org.junit.Test

class ProfileStatParserTest {
    @Test fun keepsDifferentCountersSeparateInOneRow() {
        assertEquals(ProfileStatParser.Stats("456", "123"), ProfileStatParser.parse(listOf("123 Following 456 Followers")))
    }
    @Test fun splitNodesDoNotBorrowTheOtherValue() {
        assertEquals(ProfileStatParser.Stats("9.876", "321"), ProfileStatParser.parse(listOf("321", "Takip edilen", "9.876", "Takipçi")))
    }
    @Test fun preservesTurkishAbbreviationAndZero() {
        assertEquals(ProfileStatParser.Stats("1,2 B", "0"), ProfileStatParser.parse(listOf("1,2\u00a0B Takipçi", "0 Takip ediliyor")))
    }
    @Test fun rejectsBioNumbersAndMissingCounts() {
        assertEquals(ProfileStatParser.Stats(), ProfileStatParser.parse(listOf("Joined 2020", "I have 100 followers in my club", "Followers", "Following")))
    }
    @Test fun readsLabelFirstAndGroupedDigits() {
        assertEquals(ProfileStatParser.Stats("1 234", "72"), ProfileStatParser.parse(listOf("Followers: 1 234", "Following: 72")))
    }
    @Test fun oneMissingCounterRemainsUnknown() {
        assertEquals(ProfileStatParser.Stats(following = "88"), ProfileStatParser.parse(listOf("88 Following", "Followers")))
    }
}
