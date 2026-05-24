package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    secondary = ImmersiveSecondary,
    tertiary = ImmersiveAccentCoral,
    background = ImmersiveBackground,
    surface = ImmersiveSurface,
    onBackground = Color(0xFFE3E2E6),
    onSurface = Color(0xFFE3E2E6)
  )

private val LightColorScheme =
  darkColorScheme( // Keep it dark and immersive even in light theme mode for arcade feel
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    secondary = ImmersiveSecondary,
    tertiary = ImmersiveAccentCoral,
    background = ImmersiveBackground,
    surface = ImmersiveSurface,
    onBackground = Color(0xFFE3E2E6),
    onSurface = Color(0xFFE3E2E6)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to true for the dark retro arcade / spaceship vibe
  dynamicColor: Boolean = false, // Disable dynamic colors by default so Immersive UI overrides it properly
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
