package com.buse.app.automation

import org.junit.Assert.*
import org.junit.Test

class FollowResultRecovery26_56Test {
    private fun result(observed: Boolean=false, following: Boolean=false, available: Boolean=false,
                       stable: Long=0, elapsed: Long=7000, requested: Boolean=false, unchanged: Long=0) =
        VerifiedFollowPolicy.outcome(observed,following,available,stable,elapsed,requested,250,unchanged)
    @Test fun followBackIsAnAvailableVerifiedActionInBothLanguages() {
        for(label in listOf("Geri Takip Et","Sen de takip et","Sende takip et","Follow back")) {
            assertTrue(VerifiedFollowPolicy.isAvailableFollow(listOf(label)))
            assertFalse(VerifiedFollowPolicy.isPlainFollow(listOf(label)))
        }
    }
    @Test fun plainFollowStillWorks() { assertTrue(VerifiedFollowPolicy.isAvailableFollow(listOf("Takip et"))) }
    @Test fun followingIsNeverANewAction() { assertFalse(VerifiedFollowPolicy.isAvailableFollow(listOf("Takip ediliyor"))) }
    @Test fun pendingIsNeverANewAction() { assertFalse(VerifiedFollowPolicy.isAvailableFollow(listOf("Beklemede"))) }
    @Test fun mixedStatusAndActionIsNotANewFollow() { assertFalse(VerifiedFollowPolicy.isAvailableFollow(listOf("Geri Takip Et","Following"))) }
    @Test fun followBackWithFollowingStatusIsStillConfirmedSuccess() { assertEquals(VerifiedFollowOutcome.SUCCESS,result(true,true,false,250)) }
    @Test fun newRequestedResultCompletesImmediately() { assertEquals(VerifiedFollowOutcome.SUCCESS,result(requested=true)) }
    @Test fun persistentPlainFollowIsNoEffectRatherThanPauseOrSuccess() { assertEquals(VerifiedFollowOutcome.NO_EFFECT,result(available=true,unchanged=1000)) }
    @Test fun noEffectRequiresFullStableWindow() { assertEquals(VerifiedFollowOutcome.UNKNOWN,result(available=true,unchanged=999)) }
    @Test fun shortTapWaitCannotBeClassifiedNoEffect() { assertEquals(VerifiedFollowOutcome.WAIT,result(available=true,elapsed=6999,unchanged=2000)) }
    @Test fun missingRowIsNotNoEffectOrSuccess() { assertEquals(VerifiedFollowOutcome.UNKNOWN,result(unchanged=3000)) }
    @Test fun conflictingStatusesRemainUnresolved() { assertEquals(VerifiedFollowOutcome.UNKNOWN,result(following=true,available=true,unchanged=3000)) }
    @Test fun actualFollowingRevertKeepsExistingLimitProtection() { assertEquals(VerifiedFollowOutcome.REVERTED,result(observed=true,available=true,unchanged=1000)) }
    @Test fun noEffectDoesNotIncreaseRevertStreak() { assertEquals(0,VerifiedFollowPolicy.nextStreak(2,VerifiedFollowOutcome.NO_EFFECT)) }
    @Test fun watchdogDeadlineIsTenSeconds() {
        assertEquals(10000L,AutomationStallPolicy.TIMEOUT_MS)
        val watch=AutomationWatchdog(AutomationStallPolicy.TIMEOUT_MS)
        watch.reset(1000);assertFalse(watch.isStuck(10999));assertTrue(watch.isStuck(11000))
    }
    @Test fun pendingVerificationRemainsWatchable() {
        assertTrue(AutomationStallPolicy.shouldWatch(AutomationRuntimeState(taskId="t",sessionId="s",status=RuntimeStatus.VERIFYING)))
    }
    @Test fun manualPauseAndIntentionalWaitAreNotRestarted() {
        assertFalse(AutomationStallPolicy.shouldWatch(AutomationRuntimeState(taskId="t",status=RuntimeStatus.PAUSED)))
        assertFalse(AutomationStallPolicy.shouldWatch(AutomationRuntimeState(taskId="t",status=RuntimeStatus.WAITING,flowStage=XFlowStage.WAIT_INTERVAL)))
    }
    @Test fun profileFrameWhileOpeningSheetCannotCloseIt() {
        val gate=AccountNavigationGate();gate.issued(XScreen.ACCOUNT_DRAWER,1000,XScreen.ACCOUNT_SWITCHER)
        assertTrue(gate.wait(XScreen.PROFILE,1500));assertTrue(gate.wait(XScreen.FOLLOWERS_LIST,2100))
        assertFalse(gate.wait(XScreen.ACCOUNT_SWITCHER,2200))
    }
    @Test fun expectedDrawerProofEndsWaitImmediately() {
        val gate=AccountNavigationGate();gate.issued(XScreen.HOME,1000,XScreen.ACCOUNT_DRAWER)
        assertFalse(gate.wait(XScreen.ACCOUNT_DRAWER,1100))
    }
    @Test fun transitionGateIsBoundedAndClearsForNewAccount() {
        val gate=AccountNavigationGate();gate.issued(XScreen.ACCOUNT_DRAWER,1000,XScreen.ACCOUNT_SWITCHER)
        assertTrue(gate.wait(XScreen.UNKNOWN,3999));assertFalse(gate.wait(XScreen.PROFILE,4000))
        gate.issued(XScreen.HOME,5000,XScreen.ACCOUNT_DRAWER);gate.clear();assertFalse(gate.wait(XScreen.PROFILE,5100))
    }
}
