package com.soyache.blurgiro.effect

import com.soyache.blurgiro.data.BlurMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurMaskTest {

    @Test
    fun cornersStaySofterInCenterThanAtCorner() {
        val center = BlurMask.sample(0.5f, 0.5f, 0f, 0f, BlurMode.CORNERS)
        val corner = BlurMask.sample(0.02f, 0.02f, 0f, 0f, BlurMode.CORNERS)
        assertTrue("esquina=$corner centro=$center", corner > center)
    }

    @Test
    fun directionalBlursTiltedSide() {
        val right = BlurMask.sample(0.95f, 0.5f, 1f, 0f, BlurMode.DIRECTIONAL)
        val left = BlurMask.sample(0.05f, 0.5f, 1f, 0f, BlurMode.DIRECTIONAL)
        assertTrue("derecha=$right izquierda=$left", right > left)
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
    }
}
