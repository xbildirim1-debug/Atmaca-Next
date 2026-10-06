package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class NonFollowerFollowingInspectorTest {
    private fun rect(top: Int, bottom: Int, left: Int = 0, right: Int = 1080) = Rect().apply {
        this.top = top; this.bottom = bottom; this.left = left; this.right = right
    }
    private fun node(parent: Int?, text: String?, bounds: Rect, selected: Boolean = false,
                     clickable: Boolean = false, visible: Boolean = true, clazz: String = "android.view.View") =
        NonFollowerFollowingInspector.TreeNode(NodeSnapshot(text, null, null, clazz, clickable, true, bounds,
            selected = selected, visible = visible), parent)
    private fun tree() = mutableListOf(node(null, null, rect(0, 2300)),
        node(0, "Takip ediliyor", rect(180, 260), selected = true), node(0, null, rect(260, 2250)))
    private fun row(tree: MutableList<NonFollowerFollowingInspector.TreeNode>, handle: String, top: Int = 350,
                    badge: Boolean = false, name: Boolean = true): Int {
        val index = tree.size
        tree += node(2, null, rect(top, top + 220))
        if (name) tree += node(index, "Person", rect(top + 30, top + 50, 160, 800))
        tree += node(index, "@$handle", rect(top + 70, top + 90, 160, 800))
        tree += node(index, "Takip ediliyor", rect(top + 30, top + 95, 820, 1050), clickable = true, clazz = "android.widget.Button")
        if (badge) tree += node(index, "Seni takip ediyor", rect(top + 5, top + 25, 160, 800))
        return index
    }

    @Test fun cachedCompleteSinglePersonRowStillProvesNonMutualStatus() {
        val tree = tree(); row(tree, "older")
        val rows = NonFollowerFollowingInspector.inspectCaptured(tree)
        assertEquals(1, rows.size)
        assertEquals(NonFollowerRelationship.DOES_NOT_FOLLOW, NonFollowerPolicy.relationship(rows.single().evidence))
    }

    @Test fun ownBadgeStillProtectsTheCachedRow() {
        val tree = tree(); row(tree, "mutual", badge = true)
        assertEquals(NonFollowerRelationship.FOLLOWS_YOU,
            NonFollowerPolicy.relationship(NonFollowerFollowingInspector.inspectCaptured(tree).single().evidence))
    }

    @Test fun siblingBadgeIsAssociatedOnlyWithItsActualPerson() {
        val tree = tree(); row(tree, "older")
        tree += node(2, "Seni takip ediyor", rect(580, 600, 160, 800))
        row(tree, "mutual", top = 610)
        val rows = NonFollowerFollowingInspector.inspectCaptured(tree)
        assertEquals(2, rows.size)
        assertEquals(NonFollowerRelationship.DOES_NOT_FOLLOW, NonFollowerPolicy.relationship(rows[0].evidence))
        assertEquals(NonFollowerRelationship.FOLLOWS_YOU, NonFollowerPolicy.relationship(rows[1].evidence))
    }

    @Test fun mergedPeopleCannotTurnBadgeAbsenceIntoAnAction() {
        val tree = tree(); val owner = row(tree, "older")
        tree += node(owner, "@neighbor", rect(480, 500, 160, 800))
        assertTrue(NonFollowerFollowingInspector.inspectCaptured(tree).isEmpty())
    }

    @Test fun clippedTopIsStillUnknown() {
        val tree = tree(); row(tree, "older", top = 220)
        val evidence = NonFollowerFollowingInspector.inspectCaptured(tree).single().evidence
        assertFalse(evidence.complete)
        assertEquals(NonFollowerRelationship.UNKNOWN, NonFollowerPolicy.relationship(evidence))
    }

    @Test fun missingNameStillCannotProveAnAbsentBadge() {
        val tree = tree(); row(tree, "older", name = false)
        assertEquals(NonFollowerRelationship.UNKNOWN,
            NonFollowerPolicy.relationship(NonFollowerFollowingInspector.inspectCaptured(tree).single().evidence))
    }

    @Test fun truncatedCaptureStillCannotProveBadgeAbsence() {
        val tree = tree(); row(tree, "older")
        while (tree.size < 2000) tree += node(0, null, rect(0, 0))
        val evidence = NonFollowerFollowingInspector.inspectCaptured(tree).single().evidence
        assertFalse(evidence.treeComplete)
        assertEquals(NonFollowerRelationship.UNKNOWN, NonFollowerPolicy.relationship(evidence))
    }

    @Test fun invalidParentGraphCannotProduceAnAction() {
        val tree = tree(); row(tree, "older")
        tree += node(9999, "@unseen", rect(1700, 1750))
        assertTrue(NonFollowerFollowingInspector.inspectCaptured(tree).isEmpty())
    }
}
