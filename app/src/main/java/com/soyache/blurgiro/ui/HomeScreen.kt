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
    blurApiSupported: Boolean,
    crossWindowBlurEnabled: Boolean,
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
    onOpenDeveloperSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val canActivate = canDrawOverlays && blurApiSupported && crossWindowBlurEnabled

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
                    "porque el compositor desenfoca lo que hay detrás de unas bandas flotantes. " +
                    "Al volver de frente se quitan las bandas. No grabamos la pantalla.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )

            if (!blurApiSupported) {
                PermissionCard(
                    title = "Hace falta Android 12 o superior",
                    body = "El desenfoque de otras ventanas (Window.setBackgroundBlurRadius) solo existe " +
                        "desde Android 12. En este aparato no hay API pública para desenfocar el launcher " +
                        "sin capturar la pantalla, y CristalGiro se niega a grabar.",
                    action = null,
                    onAction = {},
                )
            } else if (!crossWindowBlurEnabled) {
                PermissionCard(
                    title = "El teléfono tiene el desenfoque entre ventanas desactivado",
                    body = "CristalGiro usa la API pública del compositor (cross-window blur) para " +
                        "desenfocar otras apps bajo ventanas translúcidas. Aquí " +
                        "WindowManager.isCrossWindowBlurEnabled es falso: el sistema o el fabricante " +
                        "no aplican blur cruzado.\n\n" +
                        "Sin eso no podemos desenfocar el launcher ni otras apps. No vamos a pintar " +
                        "un velo ni a pedir captura de pantalla.\n\n" +
                        "En Pixel y AOSP a veces se enciende en Ajustes → Opciones de desarrollador → " +
                        "Representación acelerada por hardware → Permitir desenfoques a nivel de ventana. " +
                        "En muchas marcas (Xiaomi, Huawei, Oppo, Vivo, Samsung) no existe o no aplica " +
                        "a overlays: CristalGiro no puede saltarse esa política.",
                    action = "Abrir opciones de desarrollador",
                    onAction = onOpenDeveloperSettings,
                )
            }

            if (!canDrawOverlays) {
                PermissionCard(
                    title = "Permiso para mostrar sobre otras apps",
                    body = "Android llama a este permiso SYSTEM_ALERT_WINDOW («mostrar sobre otras apps»). " +
                        "CristalGiro lo usa solo para colocar bandas flotantes no táctiles. No bloquea " +
                        "toques y no lee el contenido de otras apps. Puedes quitarlo cuando quieras en Ajustes.",
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

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Esquema de bandas", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (hasSensor) {
                            "Inclina el teléfono. Las franjas claras marcan qué bandas pedirían radio " +
                                "al compositor (más claro = más blur). No es una simulación del cristal " +
                                "ni un velo sobre un launcher falso. El efecto real solo se ve sobre " +
                                "otras apps si el blur cruzado está encendido."
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
                        onActivate()
                    }
                },
                enabled = overlayOn || canActivate,
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
                "Sin anuncios, sin rastreo, sin root y sin captura de pantalla. " +
                    "El desenfoque lo hace el compositor dentro de cada banda " +
                    "(setBackgroundBlurRadius). Si el OEM lo apaga, la capa no finge un velo. " +
                    "Los toques atraviesan las bandas. Se pausa al apagar la pantalla.",
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
    action: String?,
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
            if (action != null) {
                TextButton(onClick = onAction) { Text(action) }
            }
        }
    }
}
