package com.soyache.blurgiro.effect

import com.soyache.blurgiro.data.BlurMode
import kotlin.math.roundToInt

/**
 * Radios por columna del cristal que **nosotros** pintamos (wallpaper + iconos).
 * 0 = nítido. Giro X a la derecha → radio alto a la izquierda, degradado hasta ~0.
 */
object BlurPlan {

    const val COLUMNS = 16
    const val MAX_RADIUS_PX = 18
    const val MIN_VISIBLE_RADIUS = 1

    fun columnWeights(tiltX: Float, tiltY: Float, intensity: Float, mode: BlurMode): FloatArray {
        val gain = intensity.coerceIn(0f, 1f)
        return FloatArray(COLUMNS) { col ->
            val u = (col + 0.5f) / COLUMNS
            (BlurMask.sample(u, 0.5f, tiltX, tiltY, mode) * gain).coerceIn(0f, 1f)
        }
    }

    fun columnRadii(tiltX: Float, tiltY: Float, intensity: Float, mode: BlurMode): IntArray {
        val w = columnWeights(tiltX, tiltY, intensity, mode)
        return IntArray(COLUMNS) { i ->
            val r = (w[i] * MAX_RADIUS_PX).roundToInt()
            if (r < MIN_VISIBLE_RADIUS) 0 else r.coerceAtMost(MAX_RADIUS_PX)
        }
    }

    fun anyVisible(radii: IntArray): Boolean = radii.any { it > 0 }
}
