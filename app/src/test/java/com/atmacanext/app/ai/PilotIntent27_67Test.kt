package com.atmacanext.app.ai

import com.atmacanext.app.domain.model.TaskType
import org.junit.Assert.*
import org.junit.Test

class PilotIntent27_67Test {
    private fun action(raw: String) = PilotIntentPolicy.local(raw)!!.actions.single()
    @Test fun screenshotTransferIsFiftySixAndNeverSixNewTexts() {
        val raw = "twet metinlerine son eklediğin 56 metni sil ve o metinleri hazır yorum metinlerine ekle"
        val a = action(raw)
        assertEquals(PilotTool.TRANSFER, a.tool); assertEquals(56, a.count)
        assertEquals(ContentPool.TWEET, a.pool); assertEquals(ContentPool.REPLY, a.destination)
        assertNull(TweetGenerationRequest.parse(raw))
        assertFalse(PilotIntentPolicy.authorized(raw, a.copy(count = 6)))
    }
    @Test fun explicitReplyGenerationChoosesReplyPool() {
        val a = action("Futbol konseptinde hazır yorum havuzuna 500 metin üret")
        assertEquals(PilotTool.GENERATE, a.tool); assertEquals(ContentPool.REPLY, a.pool); assertEquals(500, a.count)
    }
    @Test fun explicitTweetGenerationPreservesFiveHundred() {
        val a = action("Tweet havuzuna futbol konseptinde 500 twit metni ekle")
        assertEquals(PilotTool.GENERATE, a.tool); assertEquals(ContentPool.TWEET, a.pool); assertEquals(500, a.count)
    }
    @Test fun missingCountNeverSilentlyInventsSix() {
        assertNull(PilotIntentPolicy.local("hazır yorum metni üret"))
        assertNull(TweetGenerationRequest.parse("tweet metni üret"))
    }
    @Test fun topicYearIsNotTheGenerationCount() {
        assertEquals(50, action("2026 spor konseptinde 50 tweet üret").count)
    }
    @Test fun deleteOnlyDoesNotGenerateOrMove() {
        val a = action("tweet havuzundan son 56 metni sil")
        assertEquals(PilotTool.DELETE_LAST, a.tool); assertEquals(56, a.count); assertNull(a.destination)
    }
    @Test fun copyDoesNotPretendToBeTransfer() {
        val a = action("Tweet metinlerinden son 10 metni hazır yorum havuzuna kopyala")
        assertEquals(PilotTool.COPY, a.tool); assertEquals(10, a.count)
    }
    @Test fun undoIsAvailableEvenWhenTransferIsUsedAsANoun() {
        val a = action("Son taşıma işlemini geri al")
        assertEquals(PilotTool.UNDO_POOL, a.tool); assertTrue(PilotIntentPolicy.authorized("Son taşıma işlemini geri al", a))
    }
    @Test fun casualStatementsDoNotLaunchTasks() {
        assertNull(PilotIntentPolicy.local("neden devam etmedin"))
        assertNull(PilotIntentPolicy.local("durdurma nedenini açıkla"))
        assertNull(PilotIntentPolicy.local("toplu twit metinleri ayrı, hazır yorum metinleri ayrı"))
    }
    @Test fun localPauseAndStopRemainImmediate() {
        assertEquals(PilotTool.PAUSE, action("görevi duraklat").tool)
        assertEquals(PilotTool.STOP, action("görevi durdur").tool)
        assertEquals(PilotTool.RESUME, action("kaldığın yerden devam et").tool)
    }
    @Test fun stopGenerationDoesNotStopTheAccountQueue() {
        assertEquals(PilotTool.STOP_GENERATION, action("metin üretimini durdur").tool)
    }
    @Test fun reportUsesRealLogTool() {
        assertEquals(PilotTool.REPORT, action("işlem kayıtlarını incele, en çok sıkıntı çıkaran hatalar neler, neleri düzeltelim").tool)
    }
    @Test fun startSelectionDoesNotMixRetweetersWithPostingTweets() {
        val a = action("@hesap retweetçi takibini başlat")
        assertEquals(listOf(TaskType.RETWEETER_FOLLOW), a.types); assertEquals("hesap", a.account)
    }
    @Test fun taskOrderFollowsTheUsersSequence() {
        val a = action("önce yorumcu takibini sonra onaylı takibi başlat")
        assertEquals(listOf(TaskType.COMMENTER_FOLLOW, TaskType.VERIFIED_FOLLOW), a.types)
    }
    @Test fun readOnlyImageRequestCannotAuthorizePoolDeletion() {
        assertFalse(PilotIntentPolicy.authorized("Bu ekran görüntüsünü incele", PilotAction(PilotTool.DELETE_LAST, ContentPool.TWEET, count = 56)))
        assertFalse(PilotIntentPolicy.authorized("", PilotAction(PilotTool.START, all = true)))
    }
    @Test fun negativeRequestCannotBecomeADeleteOrGenerate() {
        assertFalse(PilotIntentPolicy.authorized("56 metni silme", PilotAction(PilotTool.DELETE_LAST, ContentPool.TWEET, count = 56)))
        assertFalse(PilotIntentPolicy.authorized("tweet üretme", PilotAction(PilotTool.GENERATE, ContentPool.TWEET, count = 6)))
    }
    @Test fun modelTransferJsonPreservesCountAndEscapedConcept() {
        val p = PilotIntentPolicy.parseModel("""{"say":"","actions":[{"tool":"TRANSFER","pool":"TWEET","destination":"REPLY","count":56}]}""")!!
        assertEquals(56, p.actions.single().count); assertEquals(ContentPool.REPLY, p.actions.single().destination)
    }
    @Test fun malformedUnsupportedAndFractionalActionsAreRejected() {
        for (body in listOf("""{"actions":[{"tool":"SQL"}]}""",
            """{"actions":[{"tool":"DELETE_LAST","pool":"TWEET","count":5.6}]}""",
            """{"actions":[{"tool":"TRANSFER","pool":"TWEET","destination":"TWEET","count":56}]}""",
            """{"actions":[{"tool":"GENERATE","pool":"REPLY","count":1001,"concept":"test"}]}"""))
            assertNull(PilotIntentPolicy.parseModel(body))
    }
    @Test fun missingPoolAndMissingTransferDestinationAreRejected() {
        assertNull(PilotIntentPolicy.parseModel("""{"actions":[{"tool":"GENERATE","count":50,"concept":"x"}]}"""))
        assertNull(PilotIntentPolicy.parseModel("""{"actions":[{"tool":"TRANSFER","pool":"TWEET","count":56}]}"""))
    }
    @Test fun chatClaimsCannotMasqueradeAsMutationReceipts() {
        assertFalse(PilotIntentPolicy.honestChat("56 metni taşıdım").contains("taşıdım"))
        assertEquals("Merhaba, nasıl yardımcı olayım?", PilotIntentPolicy.honestChat("Merhaba, nasıl yardımcı olayım?"))
    }
    @Test fun noMoreThanFourModelActionsCanRun() {
        assertNull(PilotIntentPolicy.parseModel("{\"actions\":[" + List(5) { "{\"tool\":\"STATUS\"}" }.joinToString(",") + "]}"))
    }
}
