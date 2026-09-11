package com.soyache.blurgiro.effect

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Tres resoluciones de la misma escena: nítida, media y fuerte.
 * Se mezclan en Canvas según la máscara del giro. Sin tinte ni velo.
 */
class DefocusPyramid(
    val sharp: Bitmap,
    val mid: Bitmap,
    val heavy: Bitmap,
    val owned: Boolean,
) {
    fun recycle() {
        if (mid !== sharp && !mid.isRecycled) mid.recycle()
        if (heavy !== sharp && heavy !== mid && !heavy.isRecycled) heavy.recycle()
        if (owned && !sharp.isRecycled) sharp.recycle()
    }

    companion object {
        private const val BLUR_MAX_WIDTH = 480

        fun build(source: Bitmap, intensity: Float, ownSource: Boolean = false): DefocusPyramid {
            val work = downscale(source, BLUR_MAX_WIDTH)
            val mid = StackBlur.blur(work, DefocusLook.midRadius(intensity))
            val heavy = StackBlur.blur(work, DefocusLook.heavyRadius(intensity))
            if (work !== source && work !== mid && work !== heavy) {
                work.recycle()
            }
            return DefocusPyramid(source, mid, heavy, owned = ownSource)
        }

        fun downscale(source: Bitmap, maxWidth: Int): Bitmap {
            if (source.width <= maxWidth) return source
            val h = (source.height.toFloat() * maxWidth / source.width).roundToInt().coerceAtLeast(1)
            val out = Bitmap.createBitmap(maxWidth, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(
                source,
                Rect(0, 0, source.width, source.height),
                RectF(0f, 0f, maxWidth.toFloat(), h.toFloat()),
                paint,
            )
            return out
        }

        fun coverScale(sourceW: Int, sourceH: Int, destW: Int, destH: Int): Float {
            if (sourceW <= 0 || sourceH <= 0) return 1f
            return max(destW.toFloat() / sourceW, destH.toFloat() / sourceH)
        }
    }
}
