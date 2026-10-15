package com.atmacanext.app.ai

import com.atmacanext.app.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class PilotThemes27_67Test {
    @Test fun userAndPilotAreDistinctInAllThreeModes() {
        for (mode in AppThemeMode.entries) {
            AtmacaThemeState.apply(mode)
            val mine = PilotBubbleColors.current(true); val pilot = PilotBubbleColors.current(false)
            assertNotEquals(mode.name, mine.first, pilot.first)
            assertNotEquals(mine.first, mine.second); assertNotEquals(pilot.first, pilot.second)
        }
        AtmacaThemeState.apply(AppThemeMode.LIGHT)
    }
}
