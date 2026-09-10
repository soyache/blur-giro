package com.soyache.blurgiro.effect

import com.soyache.blurgiro.data.BlurMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurMaskTest {

    @Test
    fun restIsAlmostClear() {
        val center = BlurMask.sample(0.5f, 0.5f, 0f, 0f, BlurMode.CORNERS)
        val corner = BlurMask.sample(0.02f, 0.02f, 0f, 0f, BlurMode.CORNERS)
        val dirEdge = BlurMask.sample(0.95f, 0.5f, 0f, 0f, BlurMode.DIRECTIONAL)
        assertTrue("centro en reposo=$center", center < 0.04f)
        assertTrue("esquina en reposo=$corner", corner < 0.04f)
        assertTrue("borde direccional en reposo=$dirEdge", dirEdge < 0.04f)
    }

    @Test
    fun cornersDefocusTiltedCornerAndKeepOppositeSharp() {
        val far = BlurMask.sample(0.96f, 0.04f, 1f, -1f, BlurMode.CORNERS)
        val near = BlurMask.sample(0.04f, 0.96f, 1f, -1f, BlurMode.CORNERS)
        val center = BlurMask.sample(0.5f, 0.5f, 1f, -1f, BlurMode.CORNERS)
        assertTrue("lejos=$far cerca=$near", far > near + 0.18f)
        assertTrue("lejos=$far centro=$center", far > center)
    }

    @Test
    fun directionalBlursTiltedSide() {
        val right = BlurMask.sample(0.95f, 0.5f, 1f, 0f, BlurMode.DIRECTIONAL)
        val left = BlurMask.sample(0.05f, 0.5f, 1f, 0f, BlurMode.DIRECTIONAL)
        assertTrue("derecha=$right izquierda=$left", right > left + 0.25f)
    }

    @Test
    fun dominantSideFollowsTilt() {
        assertEquals(2, BlurMask.dominantSide(0.8f, 0.1f))
        assertEquals(0, BlurMask.dominantSide(-0.8f, 0.1f))
        assertEquals(3, BlurMask.dominantSide(0.1f, 0.8f))
        assertEquals(1, BlurMask.dominantSide(0.1f, -0.8f))
    }

    @Test
    fun farCornerGetsMoreStrength() {
        val corners = BlurMask.cornerStrengths(1f, -1f)
        assertTrue(corners.topRight > corners.bottomLeft)
        val rest = BlurMask.cornerStrengths(0f, 0f)
        assertTrue(rest.topLeft < 0.05f)
        assertTrue(rest.topRight < 0.05f)
        assertTrue(rest.bottomLeft < 0.05f)
        assertTrue(rest.bottomRight < 0.05f)
    }

    @Test
    fun directionalStrengthIsZeroAtRest() {
        assertEquals(0f, BlurMask.directionalStrength(0f, 0f), 0.001f)
    }
}
