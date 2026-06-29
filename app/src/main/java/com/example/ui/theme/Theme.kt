package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AIPhotoEditorDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6), // Neon Purple
    secondary = Color(0xFFEC4899), // Hot Pink
    tertiary = Color(0xFF06B6D4), // Cyan
    background = Color(0xFF0D0E15), // Deep Slate Black
    surface = Color(0xFF161925), // Slate Card
    surfaceVariant = Color(0xFF1F2336), // Lighter Slate Card
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Mode as requested
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our beautiful theme
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AIPhotoEditorDarkColorScheme,
        typography = Typography,
        content = content
    )
}
