package com.atmacanext.app.ai

import org.junit.Assert.*
import org.junit.Test

class PilotProgram27_69Test {
    private fun action(raw: String) = PilotIntentPolicy.local(raw)!!.actions.single()

    @Test fun screenshotFollowupReadsSavedExamplesWithoutANetworkPlan() {
        val a = action("örnek neler var")
        assertEquals(PilotTool.SHOW_POOL, a.tool); assertEquals(5, a.count); assertNull(a.pool)
        assertTrue(PilotIntentPolicy.authorized("örnek neler var", a))
    }
    @Test fun upgradeRemembersThePoolFromTheExistingSuccessfulReceipt() {
        val saved = "GENERATE: 100/100 farklı metin Hazır yorum metinleri havuzuna kaydedildi."
        assertEquals(ContentPool.REPLY, PilotProgramPolicy.recentPool(saved))
        assertEquals(ContentPool.REPLY, PilotProgramPolicy.selectPool(null, PilotProgramPolicy.recentPool(saved), 156, 100))
    }
    @Test fun anExplicitTweetReadOverridesTheRecentReplyGeneration() {
        val a = action("tweet havuzundaki 3 örneği göster")
        assertEquals(ContentPool.TWEET, a.pool); assertEquals(3, a.count)
        assertEquals(ContentPool.TWEET, PilotProgramPolicy.selectPool(a.pool, ContentPool.REPLY, 156, 100))
    }
    @Test fun explicitReplyExamplesDoNotReadTheTweetPool() {
        val a = action("hazır yorumlardan 5 örnek göster")
        assertEquals(PilotTool.SHOW_POOL, a.tool); assertEquals(ContentPool.REPLY, a.pool); assertEquals(5, a.count)
    }
    @Test fun twoNonemptyPoolsWithoutContextAskInsteadOfGuessing() {
        assertNull(PilotProgramPolicy.selectPool(null, null, 156, 100))
    }
    @Test fun onlyNonemptyPoolCanBeSelectedWithoutChatContext() {
        assertEquals(ContentPool.REPLY, PilotProgramPolicy.selectPool(null, null, 0, 100))
        assertEquals(ContentPool.TWEET, PilotProgramPolicy.selectPool(null, null, 156, 0))
    }
    @Test fun previewContainsActualStoredValuesAndTheActualCount() {
        val actual = listOf("Bu nasıl olmuş?", "Devamında ne oldu?", "Bunu beklemiyordum!")
        val text = PilotProgramPolicy.show(ContentPool.REPLY, actual, 2)
        assertTrue(text.contains("toplam 3 kayıt")); assertTrue(text.contains("2. Devamında ne oldu?"))
        assertTrue(text.contains("3. Bunu beklemiyordum!")); assertFalse(text.contains("Bu nasıl olmuş?"))
    }
    @Test fun emptyPoolNeverInventsSampleComments() {
        val text = PilotProgramPolicy.show(ContentPool.REPLY, emptyList())
        assertTrue(text.contains("boş")); assertFalse(text.contains("1."))
    }
    @Test fun previewDoesNotModifySavedData() {
        val values = mutableListOf("Kayıt A", "Kayıt B", "Kayıt C")
        val before = values.toList(); PilotProgramPolicy.show(ContentPool.TWEET, values, 2)
        assertEquals(before, values)
    }
    @Test fun largePreviewIsExplicitlyBoundedButReportsTheRealTotal() {
        val text = PilotProgramPolicy.show(ContentPool.REPLY, (1..100).map { "Yorum $it" }, 100)
        assertTrue(text.contains("toplam 100 kayıt")); assertTrue(text.contains("son 20 metin"))
        assertTrue(text.contains("81. Yorum 81")); assertTrue(text.contains("100. Yorum 100"))
        assertTrue(text.contains("en fazla 20")); assertFalse(text.contains("80. Yorum 80"))
    }
    @Test fun invalidPreviewCountDoesNotBecomeAGenerationRequest() {
        assertEquals(PilotTool.SHOW_POOL, action("0 tane örnek göster").tool)
        try { PilotProgramPolicy.show(ContentPool.REPLY, listOf("Kayıt"), 0); fail() }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun newReplyGenerationRemainsAGeminiContentOperation() {
        val a = action("100 tane hazır yorum metni hazırla ekle")
        assertEquals(PilotTool.GENERATE, a.tool); assertEquals(ContentPool.REPLY, a.pool); assertEquals(100, a.count)
    }
    @Test fun writingNewExamplesIsNotMistakenForReadingSavedExamples() {
        assertNull(PilotProgramPolicy.examples("5 örnek hazır yorum yaz"))
        assertEquals(PilotTool.GENERATE, action("5 hazır yorum örneği hazırla").tool)
    }
    @Test fun movingStoredTextsDoesNotBecomeAShowOrGenerationOperation() {
        assertEquals(PilotTool.TRANSFER, action("tweet havuzundan son 5 metni hazır yorum havuzuna taşı").tool)
        assertNull(PilotProgramPolicy.examples("son 5 hazır yorumu sil"))
    }
    @Test fun poolCountsStaySeparateFromPoolContents() {
        assertEquals(PilotTool.POOLS, action("hazır yorum havuzunda kaç metin var").tool)
        assertEquals(PilotTool.SHOW_POOL, action("havuzda neler var").tool)
    }
    @Test fun simpleExampleWordingUsesTheLocalReader() {
        for (raw in listOf("örnek ver", "birkaç örnek göster", "örnekleri göster", "kaydettiklerini göster", "nasıl yorumlar var"))
            assertEquals(raw, PilotTool.SHOW_POOL, action(raw).tool)
    }
    @Test fun generalQuestionsAreNotAssumedToBePoolReads() {
        assertNull(PilotProgramPolicy.examples("İstanbul'da neler var"))
        assertNull(PilotProgramPolicy.examples("Bu resimde neler var"))
    }
    @Test fun taskControlsRemainDistinctFromGenerationControls() {
        assertEquals(PilotTool.STOP, action("görevleri durdur").tool)
        assertEquals(PilotTool.STOP_GENERATION, action("Gemini metin üretimini durdur").tool)
    }
    @Test fun reportsAreProgramToolsRatherThanContentRequests() {
        assertEquals(PilotTool.REPORT, action("işlem kayıtlarını incele").tool)
        assertEquals(PilotTool.REPORT, action("görevim neden durdu").tool)
    }
    @Test fun taskInventoryUsesTheLocalRepository() {
        assertEquals(PilotTool.TASKS, action("programdaki görevleri göster").tool)
    }
    @Test fun missingTaskNameAsksLocallyInsteadOfUsingGemini() {
        assertNull(PilotIntentPolicy.local("görevi başlat"))
        assertTrue(PilotProgramPolicy.clarification("görevi başlat")!!.contains("Hangi kayıtlı"))
    }
    @Test fun ambiguousPoolOperationsAskForThePoolAndCount() {
        assertNotNull(PilotProgramPolicy.clarification("havuzdaki metinleri sil"))
    }
    @Test fun anApiQuotaCannotAuthorizeAModelToStopTheProgramQueue() {
        assertFalse(PilotProgramPolicy.acceptsNetworkPlan(PilotToolPlan(actions = listOf(PilotAction(PilotTool.STOP)))))
    }
    @Test fun aModelCannotTransferOrDeleteStoredPoolData() {
        assertFalse(PilotProgramPolicy.acceptsNetworkPlan(PilotToolPlan(actions = listOf(PilotAction(PilotTool.TRANSFER, ContentPool.TWEET, ContentPool.REPLY, 5)))))
        assertFalse(PilotProgramPolicy.acceptsNetworkPlan(PilotToolPlan(actions = listOf(PilotAction(PilotTool.DELETE_LAST, ContentPool.REPLY, count = 5)))))
    }
    @Test fun mixedGenerationAndProgramPlanIsRejectedAsAWhole() {
        val generation = PilotAction(PilotTool.GENERATE, ContentPool.REPLY, count = 5, concept = "merak")
        assertFalse(PilotProgramPolicy.acceptsNetworkPlan(PilotToolPlan(actions = listOf(generation, PilotAction(PilotTool.START)))))
        assertTrue(PilotProgramPolicy.acceptsNetworkPlan(PilotToolPlan(actions = listOf(generation))))
    }
    @Test fun plainChatDoesNotNeedAToolOrInventAProgramAction() {
        assertTrue(PilotProgramPolicy.acceptsNetworkPlan(PilotToolPlan(say = "Merhaba")))
    }
    @Test fun apiSchemaDoesNotOfferLocalProgramOrPoolMutationTools() {
        val allowed = PilotIntentSchema.value().getJSONObject("properties").getJSONObject("actions")
            .getJSONObject("items").getJSONObject("properties").getJSONObject("tool").getJSONArray("enum")
        assertEquals(1, allowed.length()); assertEquals("GENERATE", allowed.getString(0))
    }
    @Test fun readAuthorizationCannotSwitchAnExplicitPool() {
        val raw = "hazır yorumları göster"
        val a = action(raw)
        assertFalse(PilotIntentPolicy.authorized(raw, a.copy(pool = ContentPool.TWEET)))
    }
    @Test fun oldAmbiguousTransferReceiptDoesNotGuessTheSourcePool() {
        assertNull(PilotProgramPolicy.recentPool("TRANSFER: Tweet metinleri havuzundan Hazır yorum metinleri havuzuna taşındı"))
    }
    @Test fun explicitPreviewDoesNotFallBackToTheOtherPoolWhenEmpty() {
        assertEquals(ContentPool.REPLY, PilotProgramPolicy.selectPool(ContentPool.REPLY, ContentPool.TWEET, 156, 0))
    }
}
