package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class SourceFallback26_57Test {
    private fun row(handle: String, top: Int) = NodeSnapshot(handle, null, null, "TextView", false, true,
        Rect().apply { left=120; right=700; this.top=top; bottom=top+70 })
    @Test fun exhaustedFirstFollowerAdvancesToSecond() {
        val rows=listOf(row("@first",200),row("@second",300),row("@third",400))
        assertEquals("second",RecentFollowerSelector.orderedHandles(rows,setOf("first")).first())
    }
    @Test fun exhaustedSecondFollowerAdvancesToThird() {
        val rows=listOf(row("@third",400),row("@first",200),row("@second",300))
        assertEquals("third",RecentFollowerSelector.orderedHandles(rows,setOf("first","second")).first())
    }
    @Test fun ownOnlyVerifiedListCannotBecomeAnotherSource() {
        assertTrue(VerifiedSourcePolicy.visible(listOf(row("@own",200)),"own",emptySet()).isEmpty())
    }
    @Test fun resettingOperationClearsFallbackCheckpoint() {
        val cls=AutomationController.javaClass
        val source=cls.getDeclaredField("exhaustedSourceHandle").apply { isAccessible=true }
        val opened=cls.getDeclaredField("fallbackFollowersOpened").apply { isAccessible=true }
        source.set(AutomationController,"first");opened.setBoolean(AutomationController,true)
        cls.getDeclaredMethod("resetOperationNavigation").apply { isAccessible=true }.invoke(AutomationController)
        assertNull(source.get(AutomationController));assertFalse(opened.getBoolean(AutomationController))
    }
}
