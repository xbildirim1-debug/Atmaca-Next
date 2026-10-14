package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class AutomationTuningTest {
    @Test fun actionSettingScalesEveryRuntimeStep() {
        val before = AutomationTuning.betweenActionsMs
        try {
            AutomationTuning.betweenActionsMs = 500L
            assertEquals(90L, AutomationTuning.scaleDelay(900L))
            AutomationTuning.betweenActionsMs = 250L
            assertEquals(45L, AutomationTuning.scaleDelay(900L))
            AutomationTuning.betweenActionsMs = 100L
            assertEquals(18L, AutomationTuning.scaleDelay(900L))
        } finally {
            AutomationTuning.betweenActionsMs = before
        }
    }

    @Test fun veryShortRuntimeStepKeepsAccessibilitySettleFloor() {
        val before = AutomationTuning.betweenActionsMs
        try {
            AutomationTuning.betweenActionsMs = 100L
            assertEquals(16L, AutomationTuning.scaleDelay(120L))
        } finally {
            AutomationTuning.betweenActionsMs = before
        }
    }
}
