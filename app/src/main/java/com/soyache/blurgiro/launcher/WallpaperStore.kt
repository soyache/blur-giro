package com.soyache.blurgiro.launcher

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlin.math.max
import kotlin.math.roundToInt

object WallpaperStore {

    fun load(context: Context, maxWidth: Int = 1080, maxHeight: Int = 1920): Bitmap {
        val manager = WallpaperManager.getInstance(context)
        val drawable = runCatching { manager.peekDrawable() }.getOrNull()
            ?: runCatching { manager.fastDrawable }.getOrNull()
            ?: runCatching { manager.drawable }.getOrNull()
        val bitmap = drawable?.let { drawableToBitmap(it, maxWidth, maxHeight) }
        return bitmap ?: defaultWallpaper(maxWidth, maxHeight)
    }

    fun defaultWallpaper(width: Int, height: Int): Bitmap {
        val w = width.coerceAtLeast(64)
        val h = height.coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h.toFloat(),
            intArrayOf(0xFF0B1020.toInt(), 0xFF141A2E.toInt(), 0xFF1A1030.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        return bitmap
    }

    private fun drawableToBitmap(drawable: Drawable, maxWidth: Int, maxHeight: Int): Bitmap? {
        if (drawable is BitmapDrawable) {
            val src = drawable.bitmap ?: return null
            if (src.isRecycled) return null
            return scaleDown(src, maxWidth, maxHeight)
        }
        val srcW = drawable.intrinsicWidth.takeIf { it > 0 } ?: maxWidth
        val srcH = drawable.intrinsicHeight.takeIf { it > 0 } ?: maxHeight
        val scale = max(
            srcW.toFloat() / maxWidth.coerceAtLeast(1),
            srcH.toFloat() / maxHeight.coerceAtLeast(1),
        ).coerceAtLeast(1f)
        val w = (srcW / scale).roundToInt().coerceAtLeast(1)
        val h = (srcH / scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        return bitmap
    }

    private fun scaleDown(src: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val scale = max(
            src.width.toFloat() / maxWidth.coerceAtLeast(1),
            src.height.toFloat() / maxHeight.coerceAtLeast(1),
        )
        if (scale <= 1f) return src
        val w = (src.width / scale).roundToInt().coerceAtLeast(1)
        val h = (src.height / scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }
}
