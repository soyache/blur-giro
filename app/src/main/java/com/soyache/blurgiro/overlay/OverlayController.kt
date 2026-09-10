package com.soyache.blurgiro.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import com.soyache.blurgiro.capture.ScreenGrabber
import com.soyache.blurgiro.data.AppSettings
import com.soyache.blurgiro.effect.DefocusPyramid
import java.util.concurrent.Executors

/**
 * Overlay no táctil a pantalla completa. De frente no pinta nada.
 * Al girar dibuja la captura con desenfoque direccional + perspectiva.
 */
class OverlayController(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val settings = AppSettings.get(appContext)
    private val main = Handler(Looper.getMainLooper())
    private val blurExecutor = Executors.newSingleThreadExecutor()

    private var crystal: CrystalOverlayView? = null
    private var grabber: ScreenGrabber? = null
    private var attached = false
    private var tiltX = 0f
    private var tiltY = 0f
    private var pyramidJob = 0L
    private var lastIntensity = settings.intensity
    private var currentPyramid: DefocusPyramid? = null

    fun show(projection: MediaProjection, onProjectionGone: () -> Unit = {}) {
        if (attached) return
        addCrystal()
        val view = crystal ?: return
        grabber = ScreenGrabber(
            context = appContext,
            projection = projection,
            onCleanFrame = { bitmap -> rebuildPyramid(bitmap) },
            onStopped = onProjectionGone,
        )
        view.onDrawingChanged = { drawing ->
            grabber?.acceptFrames = !drawing
        }
        grabber?.acceptFrames = !view.isDrawingEffect()
        grabber?.start()
        attached = true
        pushTilt()
    }

    fun hide() {
        if (!attached) return
        attached = false
        grabber?.stop()
        grabber = null
        crystal?.onDrawingChanged = null
        crystal?.let { runCatching { windowManager.removeViewImmediate(it) } }
        crystal?.setPyramid(null)
        currentPyramid?.recycle()
        currentPyramid = null
        crystal = null
        blurExecutor.shutdown()
    }

    fun onTilt(x: Float, y: Float) {
        tiltX = x
        tiltY = y
        if (attached) pushTilt()
    }

    fun onSettingsChanged() {
        if (!attached) return
        if (kotlin.math.abs(lastIntensity - settings.intensity) > 0.04f) {
            lastIntensity = settings.intensity
        }
        pushTilt()
    }

    fun onDisplayChanged() {
        grabber?.restartDisplay()
    }

    private fun rebuildPyramid(bitmap: android.graphics.Bitmap) {
        val token = ++pyramidJob
        val intensity = settings.intensity
        lastIntensity = intensity
        blurExecutor.execute {
            val built = runCatching { DefocusPyramid.build(bitmap, intensity, ownSource = true) }
                .getOrElse {
                    if (!bitmap.isRecycled) bitmap.recycle()
                    return@execute
                }
            main.post {
                if (token != pyramidJob || !attached) {
                    built.recycle()
                    return@post
                }
                val previous = currentPyramid
                currentPyramid = built
                crystal?.setPyramid(built)
                previous?.recycle()
            }
        }
    }

    private fun pushTilt() {
        crystal?.setTilt(tiltX, tiltY, settings.intensity, settings.mode)
        grabber?.acceptFrames = crystal?.isDrawingEffect() != true
    }

    private fun addCrystal() {
        val view = CrystalOverlayView(appContext)
        val params = baseParams()
        windowManager.addView(view, params)
        crystal = view
    }

    private fun baseParams(): WindowManager.LayoutParams {
        val flags = (
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = "CristalGiro"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }
}
