package com.soyache.blurgiro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soyache.blurgiro.overlay.OverlayService
import com.soyache.blurgiro.ui.HomeScreen
import com.soyache.blurgiro.ui.HomeViewModel
import com.soyache.blurgiro.ui.theme.CristalGiroTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private var canDrawOverlays by mutableStateOf(false)
    private var notificationsGranted by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshPermissions()
        lifecycle.addObserver(viewModel.previewLifecycle)

        setContent {
            val overlayOn by OverlayService.running.collectAsStateWithLifecycle()
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                notificationsGranted = granted
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            CristalGiroTheme {
                HomeScreen(
                    overlayOn = overlayOn,
                    canDrawOverlays = canDrawOverlays,
                    notificationsGranted = notificationsGranted,
                    intensity = viewModel.intensity,
                    smoothness = viewModel.smoothness,
                    mode = viewModel.mode,
                    tiltX = viewModel.tiltX,
                    tiltY = viewModel.tiltY,
                    hasSensor = viewModel.hasSensor,
                    onIntensity = viewModel::setIntensity,
                    onSmoothness = viewModel::setSmoothness,
                    onMode = viewModel::setMode,
                    onToggle = { enable -> toggleOverlay(enable) },
                    onRequestOverlayPermission = { openOverlaySettings() },
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
        if (viewModel.settings.overlayRequested &&
            Settings.canDrawOverlays(this) &&
            !OverlayService.running.value
        ) {
            OverlayService.start(this)
        }
    }

    override fun onDestroy() {
        lifecycle.removeObserver(viewModel.previewLifecycle)
        super.onDestroy()
    }

    private fun refreshPermissions() {
        canDrawOverlays = Settings.canDrawOverlays(this)
        notificationsGranted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun toggleOverlay(enable: Boolean) {
        if (enable) {
            if (!Settings.canDrawOverlays(this)) {
                openOverlaySettings()
                return
            }
            OverlayService.start(this)
        } else {
            OverlayService.stop(this)
        }
    }

    private fun openOverlaySettings() {
        val uri = Uri.parse("package:$packageName")
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri))
    }
}
