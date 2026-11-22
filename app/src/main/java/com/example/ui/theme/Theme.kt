package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GameColorScheme =
  lightColorScheme(
    primary = HoneyPrimary,
    onPrimary = Color.White,
    primaryContainer = HoneyLight,
    onPrimaryContainer = DeepText,
    secondary = GrassGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9F7C5),
    onSecondaryContainer = Color(0xFF1A4705),
    tertiary = SkyWater,
    onTertiary = Color.White,
    background = CreamBackground,
    onBackground = DeepText,
    surface = WarmSurface,
    onSurface = DeepText,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = Color(0xFF6D4C41),
    error = AlarmRed,
    onError = Color.White,
    outline = Color(0xFFD7B88A),
  )

val GameShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
  )

@Composable
fun SaveTheDogTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = GameColorScheme,
    typography = AppTypography,
    shapes = GameShapes,
    content = content,
  )
}
