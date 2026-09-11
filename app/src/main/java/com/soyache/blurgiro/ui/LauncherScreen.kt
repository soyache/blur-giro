package com.soyache.blurgiro.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.soyache.blurgiro.R
import com.soyache.blurgiro.data.BlurMode
import com.soyache.blurgiro.launcher.CrystalHomeView
import com.soyache.blurgiro.launcher.InstalledApps
import com.soyache.blurgiro.launcher.LaunchApp

@Composable
fun LauncherScreen(
    grid: List<LaunchApp>,
    dock: List<LaunchApp>,
    wallpaper: android.graphics.Bitmap?,
    intensity: Float,
    smoothness: Float,
    mode: BlurMode,
    tiltX: Float,
    tiltY: Float,
    hasSensor: Boolean,
    isDefaultHome: Boolean,
    hideHomeHint: Boolean,
    onIntensity: (Float) -> Unit,
    onSmoothness: (Float) -> Unit,
    onMode: (BlurMode) -> Unit,
    onChooseHome: () -> Unit,
    onDismissHomeHint: () -> Unit,
) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    BackHandler {
        if (showSettings) {
            showSettings = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                CrystalHomeView(ctx).apply {
                    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
                        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                        (v as CrystalHomeView).setInsets(bars.left, bars.top, bars.right, bars.bottom)
                        insets
                    }
                    onAppClick = { app ->
                        runCatching { ctx.startActivity(InstalledApps.launchIntent(app)) }
                            .onFailure {
                                Toast.makeText(
                                    ctx,
                                    ctx.getString(R.string.cannot_open_app, app.label),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                    }
                    onSettingsClick = { showSettings = true }
                }
            },
            update = { view ->
                view.onSettingsClick = { showSettings = true }
                view.bind(
                    grid = grid,
                    dock = dock,
                    wallpaper = wallpaper,
                    tiltX = tiltX,
                    tiltY = tiltY,
                    intensity = intensity,
                    mode = mode,
                )
            },
        )

        AnimatedVisibility(
            visible = !isDefaultHome && !hideHomeHint && !showSettings,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.94f)),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.Home, contentDescription = null, tint = colors.primary)
                        Text(
                            "Pon CristalGiro como inicio",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(
                        "El cristal solo existe en esta pantalla de inicio: la pintamos nosotros " +
                            "(fondo + iconos). De frente se ve nítida. Al girar en X, el lado que " +
                            "se aleja se desenfoca un poco. Al abrir otra app el efecto se para.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        "1. Pulsa «Elegir app de inicio».\n" +
                            "2. Elige CristalGiro.\n" +
                            "3. Confirma «Siempre» si te lo pide.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismissHomeHint) { Text("Más tarde") }
                        Button(onClick = onChooseHome) { Text("Elegir app de inicio") }
                    }
                }
            }
        }

        if (showSettings) {
            SettingsPane(
                intensity = intensity,
                smoothness = smoothness,
                mode = mode,
                tiltX = tiltX,
                tiltY = tiltY,
                hasSensor = hasSensor,
                isDefaultHome = isDefaultHome,
                onIntensity = onIntensity,
                onSmoothness = onSmoothness,
                onMode = onMode,
                onChooseHome = onChooseHome,
                onClose = { showSettings = false },
            )
        }
    }
}
