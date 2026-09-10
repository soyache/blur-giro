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
        assertEquals(0f, BlurMask.effectAmount(0f, 0f, 1f), 0.001f)
    }

    @Test
    fun yawRightBlursLeftSide() {
        val left = BlurMask.sample(0.05f, 0.5f, 0.9f, 0f, BlurMode.DIRECTIONAL)
        val mid = BlurMask.sample(0.50f, 0.5f, 0.9f, 0f, BlurMode.DIRECTIONAL)
        val right = BlurMask.sample(0.95f, 0.5f, 0.9f, 0f, BlurMode.DIRECTIONAL)
        assertTrue("izquierda=$left medio=$mid derecha=$right", left > mid && mid > right)
        assertTrue("izquierda=$left derecha=$right (giro X a la derecha → lejos es la izquierda)", left > right + 0.25f)
    }

    @Test
    fun yawLeftBlursRightSide() {
        val left = BlurMask.sample(0.05f, 0.5f, -0.9f, 0f, BlurMode.DIRECTIONAL)
        val right = BlurMask.sample(0.95f, 0.5f, -0.9f, 0f, BlurMode.DIRECTIONAL)
        assertTrue("izquierda=$left derecha=$right", right > left + 0.25f)
    }

    @Test
    fun cornersDefocusFarCorner() {
        val far = BlurMask.sample(0.04f, 0.96f, 1f, -1f, BlurMode.CORNERS)
        val near = BlurMask.sample(0.96f, 0.04f, 1f, -1f, BlurMode.CORNERS)
        assertTrue("lejos=$far cerca=$near", far > near + 0.12f)
    }

    @Test
    fun farSideFollowsYaw() {
        assertEquals(0, BlurMask.farSide(0.8f, 0.1f))
        assertEquals(2, BlurMask.farSide(-0.8f, 0.1f))
    }

    @Test
    fun farCornerGetsMoreStrength() {
        val corners = BlurMask.cornerStrengths(1f, -1f)
        assertTrue(corners.bottomLeft > corners.topRight)
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
