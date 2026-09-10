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
    fun planTilesFullScreen() {
        val plan = StripBlur.plan(1080, 2400, 0.85f, 0f, 0.8f, BlurMode.DIRECTIONAL)
        val frame = StripBlur.coverage(plan)
        assertEquals(0, frame[0])
        assertEquals(0, frame[1])
        assertEquals(1080, frame[2])
        assertEquals(2400, frame[3])
        assertTrue(plan.all { it.height >= 2390 })
        val corners = StripBlur.plan(1080, 2400, 0.85f, -0.6f, 0.8f, BlurMode.CORNERS)
        val cover = StripBlur.coverage(corners)
        assertEquals(0, cover[0])
        assertEquals(0, cover[1])
        assertEquals(1080, cover[2])
        assertEquals(2400, cover[3])
    }

    @Test
    fun yawRightIsGradualAcrossFullWidth() {
        val radii = StripBlur.radii(0.9f, 0f, 0.8f, BlurMode.DIRECTIONAL)
        val left = radii.take(4).average()
        val mid = radii.drop(6).take(4).average()
        val right = radii.takeLast(4).average()
        assertTrue("izq=$left medio=$mid der=$right", left > mid + 4 && mid > right)
        assertTrue("lado cercano casi nítido $right", right < 12)
        assertTrue("lado lejano con blur $left", left > 20)
        for (i in 0 until radii.size - 1) {
            assertTrue(
                "rampa no monótona en $i: ${radii[i]} → ${radii[i + 1]}",
                radii[i] + 1 >= radii[i + 1],
            )
        }
        val visible = StripBlur.plan(1080, 2400, 0.9f, 0f, 0.8f, BlurMode.DIRECTIONAL)
            .filter { it.visible }
        assertTrue("bandas visibles=${visible.size}", visible.size >= 10)
        val span = visible.maxOf { it.x + it.width } - visible.minOf { it.x }
        assertTrue("el degradado cubre $span px", span >= 1080 * 0.70f)
    }

    @Test
    fun yawLeftBlursRightMoreThanLeft() {
        val radii = StripBlur.radii(-0.9f, 0f, 0.8f, BlurMode.DIRECTIONAL)
        val left = radii.take(4).average()
        val right = radii.takeLast(4).average()
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
        assertEquals(8, cols)
        assertEquals(2, rows)
        val bottomLeft = radii[cols]
        val topRight = radii[cols - 1]
        assertTrue("abajo-izq=$bottomLeft arriba-der=$topRight", bottomLeft > topRight)
    }

    @Test
    fun enoughStripsForSmoothRamp() {
        assertTrue(StripBlur.WINDOW_COUNT >= 16)
        assertEquals(16, StripBlur.gridSize(BlurMode.DIRECTIONAL).first)
    }
}
