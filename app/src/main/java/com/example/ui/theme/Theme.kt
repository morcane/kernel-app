package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val OledDarkColorScheme = darkColorScheme(
    primary = AccentEmerald,
    onPrimary = OledBackground,
    primaryContainer = SurfaceHighlight,
    onPrimaryContainer = AccentEmerald,
    secondary = AccentCyan,
    onSecondary = OledBackground,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentMonochrome,
    onTertiary = OledBackground,
    background = OledBackground,
    onBackground = TextPrimary,
    surface = SurfaceGraphite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = SurfaceHighlight,
    error = AccentCrimson,
    onError = TextPrimary
)

@Composable
fun KernelTheme(
    useMonetDynamic: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (useMonetDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context).copy(
            background = OledBackground,
            surface = SurfaceGraphite
        )
    } else {
        OledDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KernelTypography,
        content = content
    )
}
