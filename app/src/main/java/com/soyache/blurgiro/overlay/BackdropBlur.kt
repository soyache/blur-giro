package com.soyache.blurgiro.overlay

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.Window

/**
 * Intenta pedir *background blur* (dentro de los bounds de la ventana),
 * no FLAG_BLUR_BEHIND (ese desenfoca toda la pantalla).
 */
object BackdropBlur {

    fun applyToWindow(window: Window?, radius: Int): Boolean {
        if (window == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return runCatching {
            window.setBackgroundBlurRadius(radius.coerceIn(0, 140))
            true
        }.getOrDefault(false)
    }

    fun bindToView(view: View, radius: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val parent = view.rootView?.parent ?: return false
        val clamped = radius.coerceIn(0, 140)
        return tryCreateDrawable(parent, view, clamped) || trySetRadius(parent, clamped)
    }

    private fun tryCreateDrawable(viewRoot: Any, view: View, radius: Int): Boolean {
        return runCatching {
            val create = viewRoot.javaClass.methods.firstOrNull { method ->
                method.name == "createBackgroundBlurDrawable" && method.parameterTypes.isEmpty()
            } ?: return false
            val drawable = create.invoke(viewRoot) as? Drawable ?: return false
            invokeIfPresent(drawable, "setBlurRadius", radius)
            invokeIfPresent(drawable, "setColor", Color.TRANSPARENT)
            view.background = drawable
            true
        }.getOrDefault(false)
    }

    private fun trySetRadius(viewRoot: Any, radius: Int): Boolean {
        return runCatching {
            val method = viewRoot.javaClass.methods.firstOrNull { candidate ->
                candidate.name == "updateBackgroundBlurRadius" &&
                    candidate.parameterTypes.size == 1
            } ?: return false
            method.invoke(viewRoot, radius)
            true
        }.getOrDefault(false)
    }

    private fun invokeIfPresent(target: Any, name: String, value: Int) {
        target.javaClass.methods.firstOrNull { method ->
            method.name == name && method.parameterTypes.size == 1
        }?.invoke(target, value)
    }
}
