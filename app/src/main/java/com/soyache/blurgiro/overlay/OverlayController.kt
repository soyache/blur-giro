package com.soyache.blurgiro.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import com.soyache.blurgiro.data.AppSettings
import com.soyache.blurgiro.data.BlurMode
import com.soyache.blurgiro.effect.BlurMask
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Capa de paso (no táctil) a pantalla completa + manchas de blur en
 * esquinas o en un lado. El blur real del compositor es opcional (OEM).
 */
class OverlayController(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val settings = AppSettings.get(appContext)

    private var frostView: BlurOverlayView? = null
    private val regions = Array(4) { RegionSlot() }
    private var attached = false

    private var tiltX = 0f
    private var tiltY = 0f

    fun show() {
        if (attached) return
        addFrost()
        addRegions()
        attached = true
        pushEffect(forceRegions = true)
    }

    fun hide() {
        if (!attached) return
        frostView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        frostView = null
        regions.forEach { slot ->
            slot.view?.let { runCatching { windowManager.removeViewImmediate(it) } }
            slot.reset()
        }
        attached = false
    }

    fun onTilt(x: Float, y: Float) {
        tiltX = x
        tiltY = y
        if (attached) pushEffect(forceRegions = false)
    }

    fun onSettingsChanged() {
        frostView?.refreshFrostBlur(settings.intensity)
        if (attached) pushEffect(forceRegions = true)
    }

    private fun pushEffect(forceRegions: Boolean) {
        val intensity = settings.intensity
        val mode = settings.mode
        frostView?.setEffect(tiltX, tiltY, intensity, mode)
        updateRegions(intensity, mode, forceRegions)
    }

    private fun addFrost() {
        val view = BlurOverlayView(appContext)
        val params = baseParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        // La capa de escarcha no usa blur-behind: el centro debe seguir nítido.
        windowManager.addView(view, params)
        frostView = view
    }

    private fun addRegions() {
        repeat(4) { index ->
            runCatching {
                val view = BlurRegionView(appContext)
                val params = baseParams(1, 1)
                applyBlurBehind(params, 12)
                windowManager.addView(view, params)
                regions[index].view = view
                regions[index].params = params
            }
        }
    }

    private fun updateRegions(intensity: Float, mode: BlurMode, force: Boolean) {
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
            for (i in 0..3) {
                val strength = strengths[i] * intensity
                val w = (screenW * (0.30f + 0.18f * strength)).roundToInt().coerceAtLeast(48)
                val h = (screenH * (0.24f + 0.16f * strength)).roundToInt().coerceAtLeast(48)
                val x = if (positions[i][0] == 0) 0 else screenW - w
                val y = if (positions[i][1] == 0) 0 else screenH - h
                val blur = (10 + 42 * strength).roundToInt()
                layoutRegion(i, x, y, w, h, strength, blur, visible = strength > 0.08f, force = force)
            }
        } else {
            val side = BlurMask.dominantSide(tiltX, tiltY)
            val strength = BlurMask.directionalStrength(tiltX, tiltY) * intensity
            val thick = (0.22f + 0.28f * strength)
            for (i in 0..3) {
                if (i != 0) {
                    layoutRegion(i, 0, 0, 1, 1, 0f, 0, visible = false, force = force)
                    continue
                }
                val w: Int
                val h: Int
                val x: Int
                val y: Int
                when (side) {
                    0 -> { // izquierda
                        w = (screenW * thick).roundToInt()
                        h = screenH
                        x = 0
                        y = 0
                    }
                    2 -> { // derecha
                        w = (screenW * thick).roundToInt()
                        h = screenH
                        x = screenW - w
                        y = 0
                    }
                    1 -> { // arriba
                        w = screenW
                        h = (screenH * thick).roundToInt()
                        x = 0
                        y = 0
                    }
                    else -> { // abajo
                        w = screenW
                        h = (screenH * thick).roundToInt()
                        x = 0
                        y = screenH - h
                    }
                }
                val blur = (12 + 48 * strength).roundToInt()
                layoutRegion(0, x, y, w, h, strength, blur, visible = true, force = force)
            }
        }
    }

    private fun layoutRegion(
        index: Int,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        strength: Float,
        blurRadius: Int,
        visible: Boolean,
        force: Boolean,
    ) {
        val slot = regions[index]
        val view = slot.view ?: return
        val params = slot.params ?: return
        val targetW = if (visible) w else 1
        val targetH = if (visible) h else 1
        val targetX = if (visible) x else 0
        val targetY = if (visible) y else 0
        val changed =
            force ||
                abs(slot.x - targetX) > 4 ||
                abs(slot.y - targetY) > 4 ||
                abs(slot.w - targetW) > 6 ||
                abs(slot.h - targetH) > 6 ||
                abs(slot.strength - strength) > 0.03f ||
                slot.blur != blurRadius ||
                slot.visible != visible

        if (!changed) {
            view.setStrength(strength)
            return
        }

        view.setStrength(strength)
        view.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
        params.width = targetW
        params.height = targetH
        params.x = targetX
        params.y = targetY
        applyBlurBehind(params, if (visible) blurRadius else 0)
        runCatching { windowManager.updateViewLayout(view, params) }
        slot.x = targetX
        slot.y = targetY
        slot.w = targetW
        slot.h = targetH
        slot.strength = strength
        slot.blur = blurRadius
        slot.visible = visible
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

    private fun applyBlurBehind(params: WindowManager.LayoutParams, radius: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        params.setBlurBehindRadius(radius.coerceIn(0, 80))
    }

    private class RegionSlot {
        var view: BlurRegionView? = null
        var params: WindowManager.LayoutParams? = null
        var x = 0
        var y = 0
        var w = 0
        var h = 0
        var strength = -1f
        var blur = -1
        var visible = true

        fun reset() {
            view = null
            params = null
            x = 0
            y = 0
            w = 0
            h = 0
            strength = -1f
            blur = -1
            visible = true
        }
    }
}
