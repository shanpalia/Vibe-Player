package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.AppThemeMode

@Composable
fun VibePlayerTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    accentIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val (primaryColor, primaryContainerColor) = AccentColors.getOrElse(accentIndex) { AccentColors[0] }
    val colorScheme = lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryContainerColor,
        onPrimaryContainer = VibeTextPrimary,
        secondary = primaryColor,
        onSecondary = Color.White,
        background = Color.White,
        onBackground = VibeTextPrimary,
        surface = Color.White,
        onSurface = VibeTextPrimary,
        surfaceVariant = VibeSurfaceVariantDark,
        onSurfaceVariant = VibeTextSecondary,
        outline = VibeBorderDark,
        error = VibeRed,
        onError = Color.White
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.White.toArgb()
                window.navigationBarColor = Color.White.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
