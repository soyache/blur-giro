package com.soyache.blurgiro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.viewinterop.AndroidView
import com.soyache.blurgiro.data.BlurMode
import com.soyache.blurgiro.overlay.BlurOverlayView

@Composable
fun TiltPreview(
    tiltX: Float,
    tiltY: Float,
    intensity: Float,
    mode: BlurMode,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        PreviewWallpaper(Modifier.fillMaxSize())
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                BlurOverlayView(context)
            },
            update = { view ->
                view.setEffect(tiltX, tiltY, intensity, mode)
            },
        )
    }
}

@Composable
private fun PreviewWallpaper(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF1B3A5F), Color(0xFF6BA3C7), Color(0xFFE8C39E), Color(0xFF7A9E4F)),
            ),
        )
        val cols = 4
        val rows = 3
        val pad = size.width * 0.06f
        val gap = size.width * 0.035f
        val tileW = (size.width - pad * 2 - gap * (cols - 1)) / cols
        val tileH = tileW * 1.05f
        val startY = size.height * 0.34f
        val palette = listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFFD93D),
            Color(0xFF6BCB77),
            Color(0xFF4D96FF),
            Color(0xFFB980F0),
            Color(0xFFFF8C42),
            Color(0xFF36C2CE),
            Color(0xFFF67280),
            Color(0xFFC4E538),
            Color(0xFF82E0AA),
            Color(0xFFF5B041),
            Color(0xFF5DADE2),
        )
        var i = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = pad + c * (tileW + gap)
                val y = startY + r * (tileH + gap)
                drawRoundRect(
                    color = palette[i % palette.size],
                    topLeft = Offset(x, y),
                    size = Size(tileW, tileH),
                    cornerRadius = CornerRadius(tileW * 0.22f, tileW * 0.22f),
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.28f),
                    topLeft = Offset(x + tileW * 0.18f, y + tileH * 0.16f),
                    size = Size(tileW * 0.64f, tileH * 0.18f),
                    cornerRadius = CornerRadius(8f, 8f),
                )
                i++
            }
        }
        drawClock()
    }
}

private fun DrawScope.drawClock() {
    val cx = size.width * 0.5f
    val cy = size.height * 0.18f
    drawCircle(Color.White.copy(alpha = 0.88f), radius = size.minDimension * 0.11f, center = Offset(cx, cy))
    drawCircle(Color(0xFF1B3A5F), radius = size.minDimension * 0.018f, center = Offset(cx, cy))
    val len = size.minDimension * 0.08f
    drawLine(Color(0xFF1B3A5F), Offset(cx, cy), Offset(cx, cy - len), strokeWidth = 6f)
    drawLine(Color(0xFF1B3A5F), Offset(cx, cy), Offset(cx + len * 0.7f, cy + len * 0.15f), strokeWidth = 5f)
}
