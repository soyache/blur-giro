package com.soyache.blurgiro.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import com.soyache.blurgiro.data.AppSettings
import com.soyache.blurgiro.data.BlurMode
import com.soyache.blurgiro.effect.BlurMask
import com.soyache.blurgiro.effect.CrossWindowBlur
import kotlin.math.roundToInt

/**
 * Overlay no táctil a pantalla completa (niebla mate) + parches que piden
 * *background blur* al compositor. No usa FLAG_BLUR_BEHIND: ese API desenfoca
 * toda la pantalla, no solo el borde.
 */
class OverlayController(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val settings = AppSettings.get(appContext)

    private var frostView: BlurOverlayView? = null
    private val patches = Array(PATCH_COUNT) { FrostPatchWindow(appContext, windowManager) }
    private var attached = false
    private var compositorBlurLive = false
    private var stopBlurListen: (() -> Unit)? = null

    private var tiltX = 0f
    private var tiltY = 0f

    fun show() {
        if (attached) return
        compositorBlurLive = CrossWindowBlur.isEnabled(appContext)
        stopBlurListen = CrossWindowBlur.listen(appContext) { enabled ->
            compositorBlurLive = enabled
            if (attached) pushEffect(forceRegions = true)
        }
        addFrost()
        patches.forEach { it.ensureShown() }
        attached = true
        pushEffect(forceRegions = true)
    }

    fun hide() {
        if (!attached) return
        stopBlurListen?.invoke()
        stopBlurListen = null
        frostView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        frostView = null
        patches.forEach { it.hide() }
        attached = false
    }

    fun onTilt(x: Float, y: Float) {
        tiltX = x
        tiltY = y
        if (attached) pushEffect(forceRegions = false)
    }

    fun onSettingsChanged() {
        if (attached) pushEffect(forceRegions = true)
    }

    private fun pushEffect(forceRegions: Boolean) {
        val intensity = settings.intensity
        val mode = settings.mode
        frostView?.setEffect(tiltX, tiltY, intensity, mode, compositorBlurLive)
        updatePatches(intensity, mode, forceRegions)
    }

    private fun addFrost() {
        val view = BlurOverlayView(appContext)
        val params = baseParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
        windowManager.addView(view, params)
        frostView = view
    }

    private fun updatePatches(intensity: Float, mode: BlurMode, force: Boolean) {
        val metrics = appContext.resources.displayMetrics
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels

        if (mode == BlurMode.CORNERS) {
            val corners = BlurMask.cornerStrengths(tiltX, tiltY)
            val strengths = floatArrayOf(
                corners.topLeft,
                corners.topRight,
                corners.bottomLeft,
                corners.bottomRight,
            )
            val positions = arrayOf(
                intArrayOf(0, 0),
                intArrayOf(1, 0),
                intArrayOf(0, 1),
                intArrayOf(1, 1),
            )
            for (i in 0 until PATCH_COUNT) {
                val strength = strengths[i] * intensity
                val w = (screenW * (0.28f + 0.20f * strength)).roundToInt().coerceAtLeast(48)
                val h = (screenH * (0.22f + 0.18f * strength)).roundToInt().coerceAtLeast(48)
                val x = if (positions[i][0] == 0) 0 else screenW - w
                val y = if (positions[i][1] == 0) 0 else screenH - h
                val blur = (18 + 70 * strength).roundToInt()
                patches[i].update(
                    targetX = x,
                    targetY = y,
                    targetW = w,
                    targetH = h,
                    blurRadius = blur,
                    strength = strength,
                    visible = strength > 0.10f,
                    blurEnabled = compositorBlurLive,
                    force = force,
                )
            }
        } else {
            val side = BlurMask.dominantSide(tiltX, tiltY)
            val strength = BlurMask.directionalStrength(tiltX, tiltY) * intensity
            val visible = strength > 0.10f
            val depths = floatArrayOf(0.38f, 0.26f, 0.16f)
            val blurScales = floatArrayOf(0.55f, 0.78f, 1f)
            for (i in 0 until PATCH_COUNT) {
                if (i >= depths.size) {
                    patches[i].update(0, 0, 1, 1, 0, 0f, visible = false, blurEnabled = false, force = force)
                    continue
                }
                val thick = depths[i] * (0.72f + 0.28f * strength)
                val w: Int
                val h: Int
                val x: Int
                val y: Int
                when (side) {
                    0 -> {
                        w = (screenW * thick).roundToInt().coerceAtLeast(24)
                        h = screenH
                        x = 0
                        y = 0
                    }
                    2 -> {
                        w = (screenW * thick).roundToInt().coerceAtLeast(24)
                        h = screenH
                        x = screenW - w
                        y = 0
                    }
                    1 -> {
                        w = screenW
                        h = (screenH * thick).roundToInt().coerceAtLeast(24)
                        x = 0
                        y = 0
                    }
                    else -> {
                        w = screenW
                        h = (screenH * thick).roundToInt().coerceAtLeast(24)
                        x = 0
                        y = screenH - h
                    }
                }
                val blur = (16 + 86 * strength * blurScales[i]).roundToInt()
                patches[i].update(
                    targetX = x,
                    targetY = y,
                    targetW = w,
                    targetH = h,
                    blurRadius = blur,
                    strength = strength * blurScales[i],
                    visible = visible,
                    blurEnabled = compositorBlurLive,
                    force = force,
                )
            }
        }
    }

    private fun baseParams(width: Int, height: Int): WindowManager.LayoutParams {
        val flags = (
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        return WindowManager.LayoutParams(
            width,
            height,
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

    companion object {
        private const val PATCH_COUNT = 4
    }
}
