package com.soyache.blurgiro.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewOutlineProvider
import android.graphics.Outline

/**
 * Mancha de cristal en una esquina o un lado. El compositor puede desenfocar
 * el contenido detrás si el fabricante habilita el blur de ventanas.
 */
class BlurRegionView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var strength = 0.5f

    init {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
        }
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width.coerceAtLeast(1), view.height.coerceAtLeast(1))
            }
        }
        clipToOutline = true
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setStrength(value: Float) {
        strength = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width * 0.5f
        val cy = height * 0.5f
        val radius = hypotSafe(width.toFloat(), height.toFloat()) * 0.55f
        val alpha = (70 + 90 * strength).toInt().coerceIn(20, 170)
        paint.shader = RadialGradient(
            cx,
            cy,
            radius,
            Color.argb(alpha, 214, 230, 245),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun hypotSafe(a: Float, b: Float): Float = kotlin.math.hypot(a, b)
}
