package com.soyache.blurgiro.overlay

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.soyache.blurgiro.R
import com.soyache.blurgiro.effect.StripSpec
import kotlin.math.abs

/**
 * Una banda flotante translúcida [TYPE_APPLICATION_OVERLAY] que pide
 * [android.view.Window.setBackgroundBlurRadius] dentro de sus bounds.
 *
 * El drawable de fondo es casi invisible: solo define el recorte del blur
 * del compositor. Sin niebla, sin brillo, sin captura.
 */
class BlurStripWindow(
    context: Context,
    private val index: Int,
) {
    private val appContext = context.applicationContext
    private val outline = ColorDrawable(OUTLINE_COLOR)

    private var dialog: Dialog? = null
    private var shown = false

    private var x = Int.MIN_VALUE
    private var y = Int.MIN_VALUE
    private var w = 0
    private var h = 0
    private var blur = -1
    private var visible = false

    fun ensureShown(): Boolean {
        if (shown) return dialog != null
        shown = true
        return tryShowDialog()
    }

    fun hide() {
        if (dialog == null && !shown) return
        val window = dialog?.window
        if (window != null) {
            BackdropBlur.applyToWindow(window, 0)
        }
        dialog?.let { runCatching { it.dismiss() } }
        dialog = null
        shown = false
        x = Int.MIN_VALUE
        y = Int.MIN_VALUE
        w = 0
        h = 0
        blur = -1
        visible = false
    }

    fun apply(spec: StripSpec, force: Boolean) {
        val show = spec.visible
        val targetX = if (show) spec.x else 0
        val targetY = if (show) spec.y else 0
        val targetW = if (show) spec.width else 1
        val targetH = if (show) spec.height else 1
        val radius = if (show) spec.radiusPx else 0

        if (show && !ensureShown()) return
        if (!shown && !show) return

        val changed = force ||
            abs(x - targetX) > 1 ||
            abs(y - targetY) > 1 ||
            abs(w - targetW) > 1 ||
            abs(h - targetH) > 1 ||
            blur != radius ||
            visible != show
        if (!changed) return

        x = targetX
        y = targetY
        w = targetW
        h = targetH
        blur = radius
        visible = show
        layout(targetX, targetY, targetW, targetH, radius, show)
    }

    private fun tryShowDialog(): Boolean {
        return runCatching {
            val themed = ContextThemeWrapper(appContext, R.style.Theme_CristalGiro_BlurStrip)
            val created = Dialog(themed, R.style.Theme_CristalGiro_BlurStrip)
            created.setCancelable(false)
            created.setCanceledOnTouchOutside(false)
            created.setContentView(View(themed).apply { background = outline })
            val window = created.window ?: return@runCatching false
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            window.setBackgroundDrawable(outline)
            window.setFormat(PixelFormat.TRANSLUCENT)
            window.setGravity(Gravity.TOP or Gravity.START)
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val flags = (
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                )
            window.setFlags(flags, flags)
            val params = window.attributes
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            params.format = PixelFormat.TRANSLUCENT
            params.width = 1
            params.height = 1
            params.x = 0
            params.y = 0
            params.gravity = Gravity.TOP or Gravity.START
            params.title = "CristalGiro.strip.$index"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            window.attributes = params
            created.show()
            BackdropBlur.applyToWindow(window, 0)
            dialog = created
            true
        }.getOrDefault(false)
    }

    private fun layout(x: Int, y: Int, w: Int, h: Int, radius: Int, show: Boolean) {
        val window = dialog?.window ?: return
        val params = window.attributes
        params.x = x
        params.y = y
        params.width = w
        params.height = h
        params.gravity = Gravity.TOP or Gravity.START
        window.attributes = params
        window.setBackgroundDrawable(outline)
        BackdropBlur.applyToWindow(window, radius)
        window.decorView.visibility = if (show) View.VISIBLE else View.GONE
    }

    companion object {
        /**
         * Alpha 3/255: define el recorte del compositor sin velo visible.
         * Un fondo opaco taparía el blur; uno con niebla (0.1.0/0.1.1) era el fallo.
         */
        private val OUTLINE_COLOR = Color.argb(3, 255, 255, 255)
    }
}
