package com.soyache.blurgiro.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Captura la pantalla con un [MediaProjection] que el usuario acaba de conceder.
 *
 * Mientras el overlay dibuja el frame procesado, se ignoran frames nuevos
 * (evitarían un bucle de blur-sobre-blur). De frente el overlay no pinta
 * nada: esos frames son limpios y refrescan el contenido.
 */
class ScreenGrabber(
    context: Context,
    private val projection: MediaProjection,
    private val onCleanFrame: (Bitmap) -> Unit,
    private val onStopped: () -> Unit,
) : ImageReader.OnImageAvailableListener {

    private val app = context.applicationContext
    private val windowManager = app.getSystemService(WindowManager::class.java)
    private val thread = HandlerThread("cristal-grab").also { it.start() }
    private val bg = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())

    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var running = false

    @Volatile
    var acceptFrames: Boolean = true

    private var lastAcceptAt = 0L

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            main.post { onStopped() }
        }
    }

    fun start() {
        if (running) return
        running = true
        projection.registerCallback(projectionCallback, main)
        openDisplay()
    }

    fun restartDisplay() {
        if (!running) return
        closeDisplay()
        openDisplay()
    }

    fun stop() {
        if (!running) return
        running = false
        closeDisplay()
        runCatching { projection.unregisterCallback(projectionCallback) }
        runCatching { projection.stop() }
        thread.quitSafely()
    }

    private fun openDisplay() {
        val bounds = screenBounds()
        val fullW = bounds.width().coerceAtLeast(1)
        val fullH = bounds.height().coerceAtLeast(1)
        val scale = CAPTURE_SCALE
        val width = max(180, (fullW * scale).roundToInt())
        val height = max(320, (fullH * scale).roundToInt())
        val dpi = max(120, (app.resources.displayMetrics.densityDpi * scale).roundToInt())

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
        imageReader.setOnImageAvailableListener(this, bg)
        reader = imageReader
        display = projection.createVirtualDisplay(
            "CristalGiro",
            width,
            height,
            dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            bg,
        )
        Log.i(TAG, "captura ${width}x$height dpi=$dpi")
    }

    private fun closeDisplay() {
        runCatching { display?.release() }
        display = null
        runCatching { reader?.close() }
        reader = null
    }

    override fun onImageAvailable(reader: ImageReader) {
        val image = try {
            reader.acquireLatestImage()
        } catch (_: Throwable) {
            null
        } ?: return
        val now = SystemClock.uptimeMillis()
        if (!acceptFrames || !running || now - lastAcceptAt < FRAME_MIN_INTERVAL_MS) {
            image.close()
            return
        }
        lastAcceptAt = now
        val bitmap = try {
            imageToBitmap(image)
        } catch (t: Throwable) {
            Log.w(TAG, "no se pudo leer el frame", t)
            null
        } finally {
            image.close()
        } ?: return
        main.post { onCleanFrame(bitmap) }
    }

    @Suppress("DEPRECATION")
    private fun screenBounds(): Rect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return windowManager.currentWindowMetrics.bounds
        }
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val width = image.width
        val height = image.height
        val rowPadding = rowStride - pixelStride * width
        val extra = if (pixelStride == 0) 0 else rowPadding / pixelStride
        val raw = Bitmap.createBitmap(width + extra, height, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        raw.copyPixelsFromBuffer(buffer)
        return if (extra == 0) {
            raw
        } else {
            val cropped = Bitmap.createBitmap(raw, 0, 0, width, height)
            if (cropped !== raw) raw.recycle()
            cropped
        }
    }

    companion object {
        private const val TAG = "CristalGiro"
        const val CAPTURE_SCALE = 0.42f
        private const val FRAME_MIN_INTERVAL_MS = 220L
    }
}
