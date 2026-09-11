package com.soyache.blurgiro.effect

import android.graphics.Bitmap

object StackBlur {
    fun blur(source: Bitmap, radius: Int): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val blurred = StackBlurCore.blur(pixels, w, h, radius)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(blurred, 0, w, 0, 0, w, h)
        return out
    }
}
