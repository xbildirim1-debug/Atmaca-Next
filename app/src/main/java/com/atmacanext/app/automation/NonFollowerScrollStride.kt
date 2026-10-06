package com.atmacanext.app.automation

/** Advance most of the viewport while retaining two complete old rows for person counting. */
internal object NonFollowerScrollStride {
    fun distance(contentTop: Int, contentBottom: Int, orderedRowTops: List<Int>): Float {
        val span = contentBottom - contentTop
        if (span < 32 || orderedRowTops.isEmpty()) return 0f
        if (orderedRowTops.size == 1) return span * .40f
        val retained = if (orderedRowTops.size >= 3) 2 else 1
        val overlapLimit = orderedRowTops[orderedRowTops.size - retained] - contentTop
        return minOf(span * .72f, overlapLimit.toFloat()).coerceAtLeast(0f)
    }
}
