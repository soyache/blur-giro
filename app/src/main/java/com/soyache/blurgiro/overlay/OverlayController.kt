package com.soyache.blurgiro.overlay

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.soyache.blurgiro.data.AppSettings
import com.soyache.blurgiro.effect.CrossWindowBlur
import com.soyache.blurgiro.effect.StripBlur
import com.soyache.blurgiro.effect.StripSpec

/**
 * Varias bandas overlay no táctiles. Cada una pide background blur acotado.
 * De frente (o si el compositor apaga el blur cruzado) se ocultan: el teléfono
 * se ve normal. Sin captura, sin velo pintado.
 */
class OverlayController(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val settings = AppSettings.get(appContext)
    private val strips = Array(StripBlur.WINDOW_COUNT) { BlurStripWindow(appContext, it) }

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
            if (attached) push(force = true)
        }
        attached = true
        push(force = true)
    }

    fun hide() {
        if (!attached) return
        stopBlurListen?.invoke()
        stopBlurListen = null
        strips.forEach { it.hide() }
        attached = false
        compositorBlurLive = false
    }

    fun onTilt(x: Float, y: Float) {
        tiltX = x
        tiltY = y
        if (attached) push(force = false)
    }

    fun onSettingsChanged() {
        if (attached) push(force = true)
    }

    fun onDisplayChanged() {
        if (attached) push(force = true)
    }

    private fun push(force: Boolean) {
        if (!compositorBlurLive) {
            strips.forEach { it.hide() }
            return
        }
        val (sw, sh) = screenSize()
        val plan = StripBlur.plan(sw, sh, tiltX, tiltY, settings.intensity, settings.mode)
        if (!StripBlur.anyVisible(plan)) {
            strips.forEach { it.hide() }
            return
        }
        for (i in strips.indices) {
            strips[i].apply(plan.getOrElse(i) { HIDDEN }, force)
        }
    }

    @Suppress("DEPRECATION")
    private fun screenSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds: Rect = windowManager.currentWindowMetrics.bounds
            return bounds.width().coerceAtLeast(1) to bounds.height().coerceAtLeast(1)
        }
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels.coerceAtLeast(1) to metrics.heightPixels.coerceAtLeast(1)
    }

    companion object {
        private val HIDDEN = StripSpec(0, 0, 1, 1, 0)
    }
}
