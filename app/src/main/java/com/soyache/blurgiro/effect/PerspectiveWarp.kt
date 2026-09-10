package com.soyache.blurgiro.effect

import kotlin.math.hypot
import kotlin.math.max

/**
 * Trapecio sutil: el lado que se aleja se encoge (la pantalla «gira» un poco).
 * En frente (tilt ≈ 0) el quad es la identidad: no hay warp.
 *
 * tiltX > 0 = giro en X hacia la derecha → el lado izquierdo se aleja.
 */
object PerspectiveWarp {

    const val MAX_RECESS = 0.055f

    fun destQuad(
        width: Float,
        height: Float,
        tiltX: Float,
        tiltY: Float,
        intensity: Float,
    ): FloatArray {
        val tx = tiltX.coerceIn(-1f, 1f)
        val ty = (tiltY * Y_WEIGHT).coerceIn(-1f, 1f)
        val engage = BlurMask.tiltEngage(hypot(tx, ty))
        val src = identityQuad(width, height)
        if (engage <= 0.001f || width <= 0f || height <= 0f) return src

        val k = MAX_RECESS * engage * intensity.coerceIn(0.15f, 1f)
        val left = max(0f, tx) * k
        val right = max(0f, -tx) * k
        val top = max(0f, ty) * k * 0.72f
        val bottom = max(0f, -ty) * k * 0.72f

        val insetL = left * width
        val insetR = right * width
        val shrinkL = left * height * 0.48f
        val shrinkR = right * height * 0.48f
        val insetT = top * height
        val insetB = bottom * height
        val shrinkT = top * width * 0.40f
        val shrinkB = bottom * width * 0.40f

        return floatArrayOf(
            0f + insetL + shrinkT, 0f + shrinkL + insetT,
            width - insetR - shrinkT, 0f + shrinkR + insetT,
            width - insetR - shrinkB, height - shrinkR - insetB,
            0f + insetL + shrinkB, height - shrinkL - insetB,
        )
    }

    fun identityQuad(width: Float, height: Float): FloatArray =
        floatArrayOf(0f, 0f, width, 0f, width, height, 0f, height)

    fun maxCornerDelta(quad: FloatArray, width: Float, height: Float): Float {
        val id = identityQuad(width, height)
        var max = 0f
        for (i in quad.indices) {
            val d = kotlin.math.abs(quad[i] - id[i])
            if (d > max) max = d
        }
        return max
    }

    const val Y_WEIGHT = 0.45f
}
