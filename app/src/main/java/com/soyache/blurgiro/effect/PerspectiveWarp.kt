package com.soyache.blurgiro.effect

import kotlin.math.hypot
import kotlin.math.max

/**
 * Trapecio sutil: el lado que se aleja se encoge (la pantalla «gira» un poco).
 * De frente (tilt ≈ 0) el quad es la identidad: no hay warp.
 *
 * tiltX > 0 = giro en X hacia la derecha → el lado izquierdo se aleja.
 */
object PerspectiveWarp {

    const val MAX_RECESS = 0.055f
    const val Y_WEIGHT = 0.45f

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

    /**
     * Mapea un punto normalizado (u,v) 0..1 por el mismo trapecio.
     * Sirve para tests del warp sin Canvas.
     */
    fun mapPoint(
        u: Float,
        v: Float,
        tiltX: Float,
        tiltY: Float,
        intensity: Float,
    ): Pair<Float, Float> {
        val q = destQuad(1f, 1f, tiltX, tiltY, intensity)
        val x = bilinear(q[0], q[2], q[6], q[4], u, v)
        val y = bilinear(q[1], q[3], q[7], q[5], u, v)
        return x to y
    }

    private fun bilinear(
        p00: Float,
        p10: Float,
        p01: Float,
        p11: Float,
        u: Float,
        v: Float,
    ): Float {
        val a = p00 + (p10 - p00) * u
        val b = p01 + (p11 - p01) * u
        return a + (b - a) * v
    }
}
