package com.soyache.blurgiro.effect

import com.soyache.blurgiro.data.BlurMode

/**
 * Rejilla de iconos tipo launcher (JVM + Android) para tests del look:
 * el contenido se queda visible pero blando en el lado lejano.
 */
object FakeHome {

    private val ICONS = intArrayOf(
        0xFF25D366.toInt(),
        0xFFEA4335.toInt(),
        0xFF10A37F.toInt(),
        0xFF0A66C2.toInt(),
        0xFF1877F2.toInt(),
        0xFFFF3B30.toInt(),
        0xFF34C759.toInt(),
        0xFF007AFF.toInt(),
        0xFFFF9500.toInt(),
        0xFFAF52DE.toInt(),
        0xFF5AC8FA.toInt(),
        0xFFFF2D55.toInt(),
        0xFF8E8E93.toInt(),
        0xFF30D158.toInt(),
        0xFFFFD60A.toInt(),
        0xFF64D2FF.toInt(),
    )

    fun render(width: Int, height: Int): IntArray {
        val pixels = IntArray(width * height)
        val bgTop = 0xFF101218.toInt()
        val bgBot = 0xFF1A1C22.toInt()
        for (y in 0 until height) {
            val t = y / (height - 1).coerceAtLeast(1).toFloat()
            val row = StackBlurCore.lerpPixel(bgTop, bgBot, t)
            val off = y * width
            for (x in 0 until width) pixels[off + x] = row
        }

        val cols = 4
        val rows = 5
        val padX = width * 0.10f
        val padTop = height * 0.18f
        val gapX = width * 0.055f
        val gapY = height * 0.045f
        val tile = (width - padX * 2 - gapX * (cols - 1)) / cols
        var i = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cx = padX + c * (tile + gapX) + tile * 0.5f
                val cy = padTop + r * (tile + gapY) + tile * 0.5f
                fillRoundIcon(pixels, width, height, cx, cy, tile * 0.46f, ICONS[i % ICONS.size])
                i++
            }
        }
        return pixels
    }

    fun mask(
        width: Int,
        height: Int,
        tiltX: Float,
        tiltY: Float,
        mode: BlurMode,
        intensity: Float,
    ): FloatArray {
        val out = FloatArray(width * height)
        val maxX = (width - 1).coerceAtLeast(1).toFloat()
        val maxY = (height - 1).coerceAtLeast(1).toFloat()
        for (y in 0 until height) {
            val v = y / maxY
            val row = y * width
            for (x in 0 until width) {
                val u = x / maxX
                out[row + x] = (BlurMask.sample(u, v, tiltX, tiltY, mode) * intensity).coerceIn(0f, 1f)
            }
        }
        return out
    }

    fun defocus(
        width: Int,
        height: Int,
        tiltX: Float,
        tiltY: Float,
        intensity: Float,
        mode: BlurMode,
    ): IntArray {
        val sharp = render(width, height)
        val amount = BlurMask.effectAmount(tiltX, tiltY, intensity)
        if (amount <= 0.01f) return sharp
        val midR = DefocusLook.midRadius(intensity)
        val heavyR = DefocusLook.heavyRadius(intensity)
        val mid = StackBlurCore.blur(sharp, width, height, midR)
        val heavy = StackBlurCore.blur(sharp, width, height, heavyR)
        val mask = mask(width, height, tiltX, tiltY, mode, intensity)
        return StackBlurCore.composite(sharp, mid, heavy, mask)
    }

    private fun fillRoundIcon(
        pixels: IntArray,
        width: Int,
        height: Int,
        cx: Float,
        cy: Float,
        radius: Float,
        color: Int,
    ) {
        val r2 = radius * radius
        val minX = (cx - radius).toInt().coerceIn(0, width - 1)
        val maxX = (cx + radius).toInt().coerceIn(0, width - 1)
        val minY = (cy - radius).toInt().coerceIn(0, height - 1)
        val maxY = (cy + radius).toInt().coerceIn(0, height - 1)
        val highlight = 0x66FFFFFF
        for (y in minY..maxY) {
            val dy = y + 0.5f - cy
            for (x in minX..maxX) {
                val dx = x + 0.5f - cx
                val d2 = dx * dx + dy * dy
                if (d2 <= r2) {
                    var c = color
                    if (dy < -radius * 0.35f && d2 < r2 * 0.72f) {
                        c = StackBlurCore.lerpPixel(color, highlight or (color and 0x00FFFFFF), 0.22f)
                    }
                    pixels[y * width + x] = c
                }
            }
        }
    }
}
