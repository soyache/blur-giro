package com.soyache.blurgiro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soyache.blurgiro.data.BlurMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPane(
    intensity: Float,
    smoothness: Float,
    mode: BlurMode,
    tiltX: Float,
    tiltY: Float,
    hasSensor: Boolean,
    isDefaultHome: Boolean,
    onIntensity: (Float) -> Unit,
    onSmoothness: (Float) -> Unit,
    onMode: (BlurMode) -> Unit,
    onChooseHome: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CristalGiro", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Launcher 0.2.0",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
        containerColor = colors.background.copy(alpha = 0.96f),
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
                "De frente, el inicio se ve normal: iconos nítidos, sin warp. " +
                    "Si giras el teléfono en X, el lado que se aleja se desenfoca un poco " +
                    "(los iconos quedan como manchas blandas) y el cercano sigue claro. " +
                    "Es un degradado a toda la pantalla, no un parche. Al abrir otra app " +
                    "el efecto se para: no nos superponemos a nada.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )

            if (!isDefaultHome) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Home, contentDescription = null, tint = colors.primary)
                            Text("Aún no es la app de inicio", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "Para usar el cristal como pantalla de inicio: Ajustes → apps predeterminadas → " +
                                "app de inicio → CristalGiro. O pulsa el botón.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                        Button(onClick = onChooseHome, modifier = Modifier.fillMaxWidth()) {
                            Text("Elegir app de inicio")
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ajustes del cristal", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (hasSensor) {
                            "Inclina el teléfono. Giro X ≈ ${"%.2f".format(tiltX)} · Y ≈ ${"%.2f".format(tiltY)}"
                        } else {
                            "Este aparato no expone giroscopio. El inicio quedará nítido."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Text("Intensidad (un poco)", style = MaterialTheme.typography.labelLarge)
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
                "Sin anuncios, sin rastreo, sin root, sin captura de pantalla y sin capa " +
                    "sobre otras apps. Pintamos wallpaper e iconos y les aplicamos StackBlur " +
                    "según el giro. El overlay de 0.1.x no podía desenfocar el launcher en " +
                    "la mayoría de fabricantes: ellos no abren el blur cruzado a terceros.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
