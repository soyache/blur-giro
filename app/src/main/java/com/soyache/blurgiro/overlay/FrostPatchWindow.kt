package com.soyache.blurgiro.overlay

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.soyache.blurgiro.R
import com.soyache.blurgiro.effect.GlassLook
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Parche pequeño (esquina o banda) que pide backdrop blur al compositor.
 * Si el OEM no lo aplica, el relleno es una niebla oscura mínima — nunca un brillo.
 */
class FrostPatchWindow(
    context: Context,
    private val windowManager: WindowManager,
) {
    private val appContext = context.applicationContext
    private val shape = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.TRANSPARENT)
    }

    private var dialog: Dialog? = null
    private var dialogContent: View? = null
    private var fallbackView: View? = null
    private var fallbackParams: WindowManager.LayoutParams? = null
    private var usingDialog = false
    private var shown = false

    var x = Int.MIN_VALUE
        private set
    var y = Int.MIN_VALUE
        private set
    var w = 0
        private set
    var h = 0
        private set
    var blur = -1
        private set
    var visible = false
        private set
    var strength = -1f
        private set

    fun ensureShown() {
        if (shown) return
        if (tryShowDialog()) {
            usingDialog = true
            shown = true
            return
        }
        tryShowFallback()
        usingDialog = false
        shown = fallbackView != null
    }

    fun hide() {
        dialog?.let { runCatching { it.dismiss() } }
        dialog = null
        dialogContent = null
        fallbackView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        fallbackView = null
        fallbackParams = null
        shown = false
        usingDialog = false
        x = Int.MIN_VALUE
        y = Int.MIN_VALUE
        w = 0
        h = 0
        blur = -1
        visible = false
        strength = -1f
    }

    fun update(
        targetX: Int,
        targetY: Int,
        targetW: Int,
        targetH: Int,
        blurRadius: Int,
        strength: Float,
        visible: Boolean,
        blurEnabled: Boolean,
        force: Boolean,
    ) {
        ensureShown()
        val show = visible && strength > 0.08f
        val changed = force ||
            abs(x - targetX) > 4 ||
            abs(y - targetY) > 4 ||
            abs(w - targetW) > 6 ||
            abs(h - targetH) > 6 ||
            abs(this.strength - strength) > 0.03f ||
            blur != blurRadius ||
            this.visible != show

        paintShape(strength, blurEnabled, show)

        if (!changed && shown) return

        x = targetX
        y = targetY
        w = if (show) targetW else 1
        h = if (show) targetH else 1
        blur = blurRadius
        this.visible = show
        this.strength = strength

        val radius = if (show && blurEnabled) blurRadius else 0
        if (usingDialog) {
            layoutDialog(x, y, w, h, radius, show)
        } else {
            layoutFallback(x, y, w, h, radius, show)
        }
    }

    private fun paintShape(strength: Float, blurEnabled: Boolean, show: Boolean) {
        val alpha = when {
            !show -> 0
            blurEnabled -> (10 + 16 * strength).roundToInt().coerceIn(8, 28)
            else -> (18 + 28 * strength).roundToInt().coerceIn(12, 46)
        }
        shape.setColor(Color.argb(alpha, GlassLook.HAZE_R_BYTE, GlassLook.HAZE_G_BYTE, GlassLook.HAZE_B_BYTE))
    }

    private fun tryShowDialog(): Boolean {
        return runCatching {
            val themed = ContextThemeWrapper(appContext, R.style.Theme_CristalGiro_FrostPatch)
            val created = Dialog(themed, R.style.Theme_CristalGiro_FrostPatch)
            created.setCancelable(false)
            created.setCanceledOnTouchOutside(false)
            val content = View(themed).apply { background = shape }
            created.setContentView(content)
            dialogContent = content
            val window = created.window ?: return@runCatching false
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            window.setBackgroundDrawable(shape)
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
            params.title = "CristalGiro.patch"
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

    private fun tryShowFallback() {
        runCatching {
            val view = View(appContext).apply { background = shape }
            val params = overlayParams(1, 1)
            windowManager.addView(view, params)
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    BackdropBlur.bindToView(v, 0)
                }

                override fun onViewDetachedFromWindow(v: View) = Unit
            })
            fallbackView = view
            fallbackParams = params
        }
    }

    private fun layoutDialog(x: Int, y: Int, w: Int, h: Int, radius: Int, show: Boolean) {
        val window = dialog?.window ?: return
        val params = window.attributes
        params.x = x
        params.y = y
        params.width = w
        params.height = h
        params.gravity = Gravity.TOP or Gravity.START
        window.attributes = params
        window.setBackgroundDrawable(shape)
        dialogContent?.background = shape
        BackdropBlur.applyToWindow(window, radius)
        window.decorView.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun layoutFallback(x: Int, y: Int, w: Int, h: Int, radius: Int, show: Boolean) {
        val view = fallbackView ?: return
        val params = fallbackParams ?: return
        params.x = x
        params.y = y
        params.width = w
        params.height = h
        view.visibility = if (show) View.VISIBLE else View.GONE
        view.background = shape
        runCatching { windowManager.updateViewLayout(view, params) }
        if (show) {
            BackdropBlur.bindToView(view, radius)
        }
    }

    private fun overlayParams(width: Int, height: Int): WindowManager.LayoutParams {
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
            title = "CristalGiro.patch"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }
}
