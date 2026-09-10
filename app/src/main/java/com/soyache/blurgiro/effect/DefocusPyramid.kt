package com.soyache.blurgiro.effect

import android.graphics.Bitmap

/**
 * Tres resoluciones de la misma captura: nítida, media y fuerte.
 * Se mezclan en GPU/Canvas según la máscara del giro. Sin tinte.
 */
class DefocusPyramid(
    val sharp: Bitmap,
    val mid: Bitmap,
    val heavy: Bitmap,
    val owned: Boolean,
) {
    fun recycle() {
        if (!owned) return
        if (sharp !== mid && !sharp.isRecycled) sharp.recycle()
        if (mid !== sharp && !mid.isRecycled) mid.recycle()
        if (heavy !== sharp && heavy !== mid && !heavy.isRecycled) heavy.recycle()
    }

    companion object {
        fun build(source: Bitmap, intensity: Float, ownSource: Boolean = true): DefocusPyramid {
            val mid = StackBlur.blur(source, DefocusLook.midRadius(intensity))
            val heavy = StackBlur.blur(source, DefocusLook.heavyRadius(intensity))
            return DefocusPyramid(source, mid, heavy, owned = ownSource)
        }
    }
}
