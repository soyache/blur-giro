package com.soyache.blurgiro.effect

import android.content.Context
import android.os.Build
import android.view.WindowManager

/**
 * Consulta si el compositor permite desenfoque entre ventanas (Android 12+).
 *
 * Si es falso, CristalGiro **no** activa un velo pintado ni captura de pantalla:
 * el OEM o el runtime tienen el blur cruzado apagado.
 */
object CrossWindowBlur {

    fun isApiSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun isEnabled(context: Context): Boolean {
        if (!isApiSupported()) return false
        return runCatching {
            context.applicationContext.getSystemService(WindowManager::class.java)
                .isCrossWindowBlurEnabled
        }.getOrDefault(false)
    }

    /**
     * @return función para dejar de escuchar. El listener recibe el valor actual al registrarse.
     */
    fun listen(context: Context, onChange: (Boolean) -> Unit): () -> Unit {
        if (!isApiSupported()) {
            onChange(false)
            return {}
        }
        val app = context.applicationContext
        val wm = app.getSystemService(WindowManager::class.java)
        val consumer = java.util.function.Consumer<Boolean> { enabled -> onChange(enabled) }
        return runCatching {
            wm.addCrossWindowBlurEnabledListener(app.mainExecutor, consumer)
            {
                runCatching { wm.removeCrossWindowBlurEnabledListener(consumer) }
                Unit
            }
        }.getOrElse {
            onChange(false)
            {}
        }
    }
}
