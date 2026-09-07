package com.atmacanext.app.automation

/** Associate a username with a right-hand relationship button, never a neighboring row/tab. */
internal object RelationshipRowGeometry {
    fun matches(hl: Int, ht: Int, hr: Int, hb: Int, bl: Int, bt: Int, br: Int, bb: Int): Boolean {
        if (hr <= hl || hb <= ht || br <= bl || bb <= bt) return false
        if (hl >= bl || hr > br) return false
        val overlap = minOf(hb, bb) - maxOf(ht, bt)
        return overlap > 0
    }
}
