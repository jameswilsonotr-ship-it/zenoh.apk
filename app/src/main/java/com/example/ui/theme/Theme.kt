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

private val DarkColorScheme =
  darkColorScheme(
    primary = ElegantDarkPrimary,
    onPrimary = ElegantDarkOnPrimary,
    secondaryContainer = ElegantDarkSecondaryContainer,
    onSecondaryContainer = ElegantDarkOnSecondaryContainer,
    background = ElegantDarkBackground,
    onBackground = ElegantDarkOnSurface,
    surface = ElegantDarkSurface,
    onSurface = ElegantDarkOnSurface,
    surfaceVariant = ElegantDarkSurface,
    onSurfaceVariant = ElegantDarkOnSurfaceVariant,
    outline = ElegantDarkOutline,
    outlineVariant = ElegantDarkOutlineVariant
  )

private val LightColorScheme = DarkColorScheme // Force all modes to Elegant Dark for visual consistency

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve "Elegant Dark" branding
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme


  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
