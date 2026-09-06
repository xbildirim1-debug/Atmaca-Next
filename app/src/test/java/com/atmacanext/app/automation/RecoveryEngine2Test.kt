package com.atmacanext.app.automation
import org.junit.Assert.assertEquals
import org.junit.Test
class RecoveryEngine2Test {
 @Test fun pausesAfterBoundedRetries() {
   val r=RecoveryEngine2(2)
   r.decide(RecoveryCause.WRONG_SCREEN, XScreen.UNKNOWN, false)
   r.decide(RecoveryCause.WRONG_SCREEN, XScreen.UNKNOWN, false)
   assertEquals(RecoveryAction.PAUSE,r.decide(RecoveryCause.WRONG_SCREEN,XScreen.UNKNOWN,false).action)
 }
 @Test fun reconnectRelaunchesX() {
   assertEquals(RecoveryAction.RELAUNCH_X,RecoveryEngine2().decide(RecoveryCause.SERVICE_RECONNECTED,XScreen.UNKNOWN,false).action)
 }
}
