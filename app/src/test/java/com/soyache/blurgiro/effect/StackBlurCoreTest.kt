package com.soyache.blurgiro.effect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class StackBlurCoreTest {

    @Test
    fun blurMixesASharpImpulse() {
        val w = 32
        val h = 24
        val src = IntArray(w * h) { 0xFF101010.toInt() }
        src[h / 2 * w + w / 2] = 0xFFFFFFFF.toInt()
        val out = StackBlurCore.blur(src, w, h, 3)
        val center = out[h / 2 * w + w / 2]
        val neighbor = out[h / 2 * w + w / 2 + 2]
        val cr = (center shr 16) and 0xff
        val nr = (neighbor shr 16) and 0xff
        assertTrue("el centro sigue más claro $cr vs $nr", cr > nr)
        assertTrue("el vecino ya no es negro $nr", nr > 16)
        assertTrue(cr < 255)
    }

    @Test
    fun lerpPixelEnds() {
        val a = 0xFF112233.toInt()
        val b = 0xFFAABBCC.toInt()
        assertEquals(a, StackBlurCore.lerpPixel(a, b, 0f))
        assertEquals(b, StackBlurCore.lerpPixel(a, b, 1f))
        val mid = StackBlurCore.lerpPixel(a, b, 0.5f)
        assertEquals(0xFF, mid ushr 24)
        assertEquals(0x5D, (mid shr 16) and 0xff)
    }

    @Test
    fun compositeFollowsMask() {
        val sharp = intArrayOf(0xFF000000.toInt(), 0xFF000000.toInt())
        val mid = intArrayOf(0xFF808080.toInt(), 0xFF808080.toInt())
        val heavy = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
        val out = StackBlurCore.composite(sharp, mid, heavy, floatArrayOf(0f, 1f))
        assertEquals(sharp[0], out[0])
        val far = (out[1] shr 16) and 0xff
        assertTrue("lado con máscara 1 se acerca al heavy $far", far > 200)
        assertTrue(abs(((out[1] shr 8) and 0xff) - far) < 2)
    }
}
