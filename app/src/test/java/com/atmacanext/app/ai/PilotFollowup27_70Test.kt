package com.atmacanext.app.ai

import org.junit.Assert.*
import org.junit.Test

class PilotFollowup27_70Test {
    private val topic = "100 tane hazır yorum metni hazırla ekle. şaşırma ve merak içerikli kısa"
    private val context = PilotGenerationContext(ContentPool.REPLY, topic)
    private fun resolve(raw: String, c: PilotGenerationContext? = context) = PilotGenerationFollowup.resolve(raw, c)
    private fun ready(raw: String) = (resolve(raw) as PilotGenerationFollowup.Decision.Ready).request
    private fun ask() = resolve("1000 tane yap onları.") as PilotGenerationFollowup.Decision.Ask
    private fun turns(receipt: String) = listOf(PilotGenerationFollowup.Turn(true, topic), PilotGenerationFollowup.Turn(false, receipt))

    @Test fun screenshotReferenceAsksTheOnlyAmbiguousPartLocally() {
        val a = ask()
        assertTrue(a.message.contains("Toplamı 1000")); assertTrue(a.message.contains("1000 yeni"))
        assertEquals(ContentPool.REPLY, a.pending!!.context.pool); assertEquals(topic, a.pending.context.concept)
    }
    @Test fun aTotalAnswerUsesThePreviousCountAndConcept() {
        val r = PilotGenerationFollowup.resolve("toplam", null, ask().pending) as PilotGenerationFollowup.Decision.Ready
        assertEquals(1000, r.request.quantity); assertEquals(PilotGenerationFollowup.Mode.TOTAL, r.request.mode)
        assertEquals(context, r.request.context)
    }
    @Test fun aNewTextAnswerAddsRatherThanCompletes() {
        val r = PilotGenerationFollowup.resolve("yeni ekle", null, ask().pending) as PilotGenerationFollowup.Decision.Ready
        assertEquals(1000, PilotGenerationFollowup.action(r.request, 100)!!.count)
    }
    @Test fun completing100ExistingTo1000Generates900() {
        val a = PilotGenerationFollowup.action(ready("Onları toplam 1000 metne tamamla"), 100)!!
        assertEquals(PilotTool.GENERATE, a.tool); assertEquals(900, a.count)
        assertEquals(ContentPool.REPLY, a.pool); assertEquals(topic, a.concept)
    }
    @Test fun completionRechecksTheActualCountAtExecutionTime() {
        val request = ready("Onları toplam 1000 yap")
        assertEquals(850, PilotGenerationFollowup.action(request, 150)!!.count)
    }
    @Test fun explicit1000NewTextsAdds1000ToTheExisting100() {
        val a = PilotGenerationFollowup.action(ready("Onlardan 1000 yeni metin üret"), 100)!!
        assertEquals(1000, a.count)
    }
    @Test fun anotherBatchWithoutAPoolNounKeepsTheReplyPool() {
        val a = PilotGenerationFollowup.action(ready("1000 tane daha üret"), 100)!!
        assertEquals(ContentPool.REPLY, a.pool); assertEquals(1000, a.count)
    }
    @Test fun sameTopicSurvivesTurkishDottedAndDotlessLetters() {
        val a = PilotGenerationFollowup.action(ready("AYNI KONUDA 25 TANE DAHA HAZIRLA"), 100)!!
        assertEquals(25, a.count); assertEquals(topic, a.concept)
    }
    @Test fun aTargetAlreadyReachedDoesNothing() {
        assertNull(PilotGenerationFollowup.action(ready("Onları toplam 1000 yap"), 1000))
    }
    @Test fun aTargetBelowTheCurrentCountNeverDeletesTexts() {
        assertNull(PilotGenerationFollowup.action(ready("Onları toplam 50 yap"), 100))
    }
    @Test fun capacityIsCheckedBeforeAdding() {
        try { PilotGenerationFollowup.action(ready("Onlardan 1000 yeni üret"), 9500); fail() }
        catch (e: IllegalArgumentException) { assertTrue(e.message!!.contains("yeterli yer yok")) }
    }
    @Test fun moreThan1000NewTextsIsRejected() {
        assertTrue(resolve("Onlardan 1001 tane daha üret") is PilotGenerationFollowup.Decision.Ask)
    }
    @Test fun aLargeTargetWithAnAllowedDeltaIsAccepted() {
        val a = PilotGenerationFollowup.action(ready("Onları toplam 10000 yap"), 9500)!!
        assertEquals(500, a.count)
    }
    @Test fun aLargeTargetDoesNotSilentlyReduceTheRequestedDelta() {
        try { PilotGenerationFollowup.action(ready("Onları toplam 5000 yap"), 100); fail() }
        catch (e: IllegalArgumentException) { assertTrue(e.message!!.contains("4900")) }
    }
    @Test fun zeroAndNegativeAmountsNeverCreateRequests() {
        for (s in listOf("Onlardan 0 daha üret", "Onlardan -5 daha üret"))
            assertTrue(s, resolve(s) is PilotGenerationFollowup.Decision.Ask)
    }
    @Test fun numericOverflowDoesNotBecomeThePendingCount() {
        val d = PilotGenerationFollowup.resolve("Onlardan 99999999999999 daha üret", context, ask().pending)
        assertTrue(d is PilotGenerationFollowup.Decision.Ask)
    }
    @Test fun multipleCountsAskRatherThanTakingTheFirstNumber() {
        assertTrue(resolve("Onlardan 100 veya 1000 tane daha üret") is PilotGenerationFollowup.Decision.Ask)
    }
    @Test fun aQuestionNeverStartsGeneration() {
        assertTrue(resolve("Onlardan 1000 tane üret mi?") is PilotGenerationFollowup.Decision.Ask)
    }
    @Test fun negatedRequestsDoNotStartGeneration() {
        assertTrue(resolve("Onlardan 1000 daha üret istemiyorum") is PilotGenerationFollowup.Decision.Ask)
    }
    @Test fun missingPriorConceptAsksForThePoolAndTopicWithoutGemini() {
        val d = resolve("1000 tane yap onları", null) as PilotGenerationFollowup.Decision.Ask
        assertNull(d.pending); assertTrue(d.message.contains("Hangi havuza"))
    }
    @Test fun anExplicitDifferentPoolDoesNotInheritTheReplyTopic() {
        assertTrue(resolve("Onlardan 1000 tane daha tweet üret") is PilotGenerationFollowup.Decision.Ask)
    }
    @Test fun twoPoolsCannotBeResolvedByAPronoun() {
        assertTrue(resolve("Onlardan 100 tweet ve hazır yorum üret") is PilotGenerationFollowup.Decision.Ask)
    }
    @Test fun nativeTaskCommandsNeverInheritGenerationPermission() {
        for (s in listOf("1000 tane yap onları ve görevleri başlat", "görevi durdur", "aynı hesapta 1000 takip yap", "1000 tane daha görev başlat"))
            assertEquals(s, PilotGenerationFollowup.Decision.None, resolve(s))
    }
    @Test fun poolEditsCannotBeSmuggledIntoAFollowup() {
        for (s in listOf("Onlardan 100 tane sil", "Onlardan 100 tane taşı", "Onlardan 100 tane kopyala", "Onlardan 100 tane üret ve sil"))
            assertEquals(s, PilotGenerationFollowup.Decision.None, resolve(s))
    }
    @Test fun oldBroadAuthorizationIsNotRelaxedForTheWordYap() {
        assertFalse(PilotIntentPolicy.authorized("1000 tane yap onları", PilotAction(PilotTool.GENERATE, ContentPool.REPLY, count = 1000, concept = topic)))
        assertFalse(PilotIntentPolicy.authorized("1000 tane yap onları", PilotAction(PilotTool.START)))
    }
    @Test fun freshExplicitGenerationStillUsesTheExistingPath() {
        assertEquals(PilotGenerationFollowup.Decision.None, resolve(topic))
        assertEquals(ContentPool.REPLY, PilotIntentPolicy.local(topic)!!.actions.single().pool)
    }
    @Test fun readingExamplesDoesNotBecomeNewGeneration() {
        assertEquals(PilotGenerationFollowup.Decision.None, resolve("örnek göster"))
        assertEquals(PilotTool.SHOW_POOL, PilotIntentPolicy.local("örnek göster")!!.actions.single().tool)
    }
    @Test fun totalWithoutAPendingQuestionNeverBorrowsAnOldQuantity() {
        assertEquals(PilotGenerationFollowup.Decision.None, resolve("toplam"))
    }
    @Test fun unrelatedAnswerDoesNotConsumeAPendingQuestionAsAnAction() {
        assertEquals(PilotGenerationFollowup.Decision.None, PilotGenerationFollowup.resolve("görevleri göster", context, ask().pending))
    }
    @Test fun pendingCountCanBeExplicitlyCorrected() {
        val d = PilotGenerationFollowup.resolve("500 yeni ekle", context, ask().pending) as PilotGenerationFollowup.Decision.Ready
        assertEquals(500, d.request.quantity)
    }
    @Test fun successfulOldReceiptRestoresTheOriginalConceptOnUpgrade() {
        assertEquals(context, PilotGenerationFollowup.recover(ContentPool.REPLY, turns("100/100 farklı metin Hazır yorum metinleri havuzuna kaydedildi.")))
    }
    @Test fun partialSuccessAlsoRestoresTheSavedTopic() {
        assertEquals(context, PilotGenerationFollowup.recover(ContentPool.REPLY, turns("Gemini üretimi tamamlanamadı; 50/100 metin Hazır yorum metinleri havuzunda korundu. Kota dolu.")))
    }
    @Test fun aPromiseOrZeroSavedReceiptDoesNotInventASavedTopic() {
        for (s in listOf("100 metin üretilip Hazır yorum metinleri havuzuna eklenecek.", "Gemini üretimi tamamlanamadı; 0/100 metin Hazır yorum metinleri havuzunda korundu."))
            assertNull(PilotGenerationFollowup.recover(ContentPool.REPLY, turns(s)))
    }
    @Test fun aPreviewCannotActAsASuccessfulGenerationReceipt() {
        assertNull(PilotGenerationFollowup.recover(ContentPool.REPLY, turns("Hazır yorum metinleri: toplam 100 kayıt. Kayıtlı son 5 metin:\n96. Merak ediyorum.")))
    }
    @Test fun screenshotHistoryRecoversTheTopicPastExampleReadsAndTheFailedFollowup() {
        val history = turns("100/100 farklı metin Hazır yorum metinleri havuzuna kaydedildi.") + listOf(
            PilotGenerationFollowup.Turn(true, "örnek göster"), PilotGenerationFollowup.Turn(false, "Hazır yorum metinleri: toplam 100 kayıt."),
            PilotGenerationFollowup.Turn(true, "1000 tane yap onları."), PilotGenerationFollowup.Turn(false, "Bu mesaj açık talimat içermiyor."))
        assertEquals(context, PilotGenerationFollowup.recover(ContentPool.REPLY, history))
    }
    @Test fun screenshotInstructionsCannotSupplyARecoveredTopic() {
        val history = listOf(PilotGenerationFollowup.Turn(true, topic, hasImage = true), PilotGenerationFollowup.Turn(false, "100/100 farklı metin Hazır yorum metinleri havuzuna kaydedildi."))
        assertNull(PilotGenerationFollowup.recover(ContentPool.REPLY, history))
    }
    @Test fun receiptFromTheOtherPoolDoesNotRestoreThisTopic() {
        assertNull(PilotGenerationFollowup.recover(ContentPool.REPLY, turns("100/100 farklı metin Tweet metinleri havuzuna kaydedildi.")))
    }
    @Test fun aFailedNewTopicDoesNotOverwriteTheLastSavedTopic() {
        val history = turns("100/100 farklı metin Hazır yorum metinleri havuzuna kaydedildi.") + listOf(
            PilotGenerationFollowup.Turn(true, "50 hazır yorum üret, spor hakkında"),
            PilotGenerationFollowup.Turn(false, "Gemini üretimi tamamlanamadı; 0/50 metin Hazır yorum metinleri havuzunda korundu."))
        assertEquals(context, PilotGenerationFollowup.recover(ContentPool.REPLY, history))
    }
    @Test fun explicitTargetSuffixIsUnderstood() {
        assertEquals(900, PilotGenerationFollowup.action(ready("Onları 1000’e çıkar"), 100)!!.count)
    }
    @Test fun noMutationCanBeCreatedWithoutANonblankTopic() {
        assertTrue(resolve("Onlardan 10 daha üret", context.copy(concept = "")) is PilotGenerationFollowup.Decision.Ask)
    }
    @Test fun taskTypeNamesAreNeverMistakenForTheReplyPool() {
        for (s in listOf("Onlardan 1000 yorumcu yap", "Onlardan 1000 retweetçi üret", "Onlardan 1000 onaylı yap", "Onlardan 1000 beğeni yap"))
            assertEquals(s, PilotGenerationFollowup.Decision.None, resolve(s))
        assertNull(PilotGenerationFollowup.mentionedPool("yorumcu"))
    }
    @Test fun removingTextsIsNotTreatedAsAddingMoreTexts() {
        assertEquals(PilotGenerationFollowup.Decision.None, resolve("Onlardan 100 tane çıkar ve 100 yeni ekle"))
        assertEquals(PilotGenerationFollowup.Decision.None, resolve("Onlardan 100 çıkar yeni ekle"))
    }
}
