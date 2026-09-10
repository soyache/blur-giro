package com.soyache.blurgiro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soyache.blurgiro.data.BlurMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    overlayOn: Boolean,
    canDrawOverlays: Boolean,
    notificationsGranted: Boolean,
    intensity: Float,
    smoothness: Float,
    mode: BlurMode,
    tiltX: Float,
    tiltY: Float,
    hasSensor: Boolean,
    compositorBlurLive: Boolean,
    onIntensity: (Float) -> Unit,
    onSmoothness: (Float) -> Unit,
    onMode: (BlurMode) -> Unit,
    onToggle: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CristalGiro", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (overlayOn) "Capa activa en todo el teléfono" else "Capa desactivada",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
        containerColor = colors.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Al inclinar el teléfono, el borde o las esquinas hacia los que giras se van de foco; el centro y el lado opuesto siguen más nítidos. En reposo el efecto se apaga: no hay un velo blanco ni un brillo a pantalla completa.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )

            if (!canDrawOverlays) {
                PermissionCard(
                    title = "Permiso para mostrar sobre otras apps",
                    body = "Android llama a este permiso SYSTEM_ALERT_WINDOW («mostrar sobre otras apps»). CristalGiro lo usa solo para dibujar una capa transparente. No captura la pantalla, no lee lo que hay debajo y no bloquea toques: las demás apps siguen usándose con normalidad. Puedes quitarlo cuando quieras en Ajustes.",
                    action = "Conceder permiso",
                    onAction = onRequestOverlayPermission,
                )
            }

            if (!notificationsGranted) {
                PermissionCard(
                    title = "Aviso silencioso",
                    body = "Mientras el cristal está activo, Android exige un servicio en primer plano con una notificación permanente y silenciosa. No envía alertas ni rastrea nada. Sin este permiso, el sistema puede ocultar ese aviso.",
                    action = "Permitir notificación",
                    onAction = onRequestNotifications,
                    iconNotifications = true,
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Vista previa", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (hasSensor) {
                            "Inclina el teléfono. Aquí sí podemos desenfocar este dibujo (es nuestro). Sobre otras apps el desenfoque real lo hace el compositor si el fabricante lo permite; si no, verás una niebla mate en el borde, no un brillo."
                        } else {
                            "Este aparato no expone giroscopio ni vector de rotación. El efecto quedará fijo."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    TiltPreview(
                        tiltX = tiltX,
                        tiltY = tiltY,
                        intensity = intensity,
                        mode = mode,
                        compositorBlurLive = compositorBlurLive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                    Text(
                        if (compositorBlurLive) {
                            "Este aparato permite desenfoque entre ventanas: la capa pedirá blur real en el borde."
                        } else {
                            "Este aparato no está aplicando desenfoque entre ventanas (muy habitual en overlays). La capa usa niebla mate direccional, sin velo claro."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = { onToggle(!overlayOn) },
                enabled = overlayOn || canDrawOverlays,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    if (overlayOn) "Desactivar" else "Activar",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (!canDrawOverlays) {
                OutlinedButton(
                    onClick = onRequestOverlayPermission,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Abrir ajustes de superposición")
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ajustes", style = MaterialTheme.typography.titleMedium)
                    Text("Intensidad", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = intensity,
                        onValueChange = onIntensity,
                        valueRange = 0.1f..1f,
                    )
                    Text("Suavidad del giro", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = smoothness,
                        onValueChange = onSmoothness,
                        valueRange = 0f..1f,
                    )
                    Text("Modo", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = mode == BlurMode.CORNERS,
                            onClick = { onMode(BlurMode.CORNERS) },
                            label = { Text("Solo esquinas") },
                            leadingIcon = { Icon(Icons.Outlined.BlurOn, contentDescription = null) },
                        )
                        FilterChip(
                            selected = mode == BlurMode.DIRECTIONAL,
                            onClick = { onMode(BlurMode.DIRECTIONAL) },
                            label = { Text("Lado direccional") },
                            leadingIcon = { Icon(Icons.Outlined.Layers, contentDescription = null) },
                        )
                    }
                }
            }

            Text(
                "Sin anuncios, sin rastreo y sin root. El desenfoque óptico de otras apps solo existe si el compositor del fabricante lo habilita (a veces en Opciones de desarrollador → «Permitir desenfoques a nivel de ventana»). Si no, CristalGiro no pinta un brillo: usa una niebla oscura que se corre con el giro. La capa se pausa al apagar la pantalla.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    iconNotifications: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (iconNotifications) Icons.Outlined.Notifications else Icons.Outlined.Info,
                    contentDescription = null,
                    tint = colors.primary,
                )
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}
