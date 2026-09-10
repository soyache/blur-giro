package com.soyache.blurgiro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.soyache.blurgiro.data.BlurMode
import com.soyache.blurgiro.effect.StripBlur

private const val SCHEME_W = 800
private const val SCHEME_H = 400

/**
 * Esquema de las bandas (qué celdas pedirían radio). No simula cristal,
 * no pinta niebla sobre un launcher falso.
 */
@Composable
fun TiltPreview(
    tiltX: Float,
    tiltY: Float,
    intensity: Float,
    mode: BlurMode,
    modifier: Modifier = Modifier,
) {
    val plan = remember(tiltX, tiltY, intensity, mode) {
        StripBlur.plan(SCHEME_W, SCHEME_H, tiltX, tiltY, intensity, mode)
    }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFF0B1020))
            val sx = size.width / SCHEME_W
            val sy = size.height / SCHEME_H
            for (spec in plan) {
                if (!spec.visible) continue
                val alpha = (spec.radiusPx.toFloat() / StripBlur.MAX_RADIUS_PX)
                    .coerceIn(0f, 1f) * 0.50f
                drawRect(
                    color = Color.White.copy(alpha = alpha),
                    topLeft = Offset(spec.x * sx, spec.y * sy),
                    size = Size(spec.width * sx, spec.height * sy),
                )
            }
        }
    }
}
