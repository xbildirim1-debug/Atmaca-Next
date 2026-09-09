package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class AutomationTuningTest {
    @Test fun actionSettingScalesEveryRuntimeStep() {
        val before = AutomationTuning.betweenActionsMs
        try {
            AutomationTuning.betweenActionsMs = 500L
            assertEquals(900L, AutomationTuning.scaleDelay(900L))
            AutomationTuning.betweenActionsMs = 250L
            assertEquals(450L, AutomationTuning.scaleDelay(900L))
            AutomationTuning.betweenActionsMs = 100L
            assertEquals(180L, AutomationTuning.scaleDelay(900L))
        } finally {
            AutomationTuning.betweenActionsMs = before
        }
    }

    @Test fun veryShortRuntimeStepKeepsAccessibilitySettleFloor() {
        val before = AutomationTuning.betweenActionsMs
        try {
            AutomationTuning.betweenActionsMs = 100L
            assertEquals(60L, AutomationTuning.scaleDelay(120L))
        } finally {
            AutomationTuning.betweenActionsMs = before
        }
    }
}
