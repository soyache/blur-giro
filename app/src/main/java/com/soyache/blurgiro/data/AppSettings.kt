package com.soyache.blurgiro.data

import android.content.Context
import android.content.SharedPreferences

class AppSettings private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var intensity: Float
        get() = prefs.getFloat(KEY_INTENSITY, DEFAULT_INTENSITY)
        set(value) = prefs.edit().putFloat(KEY_INTENSITY, value.coerceIn(0.05f, 1f)).apply()

    var smoothness: Float
        get() = prefs.getFloat(KEY_SMOOTHNESS, DEFAULT_SMOOTHNESS)
        set(value) = prefs.edit().putFloat(KEY_SMOOTHNESS, value.coerceIn(0f, 1f)).apply()

    var mode: BlurMode
        get() = BlurMode.fromOrdinal(prefs.getInt(KEY_MODE, BlurMode.DIRECTIONAL.ordinal))
        set(value) = prefs.edit().putInt(KEY_MODE, value.ordinal).apply()

    var laterHomeHint: Boolean
        get() = prefs.getBoolean(KEY_LATER_HOME, false)
        set(value) = prefs.edit().putBoolean(KEY_LATER_HOME, value).apply()

    fun register(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregister(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val PREFS_NAME = "cristalgiro"
        const val KEY_INTENSITY = "intensity"
        const val KEY_SMOOTHNESS = "smoothness"
        const val KEY_MODE = "mode"
        const val KEY_LATER_HOME = "later_home_hint"

        /** «Un poco»: iconos lejanos blandos, todavía reconocibles. */
        const val DEFAULT_INTENSITY = 0.48f
        const val DEFAULT_SMOOTHNESS = 0.72f

        @Volatile
        private var instance: AppSettings? = null

        fun get(context: Context): AppSettings {
            return instance ?: synchronized(this) {
                instance ?: AppSettings(context).also { instance = it }
            }
        }
    }
}
