package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Brass,
    onPrimary = Graphite,
    primaryContainer = PanelElevated,
    onPrimaryContainer = Brass,
    secondary = Dim,
    onSecondary = Graphite,
    secondaryContainer = Panel,
    onSecondaryContainer = Pearl,
    tertiary = Sage,
    onTertiary = Graphite,
    background = Graphite,
    onBackground = Pearl,
    surface = Panel,
    onSurface = Pearl,
    surfaceVariant = PanelElevated,
    onSurfaceVariant = Dim,
    outline = LineBorder,
    error = RoseError
)

private val LightColorScheme = lightColorScheme(
    primary = BrassDark,
    onPrimary = Pearl,
    primaryContainer = PageLight,
    onPrimaryContainer = TextLight,
    secondary = MutedLight,
    onSecondary = Pearl,
    secondaryContainer = PageLight,
    onSecondaryContainer = TextLight,
    tertiary = Sage,
    onTertiary = TextLight,
    background = PageLight,
    onBackground = TextLight,
    surface = Pearl,
    onSurface = TextLight,
    surfaceVariant = LineBorder,
    onSurfaceVariant = MutedLight,
    outline = LineBorder,
    error = RoseError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
