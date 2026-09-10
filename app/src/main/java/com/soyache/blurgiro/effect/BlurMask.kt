package com.soyache.blurgiro.effect

import com.soyache.blurgiro.data.BlurMode
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Máscara 0..1 del cristal: 0 = nítido, 1 = máximo desenfoque / niebla.
 *
 * El foco se desplaza con la inclinación (DOF poco profundo):
 * el lado o las esquinas hacia los que se inclina el teléfono se van de foco.
 * En reposo (plano) la máscara queda casi en cero: sin velo a pantalla completa.
 */
object BlurMask {

    data class CornerStrengths(
        val topLeft: Float,
        val topRight: Float,
        val bottomLeft: Float,
        val bottomRight: Float,
    )

    fun sample(u: Float, v: Float, tiltX: Float, tiltY: Float, mode: BlurMode): Float {
        val px = u * 2f - 1f
        val py = v * 2f - 1f
        val tx = tiltX.coerceIn(-1f, 1f)
        val ty = tiltY.coerceIn(-1f, 1f)
        val tiltMag = hypot(tx, ty)

        return when (mode) {
            BlurMode.CORNERS -> corners(px, py, u, v, tx, ty, tiltMag)
            BlurMode.DIRECTIONAL -> directional(px, py, tx, ty, tiltMag)
        }.coerceIn(0f, 1f)
    }

    fun cornerStrengths(tiltX: Float, tiltY: Float): CornerStrengths {
        val tx = tiltX.coerceIn(-1f, 1f)
        val ty = tiltY.coerceIn(-1f, 1f)
        return CornerStrengths(
            topLeft = cornerInfluence(tx, ty, -1f, -1f),
            topRight = cornerInfluence(tx, ty, 1f, -1f),
            bottomLeft = cornerInfluence(tx, ty, -1f, 1f),
            bottomRight = cornerInfluence(tx, ty, 1f, 1f),
        )
    }

    /**
     * Lado dominante para el modo direccional.
     * 0 = izquierda, 1 = arriba, 2 = derecha, 3 = abajo.
     */
    fun dominantSide(tiltX: Float, tiltY: Float): Int {
        return if (abs(tiltX) >= abs(tiltY)) {
            if (tiltX >= 0f) 2 else 0
        } else {
            if (tiltY >= 0f) 3 else 1
        }
    }

    fun directionalStrength(tiltX: Float, tiltY: Float): Float {
        return hypot(tiltX, tiltY).coerceIn(0f, 1f)
    }

    fun tiltEngage(tiltMag: Float): Float = smoothstep(ENGAGE_START, ENGAGE_FULL, tiltMag)

    private fun corners(
        px: Float,
        py: Float,
        u: Float,
        v: Float,
        tx: Float,
        ty: Float,
        tiltMag: Float,
    ): Float {
        val engage = tiltEngage(tiltMag)
        if (engage <= 0.001f) return 0f

        val focusX = -tx * 0.55f
        val focusY = -ty * 0.55f
        val dist = hypot(px - focusX, py - focusY)
        val dof = smoothstep(0.42f, 1.28f, dist)

        val cx = abs(u * 2f - 1f)
        val cy = abs(v * 2f - 1f)
        val cornerness = (cx * cy).let { it * it }
        val toward = max(0f, px * tx + py * ty)
        val far = if (tiltMag > 0.02f) {
            val dx = px / (hypot(px, py) + 1e-4f)
            val dy = py / (hypot(px, py) + 1e-4f)
            max(0f, dx * (tx / tiltMag) + dy * (ty / tiltMag))
        } else {
            0f
        }

        return (
            dof * 0.58f +
                toward * 0.42f * (0.30f + 0.70f * cornerness) +
                far * 0.22f
            ).coerceIn(0f, 1f) * engage
    }

    private fun directional(px: Float, py: Float, tx: Float, ty: Float, tiltMag: Float): Float {
        val engage = tiltEngage(tiltMag)
        if (engage <= 0.001f) return 0f
        val dirX = tx / tiltMag
        val dirY = ty / tiltMag
        val projected = px * dirX + py * dirY
        val band = smoothstep(-0.12f, 0.90f, projected)
        val edge = smoothstep(0.55f, 1.02f, max(abs(px), abs(py)))
        return max(band, edge * 0.12f * engage) * engage
    }

    private fun cornerInfluence(tiltX: Float, tiltY: Float, cornerX: Float, cornerY: Float): Float {
        val tiltMag = hypot(tiltX, tiltY)
        val engage = tiltEngage(tiltMag)
        if (engage <= 0.001f) return 0f
        val alignment = (tiltX * cornerX + tiltY * cornerY) * 0.5f
        return (smoothstep(0.04f, 0.88f, alignment) * engage).coerceIn(0f, 1f)
    }

    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val span = edge1 - edge0
        if (span == 0f) return if (x >= edge1) 1f else 0f
        val t = ((x - edge0) / span).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun clamp01(v: Float): Float = min(1f, max(0f, v))

    const val ENGAGE_START = 0.06f
    const val ENGAGE_FULL = 0.34f
}
