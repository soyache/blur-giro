package com.soyache.blurgiro.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.soyache.blurgiro.data.AppSettings
import com.soyache.blurgiro.data.BlurMode
import com.soyache.blurgiro.launcher.HomeRole
import com.soyache.blurgiro.launcher.InstalledApps
import com.soyache.blurgiro.launcher.LaunchApp
import com.soyache.blurgiro.launcher.WallpaperStore
import com.soyache.blurgiro.sensor.TiltTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

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
    var grid by mutableStateOf<List<LaunchApp>>(emptyList())
        private set
    var dock by mutableStateOf<List<LaunchApp>>(emptyList())
        private set
    var wallpaper by mutableStateOf<Bitmap?>(null)
        private set
    var isDefaultHome by mutableStateOf(HomeRole.isDefaultHome(application))
        private set
    var hideHomeHint by mutableStateOf(settings.laterHomeHint)
        private set
    var loading by mutableStateOf(true)
        private set

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
            refreshHomeRole()
        }

        override fun onStop(owner: LifecycleOwner) {
            tracker.stop()
            tiltX = 0f
            tiltY = 0f
        }
    }

    init {
        viewModelScope.launch {
            val catalog = withContext(Dispatchers.Default) {
                InstalledApps.load(getApplication())
            }
            val paper = withContext(Dispatchers.Default) {
                WallpaperStore.load(getApplication())
            }
            grid = catalog.grid
            dock = catalog.dock
            wallpaper = paper
            loading = false
        }
    }

    fun refreshHomeRole() {
        isDefaultHome = HomeRole.isDefaultHome(getApplication())
        if (isDefaultHome) hideHomeHint = true
    }

    fun dismissHomeHint() {
        hideHomeHint = true
        settings.laterHomeHint = true
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
        tracker.stop()
        super.onCleared()
    }
}
