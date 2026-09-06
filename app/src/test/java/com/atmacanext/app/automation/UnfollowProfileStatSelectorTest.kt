package com.atmacanext.app.automation

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UnfollowProfileStatSelectorTest {
    @Test fun acceptsTurkishFollowingCounterAtAnyPosition() {
        assertNotNull(UnfollowProfileStatSelector.candidateScore(listOf("376 Takip ediliyor"), ""))
        assertNotNull(UnfollowProfileStatSelector.candidateScore(listOf("Takip edilen", "100"), "profile_following_count"))
    }

    @Test fun acceptsEnglishFollowingCounter() {
        assertNotNull(UnfollowProfileStatSelector.candidateScore(listOf("1,204 Following"), ""))
    }

    @Test fun rejectsActionButtonAndForbiddenProfileText() {
        assertNull(UnfollowProfileStatSelector.candidateScore(listOf("Takip ediliyor"), "following_button"))
        assertNull(UnfollowProfileStatSelector.candidateScore(listOf("376 Takip ediliyor", "Doğum tarihi"), ""))
        assertNull(UnfollowProfileStatSelector.candidateScore(listOf("376 Takip ediliyor", "Daha fazlasını göster"), ""))
    }
}
