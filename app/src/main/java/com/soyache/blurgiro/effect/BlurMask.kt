package com.soyache.blurgiro.effect

import com.soyache.blurgiro.data.BlurMode
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Máscara 0..1 del cristal: 0 = nítido, 1 = máximo desenfoque del *contenido*.
 *
 * El lado que se **aleja** (el opuesto al giro) se va de foco.
 * Héctor: giro en X hacia la derecha → la izquierda se pone un poco blur;
 * de frente se quita el blur de todos los lugares.
 *
 * tiltX > 0 = derecha más cerca; el far side es la izquierda.
 * La Y pesa menos ([PerspectiveWarp.Y_WEIGHT]) para priorizar el eje X.
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
        val ty = (tiltY * PerspectiveWarp.Y_WEIGHT).coerceIn(-1f, 1f)
        val tiltMag = hypot(tx, ty)

        return when (mode) {
            BlurMode.CORNERS -> corners(px, py, tx, ty, tiltMag)
            BlurMode.DIRECTIONAL -> directional(px, py, tx, ty, tiltMag)
        }.coerceIn(0f, 1f)
    }

    fun cornerStrengths(tiltX: Float, tiltY: Float): CornerStrengths {
        val tx = tiltX.coerceIn(-1f, 1f)
        val ty = (tiltY * PerspectiveWarp.Y_WEIGHT).coerceIn(-1f, 1f)
        return CornerStrengths(
            topLeft = cornerInfluence(tx, ty, -1f, -1f),
            topRight = cornerInfluence(tx, ty, 1f, -1f),
            bottomLeft = cornerInfluence(tx, ty, -1f, 1f),
            bottomRight = cornerInfluence(tx, ty, 1f, 1f),
        )
    }

    /**
     * Lado que se aleja (el que debe desenfocarse).
     * 0 = izquierda, 1 = arriba, 2 = derecha, 3 = abajo.
     */
    fun farSide(tiltX: Float, tiltY: Float): Int {
        val ty = tiltY * PerspectiveWarp.Y_WEIGHT
        return if (abs(tiltX) >= abs(ty)) {
            if (tiltX >= 0f) 0 else 2
        } else {
            if (ty >= 0f) 1 else 3
        }
    }

    fun directionalStrength(tiltX: Float, tiltY: Float): Float {
        return hypot(tiltX, tiltY * PerspectiveWarp.Y_WEIGHT).coerceIn(0f, 1f)
    }

    fun tiltEngage(tiltMag: Float): Float = smoothstep(ENGAGE_START, ENGAGE_FULL, tiltMag)

    fun effectAmount(tiltX: Float, tiltY: Float, intensity: Float): Float {
        val mag = hypot(tiltX.coerceIn(-1f, 1f), tiltY.coerceIn(-1f, 1f) * PerspectiveWarp.Y_WEIGHT)
        return (tiltEngage(mag) * intensity.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    }

    private fun corners(
        px: Float,
        py: Float,
        tx: Float,
        ty: Float,
        tiltMag: Float,
    ): Float {
        val engage = tiltEngage(tiltMag)
        if (engage <= 0.001f) return 0f

        val focusX = tx * 0.55f
        val focusY = ty * 0.55f
        val dist = hypot(px - focusX, py - focusY)
        val dof = smoothstep(0.38f, 1.22f, dist)
        return (dof * engage).coerceIn(0f, 1f)
    }

    private fun directional(px: Float, py: Float, tx: Float, ty: Float, tiltMag: Float): Float {
        val engage = tiltEngage(tiltMag)
        if (engage <= 0.001f) return 0f
        val dirX = tx / tiltMag
        val dirY = ty / tiltMag
        val towardCloser = px * dirX + py * dirY
        val towardFar = -towardCloser
        val band = smoothstep(-0.18f, 0.88f, towardFar)
        return (band * engage).coerceIn(0f, 1f)
    }

    private fun cornerInfluence(tiltX: Float, tiltY: Float, cornerX: Float, cornerY: Float): Float {
        val tiltMag = hypot(tiltX, tiltY)
        val engage = tiltEngage(tiltMag)
        if (engage <= 0.001f) return 0f
        val alignment = (-tiltX * cornerX + -tiltY * cornerY) * 0.5f
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
