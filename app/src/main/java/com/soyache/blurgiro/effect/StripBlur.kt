package com.soyache.blurgiro.effect

import com.soyache.blurgiro.data.BlurMode
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
 * Plan de bandas a **pantalla completa**: recorren todo el ancho y todo el alto.
 *
 * Giro X a la derecha (tiltX > 0) → radio alto a la izquierda, degradado suave
 * hasta ~0 a la derecha. De frente → todos los radios a 0.
 *
 * No captura píxeles ni pinta niebla. Sin insets: de borde a borde.
 */
object StripBlur {

    const val WINDOW_COUNT = 16
    const val MAX_RADIUS_PX = 64
    const val MIN_VISIBLE_RADIUS = 2

    private const val DIRECTIONAL_COLS = 16
    private const val DIRECTIONAL_ROWS = 1
    private const val CORNER_COLS = 8
    private const val CORNER_ROWS = 2
    private const val SEAM_OVERLAP_PX = 3

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

        val specs = ArrayList<StripSpec>(WINDOW_COUNT)
        var i = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val x0 = (w * col / cols - if (col > 0) SEAM_OVERLAP_PX else 0).coerceAtLeast(0)
                val x1 = (w * (col + 1) / cols + if (col < cols - 1) SEAM_OVERLAP_PX else 0)
                    .coerceAtMost(w)
                val y0 = (h * row / rows - if (row > 0) SEAM_OVERLAP_PX else 0).coerceAtLeast(0)
                val y1 = (h * (row + 1) / rows + if (row < rows - 1) SEAM_OVERLAP_PX else 0)
                    .coerceAtMost(h)
                specs += StripSpec(
                    x = x0,
                    y = y0,
                    width = (x1 - x0).coerceAtLeast(1),
                    height = (y1 - y0).coerceAtLeast(1),
                    radiusPx = radii.getOrElse(i) { 0 },
                )
                i++
            }
        }
        while (specs.size < WINDOW_COUNT) {
            specs += StripSpec(0, 0, 1, 1, 0)
        }
        return specs
    }

    fun anyVisible(plan: List<StripSpec>): Boolean = plan.any { it.visible }

    /** Unión de las celdas del plan (debe ser la pantalla entera). */
    fun coverage(plan: List<StripSpec>): IntArray {
        if (plan.isEmpty()) return intArrayOf(0, 0, 0, 0)
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        for (spec in plan) {
            minX = minOf(minX, spec.x)
            minY = minOf(minY, spec.y)
            maxX = maxOf(maxX, spec.x + spec.width)
            maxY = maxOf(maxY, spec.y + spec.height)
        }
        return intArrayOf(minX, minY, maxX, maxY)
    }
}
