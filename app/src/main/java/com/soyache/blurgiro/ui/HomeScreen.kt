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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    captureDenied: Boolean,
    intensity: Float,
    smoothness: Float,
    mode: BlurMode,
    tiltX: Float,
    tiltY: Float,
    hasSensor: Boolean,
    onIntensity: (Float) -> Unit,
    onSmoothness: (Float) -> Unit,
    onMode: (BlurMode) -> Unit,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var showCaptureDialog by rememberSaveable { mutableStateOf(false) }

    if (showCaptureDialog) {
        AlertDialog(
            onDismissRequest = { showCaptureDialog = false },
            title = { Text("Permiso para capturar la pantalla") },
            text = {
                Text(
                    "Para que los iconos se vean desenfocados de verdad (como cuando giras el teléfono " +
                        "hacia un lado), CristalGiro necesita ver lo que hay en pantalla. Android te va a " +
                        "pedir permiso de captura.\n\n" +
                        "No se guarda nada, no se envía a internet y no se activa a tus espaldas: solo " +
                        "mientras la capa está encendida y tú lo acabas de conceder. Los toques siguen " +
                        "pasando a las otras apps.\n\n" +
                        "Si lo niegas, no activamos la capa. Un velo pintado no se parece a lo que pediste.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCaptureDialog = false
                        onActivate()
                    },
                ) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = { showCaptureDialog = false }) { Text("Cancelar") }
            },
        )
    }

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
                "De frente el teléfono se ve normal: sin blur. Si lo giras en X (izquierda o derecha), " +
                    "el lado que se aleja se desenfoca un poco — los iconos se quedan, pero blandos — " +
                    "y la pantalla parece que también gira. Al volver de frente se quita el blur de todos los lugares.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )

            if (!canDrawOverlays) {
                PermissionCard(
                    title = "Permiso para mostrar sobre otras apps",
                    body = "Android llama a este permiso SYSTEM_ALERT_WINDOW («mostrar sobre otras apps»). " +
                        "CristalGiro lo usa solo para dibujar la capa encima. No bloquea toques: las demás " +
                        "apps se siguen usando. Puedes quitarlo cuando quieras en Ajustes.",
                    action = "Conceder permiso",
                    onAction = onRequestOverlayPermission,
                )
            }

            if (!notificationsGranted) {
                PermissionCard(
                    title = "Aviso silencioso",
                    body = "Mientras el cristal está activo, Android exige un servicio en primer plano con " +
                        "una notificación permanente y silenciosa. No envía alertas ni rastrea nada.",
                    action = "Permitir notificación",
                    onAction = onRequestNotifications,
                    iconNotifications = true,
                )
            }

            if (captureDenied && !overlayOn) {
                PermissionCard(
                    title = "Hace falta el permiso de captura",
                    body = "Sin captura no podemos desenfocar los iconos de verdad. Un tinte o una niebla " +
                        "no sirven: se vería un velo, no el contenido fuera de foco. Vuelve a pulsar Activar " +
                        "y acepta el diálogo del sistema.",
                    action = "Reintentar",
                    onAction = { showCaptureDialog = true },
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
                            "Inclina el teléfono. Aquí se desenfoca este dibujo igual que la capa: " +
                                "lado que se aleja blando, lado cercano nítido, y un poco de perspectiva."
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
            }

            Button(
                onClick = {
                    if (overlayOn) {
                        onDeactivate()
                    } else if (!canDrawOverlays) {
                        onRequestOverlayPermission()
                    } else {
                        showCaptureDialog = true
                    }
                },
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
                            selected = mode == BlurMode.DIRECTIONAL,
                            onClick = { onMode(BlurMode.DIRECTIONAL) },
                            label = { Text("Lado (eje X)") },
                            leadingIcon = { Icon(Icons.Outlined.Layers, contentDescription = null) },
                        )
                        FilterChip(
                            selected = mode == BlurMode.CORNERS,
                            onClick = { onMode(BlurMode.CORNERS) },
                            label = { Text("Solo esquinas") },
                            leadingIcon = { Icon(Icons.Outlined.BlurOn, contentDescription = null) },
                        )
                    }
                }
            }

            Text(
                "Sin anuncios, sin rastreo y sin root. La captura solo vive en el teléfono mientras " +
                    "la capa está encendida. Apps con FLAG_SECURE (banca, DRM) salen en negro. " +
                    "Mientras inclinas ves el último fotograma limpio desenfocado; al volver de frente " +
                    "ves la pantalla real al instante. La capa se pausa al apagar la pantalla.",
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
