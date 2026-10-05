package com.atmacanext.app.automation

import org.junit.Assert.*
import org.junit.Test

class UnfollowRecovery26_59Test {
    private fun can(confirmed:Boolean=false, observed:Boolean=false, follow:Boolean=false,
                    following:Boolean=true, elapsed:Long=10000,stable:Long=1000) =
        UnfollowRecoveryPolicy.shouldRestart(confirmed,observed,follow,following,elapsed,stable)
    @Test fun unappliedConfirmationRestartsAtTenSeconds() { assertTrue(can()) }
    @Test fun earlierFailureWaitsForRecoveryDeadline() { assertFalse(can(elapsed=9999)) }
    @Test fun followingEvidenceMustBeStable() { assertFalse(can(stable=999)) }
    @Test fun acceptedConfirmationCannotBeRepeated() { assertFalse(can(confirmed=true)) }
    @Test fun observedChangeCannotReleaseAttemptBudget() { assertFalse(can(observed=true)) }
    @Test fun missingRowCannotCreateAnotherAction() { assertFalse(can(following=false)) }
    @Test fun conflictingLabelsDoNotProveUnappliedAction() { assertFalse(can(follow=true)) }
    @Test fun appliedFollowStateCannotRestartAsNoEffect() { assertFalse(can(follow=true,following=false)) }
    @Test fun manualPauseCancelsAutomaticReturnCallback() {
        val f=AutomationController.javaClass.getDeclaredField("unfollowRecoveryReturning").apply { isAccessible=true }
        f.setBoolean(AutomationController,true)
        AutomationController.pause()
        assertFalse(f.getBoolean(AutomationController))
    }
    @Test fun stopClearsReturnCallbackGuard() {
        val f=AutomationController.javaClass.getDeclaredField("unfollowRecoveryReturning").apply { isAccessible=true }
        f.setBoolean(AutomationController,true)
        AutomationController.stop()
        assertFalse(f.getBoolean(AutomationController))
    }
}
