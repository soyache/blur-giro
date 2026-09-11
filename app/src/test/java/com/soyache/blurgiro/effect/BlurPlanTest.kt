package com.soyache.blurgiro.effect

import com.soyache.blurgiro.data.BlurMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurPlanTest {

    @Test
    fun restHidesEveryColumn() {
        val radii = BlurPlan.columnRadii(0f, 0f, 1f, BlurMode.DIRECTIONAL)
        assertFalse(BlurPlan.anyVisible(radii))
        assertTrue(radii.all { it == 0 })
    }

    @Test
    fun yawRightIsGradualAcrossFullWidth() {
        val radii = BlurPlan.columnRadii(0.9f, 0f, 0.8f, BlurMode.DIRECTIONAL)
        val left = radii.take(4).average()
        val mid = radii.drop(6).take(4).average()
        val right = radii.takeLast(4).average()
        assertTrue("izq=$left medio=$mid der=$right", left > mid && mid > right)
        assertTrue("lado cercano casi nítido $right", right < 4)
        assertTrue("lado lejano con blur $left", left > 6)
        for (i in 0 until radii.size - 1) {
            assertTrue(
                "rampa no monótona en $i: ${radii[i]} → ${radii[i + 1]}",
                radii[i] + 1 >= radii[i + 1],
            )
        }
    }

    @Test
    fun yawLeftBlursRightMoreThanLeft() {
        val radii = BlurPlan.columnRadii(-0.9f, 0f, 0.8f, BlurMode.DIRECTIONAL)
        val left = radii.take(4).average()
        val right = radii.takeLast(4).average()
        assertTrue("izq=$left der=$right", right > left + 4)
    }

    @Test
    fun intensityScalesRadius() {
        val soft = BlurPlan.columnRadii(1f, 0f, 0.2f, BlurMode.DIRECTIONAL).max()
        val hard = BlurPlan.columnRadii(1f, 0f, 1f, BlurMode.DIRECTIONAL).max()
        assertTrue("suave=$soft fuerte=$hard", hard > soft)
        assertTrue(hard <= BlurPlan.MAX_RADIUS_PX)
    }
}
