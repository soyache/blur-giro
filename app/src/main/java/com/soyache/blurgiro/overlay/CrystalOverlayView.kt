package com.soyache.blurgiro.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.soyache.blurgiro.data.BlurMode
import com.soyache.blurgiro.effect.BlurMask
import com.soyache.blurgiro.effect.DefocusPyramid
import com.soyache.blurgiro.effect.PerspectiveWarp
import kotlin.math.hypot

/**
 * Dibuja la captura desenfocada + warp, o nada (identidad).
 * Sin tinte, sin niebla: solo píxeles del contenido con defocus.
 */
class CrystalOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var previewMode: Boolean = false

    private var pyramid: DefocusPyramid? = null
    private var tiltX = 0f
    private var tiltY = 0f
    private var intensity = 0.5f
    private var mode: BlurMode = BlurMode.DIRECTIONAL

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dstIn = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    private val warp = Matrix()
    private val srcRect = Rect()
    private val dstRect = RectF()

    private var drawingEffect = false
    var onDrawingChanged: ((drawing: Boolean) -> Unit)? = null

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun setPyramid(next: DefocusPyramid?) {
        if (pyramid === next) return
        pyramid = next
        updateDrawingFlag()
        invalidate()
    }

    fun setTilt(x: Float, y: Float, intensity: Float, mode: BlurMode) {
        tiltX = x
        tiltY = y
        this.intensity = intensity
        this.mode = mode
        updateDrawingFlag()
        invalidate()
    }

    fun isDrawingEffect(): Boolean = drawingEffect

    private fun updateDrawingFlag() {
        val amount = BlurMask.effectAmount(tiltX, tiltY, intensity)
        val next = amount > 0.02f && pyramid != null
        if (next != drawingEffect) {
            drawingEffect = next
            alpha = if (next || previewMode) 1f else 0f
            onDrawingChanged?.invoke(next)
        } else if (!previewMode) {
            alpha = if (next) 1f else 0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        val layers = pyramid ?: return
        if (width <= 0 || height <= 0) return
        if (layers.sharp.isRecycled) return

        val amount = BlurMask.effectAmount(tiltX, tiltY, intensity)
        if (amount <= 0.02f) {
            if (previewMode) {
                drawBitmap(canvas, layers.sharp, identity = true)
            }
            return
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val dest = PerspectiveWarp.destQuad(w, h, tiltX, tiltY, intensity)
        warp.reset()
        warp.setPolyToPoly(PerspectiveWarp.identityQuad(w, h), 0, dest, 0, 4)
        val cover = 1f + PerspectiveWarp.maxCornerDelta(dest, w, h) / hypot(w, h).coerceAtLeast(1f) * 2.4f
        warp.postScale(cover.coerceIn(1f, 1.12f), cover.coerceIn(1f, 1.12f), w * 0.5f, h * 0.5f)

        canvas.save()
        canvas.concat(warp)
        drawBitmap(canvas, layers.sharp, identity = false)
        drawMasked(canvas, layers.mid, farMask(w, h, mid = true))
        drawMasked(canvas, layers.heavy, farMask(w, h, mid = false))
        canvas.restore()
    }

    private fun drawBitmap(canvas: Canvas, bitmap: Bitmap, identity: Boolean) {
        if (bitmap.isRecycled) return
        srcRect.set(0, 0, bitmap.width, bitmap.height)
        dstRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(bitmap, srcRect, dstRect, bitmapPaint)
        if (identity) return
    }

    private fun drawMasked(canvas: Canvas, bitmap: Bitmap, shader: Shader?) {
        if (bitmap.isRecycled || shader == null) return
        srcRect.set(0, 0, bitmap.width, bitmap.height)
        dstRect.set(0f, 0f, width.toFloat(), height.toFloat())
        val save = canvas.saveLayer(dstRect, null)
        canvas.drawBitmap(bitmap, srcRect, dstRect, bitmapPaint)
        maskPaint.xfermode = dstIn
        maskPaint.shader = shader
        canvas.drawRect(dstRect, maskPaint)
        maskPaint.xfermode = null
        maskPaint.shader = null
        canvas.restoreToCount(save)
    }

    private fun farMask(w: Float, h: Float, mid: Boolean): Shader {
        val tx = tiltX.coerceIn(-1f, 1f)
        val ty = (tiltY * PerspectiveWarp.Y_WEIGHT).coerceIn(-1f, 1f)
        if (mode == BlurMode.CORNERS) {
            val fx = w * (0.5f + tx * 0.42f)
            val fy = h * (0.5f + ty * 0.42f)
            val radius = hypot(w, h) * if (mid) 0.92f else 0.78f
            val stops = if (mid) {
                floatArrayOf(0f, 0.32f, 1f)
            } else {
                floatArrayOf(0f, 0.52f, 1f)
            }
            return RadialGradient(
                fx,
                fy,
                radius,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.WHITE),
                stops,
                Shader.TileMode.CLAMP,
            )
        }
        val mag = hypot(tx, ty).coerceAtLeast(0.04f)
        val dx = tx / mag
        val dy = ty / mag
        val cx = w * 0.5f
        val cy = h * 0.5f
        val len = hypot(w, h) * 0.58f
        val closerX = cx + dx * len
        val closerY = cy + dy * len
        val farX = cx - dx * len
        val farY = cy - dy * len
        val stops = if (mid) {
            floatArrayOf(0f, 0.24f, 0.72f, 1f)
        } else {
            floatArrayOf(0f, 0.50f, 0.86f, 1f)
        }
        return LinearGradient(
            closerX,
            closerY,
            farX,
            farY,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.WHITE, Color.WHITE),
            stops,
            Shader.TileMode.CLAMP,
        )
    }
}
