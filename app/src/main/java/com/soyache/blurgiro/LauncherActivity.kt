package com.soyache.blurgiro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.soyache.blurgiro.launcher.HomeRole
import com.soyache.blurgiro.ui.LauncherScreen
import com.soyache.blurgiro.ui.LauncherViewModel
import com.soyache.blurgiro.ui.theme.CristalGiroTheme

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        lifecycle.addObserver(viewModel.previewLifecycle)

        setContent {
            CristalGiroTheme {
                LauncherScreen(
                    grid = viewModel.grid,
                    dock = viewModel.dock,
                    wallpaper = viewModel.wallpaper,
                    intensity = viewModel.intensity,
                    smoothness = viewModel.smoothness,
                    mode = viewModel.mode,
                    tiltX = viewModel.tiltX,
                    tiltY = viewModel.tiltY,
                    hasSensor = viewModel.hasSensor,
                    isDefaultHome = viewModel.isDefaultHome,
                    hideHomeHint = viewModel.hideHomeHint,
                    onIntensity = viewModel::updateIntensity,
                    onSmoothness = viewModel::updateSmoothness,
                    onMode = viewModel::updateMode,
                    onChooseHome = { HomeRole.openHomeChooser(this) },
                    onDismissHomeHint = viewModel::dismissHomeHint,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.refreshHomeRole()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshHomeRole()
    }

    override fun onDestroy() {
        lifecycle.removeObserver(viewModel.previewLifecycle)
        super.onDestroy()
    }
}
