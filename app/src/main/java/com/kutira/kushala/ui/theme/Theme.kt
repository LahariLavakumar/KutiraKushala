package com.kutira.kushala.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

private val ArtisanBrown = Color(0xFF5D4037)
private val ArtisanGold = Color(0xFF8D6E63)
private val ArtisanCream = Color(0xFFF5F5F5)

private val LightColors = lightColorScheme(
    primary = ArtisanBrown,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7CCC8),
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = ArtisanGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFEBE9),
    surface = ArtisanCream,
    onSurface = Color(0xFF212121),
    background = ArtisanCream
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD7CCC8),
    onPrimary = Color(0xFF3E2723),
    secondary = Color(0xFFBCAAA4),
    onSecondary = Color(0xFF3E2723),
    surface = Color(0xFF212121),
    onSurface = ArtisanCream,
    background = Color(0xFF121212)
)

@Composable
fun KutiraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
