package com.soyache.blurgiro.effect

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.core.content.ContextCompat
import java.util.function.Consumer

/**
 * Consulta si el compositor permite desenfoque entre ventanas (Android 12+).
 *
 * Si es falso, el fabricante no habilitó blur de ventana para terceros
 * (`ro.surface_flinger.supports_background_blur` no está en 1, o el runtime
 * lo apagó). CristalGiro **no** activa un velo pintado ni captura de pantalla,
 * y no manda a Opciones de desarrollador: si el interruptor no existe, no hay
 * nada que encender.
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            onChange(false)
            return {}
        }
        val app = context.applicationContext
        val wm = app.getSystemService(WindowManager::class.java)
        val consumer = Consumer<Boolean> { enabled -> onChange(enabled) }
        return try {
            wm.addCrossWindowBlurEnabledListener(ContextCompat.getMainExecutor(app), consumer)
            ({
                runCatching { wm.removeCrossWindowBlurEnabledListener(consumer) }
                Unit
            })
        } catch (_: Throwable) {
            onChange(false)
            ({})
        }
    }
}
