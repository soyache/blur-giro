package com.soyache.blurgiro.effect

import com.soyache.blurgiro.data.BlurMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StripBlurTest {

    @Test
    fun restHidesEveryStrip() {
        val plan = StripBlur.plan(1080, 2400, 0f, 0f, 1f, BlurMode.DIRECTIONAL)
        assertEquals(StripBlur.WINDOW_COUNT, plan.size)
        assertFalse(StripBlur.anyVisible(plan))
        assertTrue(plan.all { it.radiusPx == 0 })
        val corners = StripBlur.plan(1080, 2400, 0f, 0f, 1f, BlurMode.CORNERS)
        assertFalse(StripBlur.anyVisible(corners))
    }

    @Test
    fun yawRightBlursLeftMoreThanRight() {
        val radii = StripBlur.radii(0.9f, 0f, 0.8f, BlurMode.DIRECTIONAL)
        val left = radii.take(3).average()
        val right = radii.takeLast(3).average()
        assertTrue("izq=$left der=$right (giro X derecha → lejos es la izquierda)", left > right + 8)
        assertTrue("lado cercano casi nítido $right", right < 8)
        assertTrue("hay blur a la izquierda", left > 12)
    }

    @Test
    fun yawLeftBlursRightMoreThanLeft() {
        val radii = StripBlur.radii(-0.9f, 0f, 0.8f, BlurMode.DIRECTIONAL)
        val left = radii.take(3).average()
        val right = radii.takeLast(3).average()
        assertTrue("izq=$left der=$right", right > left + 8)
    }

    @Test
    fun intensityScalesRadius() {
        val soft = StripBlur.radii(1f, 0f, 0.2f, BlurMode.DIRECTIONAL).max()
        val hard = StripBlur.radii(1f, 0f, 1f, BlurMode.DIRECTIONAL).max()
        assertTrue("suave=$soft fuerte=$hard", hard > soft)
        assertTrue(hard <= StripBlur.MAX_RADIUS_PX)
    }

    @Test
    fun farCornerCellGetsMoreBlur() {
        val radii = StripBlur.radii(1f, -1f, 1f, BlurMode.CORNERS)
        val (cols, rows) = StripBlur.gridSize(BlurMode.CORNERS)
        assertEquals(4, cols)
        assertEquals(2, rows)
        val bottomLeft = radii[cols]
        val topRight = radii[cols - 1]
        assertTrue("abajo-izq=$bottomLeft arriba-der=$topRight", bottomLeft > topRight)
    }

    @Test
    fun yawRightInsetsLeftEdge() {
        val insets = StripBlur.insets(1000, 2000, 0.9f, 0f, 1f)
        assertTrue("inset izq=${insets[0]}", insets[0] > insets[2])
        val rest = StripBlur.insets(1000, 2000, 0f, 0f, 1f)
        assertEquals(0, rest[0])
        assertEquals(0, rest[1])
        assertEquals(0, rest[2])
        assertEquals(0, rest[3])
    }

    @Test
    fun directionalPlanCoversFullHeightOnVisibleStrips() {
        val plan = StripBlur.plan(1080, 2400, 0.85f, 0f, 0.7f, BlurMode.DIRECTIONAL)
        val visible = plan.filter { it.visible }
        assertTrue(visible.isNotEmpty())
        assertTrue(visible.all { it.height >= 2300 })
        assertTrue(visible.first().x < 200)
    }
}
