package com.soyache.blurgiro.overlay

import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.util.AttributeSet
import android.view.View
import com.soyache.blurgiro.data.BlurMode
import com.soyache.blurgiro.effect.GlassBlurRenderer

/**
 * Capa de niebla a pantalla completa. No consume toques (la ventana es NOT_TOUCHABLE).
 *
 * No aplica RenderEffect sobre sí misma: eso solo desenfocaba la pintura y
 * convertía el tinte en un resplandor.
 */
class BlurOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val renderer = GlassBlurRenderer()

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(null)
        }
    }

    fun setEffect(
        tiltX: Float,
        tiltY: Float,
        intensity: Float,
        mode: BlurMode,
        compositorBlurLive: Boolean = false,
    ) {
        renderer.tiltX = tiltX
        renderer.tiltY = tiltY
        renderer.intensity = intensity
        renderer.mode = mode
        renderer.compositorBlurLive = compositorBlurLive
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        renderer.draw(canvas, width, height)
    }
}
