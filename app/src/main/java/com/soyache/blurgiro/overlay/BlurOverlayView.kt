package com.soyache.blurgiro.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import com.soyache.blurgiro.data.BlurMode
import com.soyache.blurgiro.effect.GlassBlurRenderer

/**
 * Capa de cristal a pantalla completa. No consume toques (la ventana es NOT_TOUCHABLE).
 */
class BlurOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val renderer = GlassBlurRenderer()
    private var frostBlurApplied = false

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setEffect(tiltX: Float, tiltY: Float, intensity: Float, mode: BlurMode) {
        renderer.tiltX = tiltX
        renderer.tiltY = tiltY
        renderer.intensity = intensity
        renderer.mode = mode
        applySoftFrost(intensity)
        invalidate()
    }

    private fun applySoftFrost(intensity: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (frostBlurApplied) return
        val radius = 6f + intensity * 16f
        setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
        frostBlurApplied = true
    }

    fun refreshFrostBlur(intensity: Float) {
        frostBlurApplied = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(null)
        }
        applySoftFrost(intensity)
    }

    override fun onDraw(canvas: Canvas) {
        renderer.draw(canvas, width, height)
    }
}
