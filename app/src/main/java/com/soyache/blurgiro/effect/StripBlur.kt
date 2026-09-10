package com.soyache.blurgiro.effect

import com.soyache.blurgiro.data.BlurMode
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Una banda (o celda) que pedirá [android.view.Window.setBackgroundBlurRadius]
 * al compositor. Radio 0 = no se muestra: el teléfono se ve normal.
 */
data class StripSpec(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val radiusPx: Int,
) {
    val visible: Boolean get() = radiusPx > 0 && width > 1 && height > 1
}

/**
 * Plan de bandas: varias ventanas finas con radios distintos.
 *
 * Giro X a la derecha (tiltX > 0) → la izquierda se aleja → radios altos a la izquierda.
 * De frente → todos los radios a 0.
 *
 * El inset geométrico es un sesgo mínimo del layout (el cristal «gira» un poco).
 * No captura píxeles ni pinta niebla.
 */
object StripBlur {

    const val WINDOW_COUNT = 8
    const val MAX_RADIUS_PX = 64
    const val MIN_VISIBLE_RADIUS = 3

    private const val DIRECTIONAL_COLS = 8
    private const val DIRECTIONAL_ROWS = 1
    private const val CORNER_COLS = 4
    private const val CORNER_ROWS = 2

    /** Receso máximo del lado lejano (~1.6 % del ancho). */
    const val MAX_RECESS = 0.016f

    fun gridSize(mode: BlurMode): Pair<Int, Int> = when (mode) {
        BlurMode.DIRECTIONAL -> DIRECTIONAL_COLS to DIRECTIONAL_ROWS
        BlurMode.CORNERS -> CORNER_COLS to CORNER_ROWS
    }

    /** Peso 0..1 por celda (fila-major). Longitud [WINDOW_COUNT]. */
    fun weights(tiltX: Float, tiltY: Float, intensity: Float, mode: BlurMode): FloatArray {
        val (cols, rows) = gridSize(mode)
        val out = FloatArray(WINDOW_COUNT)
        val gain = intensity.coerceIn(0f, 1f)
        var i = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val u = (col + 0.5f) / cols
                val v = (row + 0.5f) / rows
                out[i] = (BlurMask.sample(u, v, tiltX, tiltY, mode) * gain).coerceIn(0f, 1f)
                i++
            }
        }
        return out
    }

    fun radii(tiltX: Float, tiltY: Float, intensity: Float, mode: BlurMode): IntArray {
        val w = weights(tiltX, tiltY, intensity, mode)
        return IntArray(WINDOW_COUNT) { i ->
            val r = (w[i] * MAX_RADIUS_PX).roundToInt()
            if (r < MIN_VISIBLE_RADIUS) 0 else r.coerceAtMost(MAX_RADIUS_PX)
        }
    }

    fun plan(
        screenW: Int,
        screenH: Int,
        tiltX: Float,
        tiltY: Float,
        intensity: Float,
        mode: BlurMode,
    ): List<StripSpec> {
        val w = screenW.coerceAtLeast(1)
        val h = screenH.coerceAtLeast(1)
        val (cols, rows) = gridSize(mode)
        val radii = radii(tiltX, tiltY, intensity, mode)
        val (insetL, insetT, insetR, insetB) = insets(w, h, tiltX, tiltY, intensity)
        val innerW = (w - insetL - insetR).coerceAtLeast(cols)
        val innerH = (h - insetT - insetB).coerceAtLeast(rows)

        val specs = ArrayList<StripSpec>(WINDOW_COUNT)
        var i = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val x0 = insetL + innerW * col / cols
                val x1 = insetL + innerW * (col + 1) / cols + if (col < cols - 1) 1 else 0
                val y0 = insetT + innerH * row / rows
                val y1 = insetT + innerH * (row + 1) / rows + if (row < rows - 1) 1 else 0
                val radius = radii.getOrElse(i) { 0 }
                specs += if (radius <= 0) {
                    StripSpec(0, 0, 1, 1, 0)
                } else {
                    StripSpec(
                        x = x0,
                        y = y0,
                        width = (x1 - x0).coerceAtLeast(1),
                        height = (y1 - y0).coerceAtLeast(1),
                        radiusPx = radius,
                    )
                }
                i++
            }
        }
        while (specs.size < WINDOW_COUNT) {
            specs += StripSpec(0, 0, 1, 1, 0)
        }
        return specs
    }

    fun anyVisible(plan: List<StripSpec>): Boolean = plan.any { it.visible }

    /**
     * Insets del lado que se aleja: [left, top, right, bottom].
     * tiltX > 0 (derecha más cerca) → inset izquierdo.
     */
    fun insets(
        screenW: Int,
        screenH: Int,
        tiltX: Float,
        tiltY: Float,
        intensity: Float,
    ): IntArray {
        val tx = tiltX.coerceIn(-1f, 1f)
        val ty = (tiltY * BlurMask.Y_WEIGHT).coerceIn(-1f, 1f)
        val engage = BlurMask.tiltEngage(hypot(tx, ty)) * intensity.coerceIn(0f, 1f)
        val recessX = screenW * MAX_RECESS * engage
        val recessY = screenH * MAX_RECESS * engage * 0.7f
        val insetL = if (tx > 0f) (recessX * tx).roundToInt() else 0
        val insetR = if (tx < 0f) (recessX * -tx).roundToInt() else 0
        val insetT = if (ty > 0f) (recessY * ty).roundToInt() else 0
        val insetB = if (ty < 0f) (recessY * -ty).roundToInt() else 0
        return intArrayOf(insetL, insetT, insetR, insetB)
    }
}
