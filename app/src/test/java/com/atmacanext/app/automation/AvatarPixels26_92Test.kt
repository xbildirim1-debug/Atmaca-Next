package com.atmacanext.app.automation

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarPixels26_92Test {
    @Test fun colorfulBlockBeatsTheHandleText() {
        val width = 120
        val height = 90
        val pixels = IntArray(width * height) { 0xFFFFFFFF.toInt() }
        for (y in 18 until 62) {
            for (x in 8 until 52) {
                val shift = (x + y) % 3
                pixels[y * width + x] = when (shift) {
                    0 -> 0xFF2244AA.toInt()
                    1 -> 0xFFCC5533.toInt()
                    else -> 0xFF228855.toInt()
                }
            }
        }
        for (x in 60 until 110) pixels[40 * width + x] = 0xFF222222.toInt()
        val box = AvatarPixels.square(pixels, width, height, 28, 40)
        assertNotNull(box)
        assertTrue(box!![0] < 30)
        assertTrue(box[1] < 40)
    }
}
