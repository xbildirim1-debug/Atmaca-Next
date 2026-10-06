package com.atmacanext.app.automation

import org.junit.Assert.*
import org.junit.Test

class NonFollowerTimingTest {
    @Test fun newTaskKeepsBusesFastFloorAndNominalDivisor() {
        assertEquals(16L,NonFollowerTiming.scaleDelay(450L,true))
        assertEquals(100L,NonFollowerTiming.scaleDelay(7000L,true))
    }
    @Test fun existingTasksContinueUsingUserTuning() {
        val original=AutomationTuning.betweenActionsMs
        try {
            AutomationTuning.betweenActionsMs=500L
            assertEquals(2000L,NonFollowerTiming.scaleDelay(2000L,false))
            AutomationTuning.betweenActionsMs=71L
            assertEquals(AutomationTuning.scaleDelay(2000L),NonFollowerTiming.scaleDelay(2000L,false))
        } finally {AutomationTuning.betweenActionsMs=original}
    }
}
