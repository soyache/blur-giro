package com.soyache.blurgiro.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.soyache.blurgiro.data.AppSettings
import com.soyache.blurgiro.data.BlurMode
import com.soyache.blurgiro.effect.CrossWindowBlur
import com.soyache.blurgiro.sensor.TiltTracker

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val settings = AppSettings.get(application)

    var intensity by mutableFloatStateOf(settings.intensity)
        private set
    var smoothness by mutableFloatStateOf(settings.smoothness)
        private set
    var mode by mutableStateOf(settings.mode)
        private set
    var tiltX by mutableFloatStateOf(0f)
        private set
    var tiltY by mutableFloatStateOf(0f)
        private set
    var compositorBlurLive by mutableStateOf(CrossWindowBlur.isEnabled(application))
        private set

    private var stopBlurListen: (() -> Unit)? = CrossWindowBlur.listen(application) { enabled ->
        compositorBlurLive = enabled
    }

    private val tracker = TiltTracker(
        context = application,
        smoothness = { smoothness },
        onTilt = { x, y ->
            tiltX = x
            tiltY = y
        },
    )

    val hasSensor: Boolean get() = tracker.hasSensor

    val previewLifecycle = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            tracker.start()
        }

        override fun onStop(owner: LifecycleOwner) {
            tracker.stop()
        }
    }

    fun updateIntensity(value: Float) {
        intensity = value
        settings.intensity = value
    }

    fun updateSmoothness(value: Float) {
        smoothness = value
        settings.smoothness = value
    }

    fun updateMode(value: BlurMode) {
        mode = value
        settings.mode = value
    }

    override fun onCleared() {
        stopBlurListen?.invoke()
        stopBlurListen = null
        tracker.stop()
        super.onCleared()
    }
}
