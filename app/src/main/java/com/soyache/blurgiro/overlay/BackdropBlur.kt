package com.soyache.blurgiro.overlay

import android.os.Build
import android.view.Window

/**
 * Pide *background blur* acotado a los bounds de la ventana
 * ([Window.setBackgroundBlurRadius]). No usa FLAG_BLUR_BEHIND ni
 * [android.view.WindowManager.LayoutParams.setBlurBehindRadius]: esos
 * desenfocan toda la pantalla.
 */
object BackdropBlur {

    fun applyToWindow(window: Window?, radius: Int): Boolean {
        if (window == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return runCatching {
            window.setBackgroundBlurRadius(radius.coerceIn(0, 140))
            true
        }.getOrDefault(false)
    }
}
