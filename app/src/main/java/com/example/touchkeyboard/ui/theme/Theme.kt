package com.example.touchkeyboard.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark color scheme (default for the app)
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTurquoise,
    onPrimary = BackgroundDark,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = TextPrimary,
    secondary = SecondaryYellow,
    onSecondary = BackgroundDark,
    secondaryContainer = SecondaryVariant,
    onSecondaryContainer = TextPrimary,
    tertiary = Info,
    onTertiary = TextPrimary,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = BackgroundMedium,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondary,
    error = Error,
    onError = TextPrimary
)

// Light color scheme (not used but defined for completeness)
private val LightColorScheme = lightColorScheme(
    primary = PrimaryDark,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = BackgroundDark,
    secondary = SecondaryDark,
    onSecondary = TextPrimary,
    secondaryContainer = SecondaryVariant,
    onSecondaryContainer = BackgroundDark,
    tertiary = Info,
    onTertiary = TextPrimary,
    background = TextPrimary,
    onBackground = BackgroundDark,
    surface = TextSecondary,
    onSurface = BackgroundDark,
    surfaceVariant = TextSecondary,
    onSurfaceVariant = BackgroundMedium,
    error = Error,
    onError = TextPrimary
)

@Composable
fun TouchKeyboardTheme(
    darkTheme: Boolean = true, // Default to dark theme
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}