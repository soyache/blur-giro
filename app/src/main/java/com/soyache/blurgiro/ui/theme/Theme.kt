package com.soyache.blurgiro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ice = Color(0xFF8EC8FF)
private val IceDim = Color(0xFF5B8FBF)
private val Lilac = Color(0xFFC4B5FD)
private val Navy = Color(0xFF0B1020)
private val Surface = Color(0xFF141A2E)
private val SurfaceHi = Color(0xFF1D2540)
private val On = Color(0xFFE8EEF8)
private val OnMuted = Color(0xFFA8B3C7)

private val Scheme = darkColorScheme(
    primary = Ice,
    onPrimary = Color(0xFF082033),
    primaryContainer = IceDim,
    onPrimaryContainer = On,
    secondary = Lilac,
    onSecondary = Color(0xFF1A1030),
    background = Navy,
    onBackground = On,
    surface = Surface,
    onSurface = On,
    surfaceVariant = SurfaceHi,
    onSurfaceVariant = OnMuted,
    outline = Color(0xFF3A4663),
    error = Color(0xFFFFB4AB),
)

@Composable
fun CristalGiroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        content = content,
    )
}
