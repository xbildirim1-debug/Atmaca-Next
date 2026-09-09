package com.atmacanext.app.automation

import org.junit.Assert.*
import org.junit.Test

class DiscoveryTweetOpenRecoveryTest {
    private val body = "Bakan Akın Gürlek: Sosyal medya üzerinden lüks yaşam, gösterişli düğün veya benzeri mizansenlerle suç gelirlerinin kaynağını"
    private val attempt = DiscoveryTweetOpenRecovery.Attempt("old", "pusholder", "$body… Daha fazlasını göster", 1, 1000L)
    private fun candidate(text: String = "$body gizlemeye yönelik faaliyetler incelenecektir.", key: String = "expanded", author: String = "pusholder", age: Long = 120L) =
        DiscoveryTweetOpenRecovery.Candidate(key, author, text, age)

    @Test fun videoInlineExpansionPreservesPostDespiteChangedTextHash() {
        assertTrue(DiscoveryTweetOpenRecovery.matches(attempt, candidate()))
    }
    @Test fun embeddedQuoteBySameAuthorIsNotTheRetry() {
        assertFalse(DiscoveryTweetOpenRecovery.matches(attempt, candidate("Bakan Akın Gürlek: Tavrımız nettir; sosyal medyada oluşturulan sanal şöhret perde olamayacaktır.")))
    }
    @Test fun sameHashCannotOverrideDifferentAuthorOrYoungAge() {
        assertFalse(DiscoveryTweetOpenRecovery.matches(attempt, candidate(key="old", author="other")))
        assertFalse(DiscoveryTweetOpenRecovery.matches(attempt, candidate(key="old", age=119L)))
    }
    @Test fun missingTextAndShortSharedPrefixesDoNotAuthorizeRetry() {
        assertFalse(DiscoveryTweetOpenRecovery.matches(attempt, candidate("",key="old")))
        assertFalse(DiscoveryTweetOpenRecovery.matches(attempt.copy(text="Bakan Akın Gürlek"), candidate()))
    }
    @Test fun semanticWhitespaceAndEnglishExpansionAreHandled() {
        assertTrue(DiscoveryTweetOpenRecovery.matches(attempt.copy(text=body.replace(" ", "\n")+"... Show more"),candidate()))
    }
    @Test fun acceptedTapWaitsForNavigationBeforeRetry() {
        assertEquals(DiscoveryTweetOpenRecovery.Decision.WAIT,DiscoveryTweetOpenRecovery.decide(attempt,true,2499L))
        assertEquals(DiscoveryTweetOpenRecovery.Decision.RETRY,DiscoveryTweetOpenRecovery.decide(attempt,true,2500L))
    }
    @Test fun threeAttemptsThenRescanEvenWhenDispatchAlwaysSucceeds() {
        var state=attempt
        for (i in 2..3) {
            val now=state.at+1500L
            assertEquals(DiscoveryTweetOpenRecovery.Decision.RETRY,DiscoveryTweetOpenRecovery.decide(state,true,now))
            state=state.copy(count=i,at=now)
        }
        assertEquals(DiscoveryTweetOpenRecovery.Decision.RESCAN,DiscoveryTweetOpenRecovery.decide(state,true,state.at+1500L))
    }
    @Test fun MissingOrAmbiguousCandidateHasBoundedWait() {
        assertEquals(DiscoveryTweetOpenRecovery.Decision.WAIT,DiscoveryTweetOpenRecovery.decide(attempt,false,3999L))
        assertEquals(DiscoveryTweetOpenRecovery.Decision.RESCAN,DiscoveryTweetOpenRecovery.decide(attempt,false,4000L))
    }
    @Test fun expandedTweetRetriesUseDistinctBodyPoints() {
        assertEquals(0.50f to 0.50f, DiscoveryTweetOpenRecovery.tapPosition(1))
        assertEquals(0.30f to 0.65f, DiscoveryTweetOpenRecovery.tapPosition(2))
    }
}
