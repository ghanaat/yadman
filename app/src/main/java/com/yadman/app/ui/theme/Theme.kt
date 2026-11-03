package com.yadman.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00201C),
    secondaryContainer = Color(0xFFD7E8E4),
    onSecondaryContainer = Color(0xFF122420),
    tertiary = Color(0xFFD9822B),
    tertiaryContainer = Color(0xFFFFE0C2),
    onTertiaryContainer = Color(0xFF2E1500),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFD7E8E4),
    tertiary = Color(0xFFFFB77C),
    tertiaryContainer = Color(0xFF6B3B00),
    onTertiaryContainer = Color(0xFFFFE0C2),
)

@Composable
fun YadmanTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
