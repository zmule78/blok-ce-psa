package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class AdaptiveColors(
    val isDark: Boolean,
    val cardContainer: Color,
    val cardElevated: Color,
    val subtleContainer: Color,
    val inputContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val divider: Color,
    val emeraldSurface: Color,
    val emeraldBorder: Color,
    val emeraldText: Color,
    val amberSurface: Color,
    val amberBorder: Color,
    val amberText: Color,
    val roseSurface: Color,
    val roseBorder: Color,
    val roseText: Color,
    val danaSurface: Color,
    val danaBorder: Color,
    val danaText: Color,
    val purpleSurface: Color,
    val purpleBorder: Color,
    val purpleText: Color
)

private val LightAdaptiveColors = AdaptiveColors(
    isDark = false,
    cardContainer = Color.White,
    cardElevated = Slate50,
    subtleContainer = Slate100,
    inputContainer = Color.White,
    textPrimary = Slate900,
    textSecondary = Slate700,
    textMuted = Slate600,
    borderSubtle = Slate200,
    divider = Slate200,
    emeraldSurface = Emerald50,
    emeraldBorder = Emerald200,
    emeraldText = Emerald700,
    amberSurface = Amber50,
    amberBorder = Amber100,
    amberText = Amber600,
    roseSurface = Rose50,
    roseBorder = Rose100,
    roseText = Rose600,
    danaSurface = DanaLight,
    danaBorder = DanaBlue.copy(alpha = 0.28f),
    danaText = DanaDark,
    purpleSurface = Purple100,
    purpleBorder = Purple700.copy(alpha = 0.25f),
    purpleText = Purple700
)

private val DarkAdaptiveColors = AdaptiveColors(
    isDark = true,
    cardContainer = Slate900,
    cardElevated = Slate800,
    subtleContainer = Slate800,
    inputContainer = Slate950,
    textPrimary = Slate50,
    textSecondary = Slate200,
    textMuted = Slate400,
    borderSubtle = Slate700,
    divider = Slate700,
    emeraldSurface = Color(0xFF052E22),
    emeraldBorder = Emerald600.copy(alpha = 0.55f),
    emeraldText = Emerald300,
    amberSurface = Color(0xFF2D1E08),
    amberBorder = Amber600.copy(alpha = 0.55f),
    amberText = Color(0xFFFCD34D),
    roseSurface = Color(0xFF3B0A18),
    roseBorder = Rose600.copy(alpha = 0.55f),
    roseText = Color(0xFFFDA4AF),
    danaSurface = Color(0xFF0A2945),
    danaBorder = DanaBlue.copy(alpha = 0.55f),
    danaText = Color(0xFF7DD3FC),
    purpleSurface = Color(0xFF2C1346),
    purpleBorder = Color(0xFFC084FC).copy(alpha = 0.5f),
    purpleText = Color(0xFFE9D5FF)
)

val LocalAdaptiveColors = staticCompositionLocalOf { LightAdaptiveColors }

val adaptiveColors: AdaptiveColors
    @Composable get() = LocalAdaptiveColors.current

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald950,
    inversePrimary = Emerald300,
    secondary = Emerald600,
    onSecondary = Color.White,
    secondaryContainer = Emerald50,
    onSecondaryContainer = Emerald900,
    tertiary = Teal700,
    onTertiary = Color.White,
    tertiaryContainer = Emerald100,
    onTertiaryContainer = Teal900,
    surfaceTint = Emerald700,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Slate100,
    surfaceContainerHighest = Slate200,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Rose600,
    onError = Color.White,
    errorContainer = Rose100,
    onErrorContainer = Rose600
)

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Emerald950,
    primaryContainer = Emerald900,
    onPrimaryContainer = Emerald100,
    inversePrimary = Emerald700,
    secondary = Emerald400,
    onSecondary = Emerald950,
    secondaryContainer = Emerald950,
    onSecondaryContainer = Emerald100,
    tertiary = Teal400,
    onTertiary = Emerald950,
    tertiaryContainer = Teal900,
    onTertiaryContainer = Emerald100,
    surfaceTint = Emerald500,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate200,
    surfaceContainerLowest = Slate950,
    surfaceContainerLow = Slate900,
    surfaceContainer = Slate900,
    surfaceContainerHigh = Slate800,
    surfaceContainerHighest = Slate700,
    outline = Slate600,
    outlineVariant = Slate700,
    error = Color(0xFFFB7185),
    onError = Slate950,
    errorContainer = Color(0xFF4C0519),
    onErrorContainer = Rose100
)

@Composable
fun adaptiveFieldColors(): TextFieldColors {
    val ac = adaptiveColors
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = ac.textPrimary,
        unfocusedTextColor = ac.textPrimary,
        disabledTextColor = ac.textMuted,
        errorTextColor = ac.textPrimary,
        focusedContainerColor = ac.inputContainer,
        unfocusedContainerColor = ac.inputContainer,
        disabledContainerColor = ac.subtleContainer,
        errorContainerColor = ac.inputContainer,
        focusedBorderColor = primary,
        unfocusedBorderColor = ac.borderSubtle,
        disabledBorderColor = ac.borderSubtle.copy(alpha = 0.55f),
        errorBorderColor = error,
        focusedLabelColor = primary,
        unfocusedLabelColor = ac.textSecondary,
        disabledLabelColor = ac.textMuted,
        errorLabelColor = error,
        focusedPlaceholderColor = ac.textMuted,
        unfocusedPlaceholderColor = ac.textMuted,
        disabledPlaceholderColor = ac.textMuted,
        focusedLeadingIconColor = primary,
        unfocusedLeadingIconColor = ac.textMuted,
        disabledLeadingIconColor = ac.textMuted,
        errorLeadingIconColor = error,
        focusedTrailingIconColor = ac.textSecondary,
        unfocusedTrailingIconColor = ac.textMuted,
        disabledTrailingIconColor = ac.textMuted,
        errorTrailingIconColor = error,
        focusedSupportingTextColor = ac.textSecondary,
        unfocusedSupportingTextColor = ac.textMuted,
        errorSupportingTextColor = error,
        cursorColor = primary,
        errorCursorColor = error,
        selectionColors = TextSelectionColors(
            handleColor = primary,
            backgroundColor = primary.copy(alpha = 0.24f)
        )
    )
}

@Composable
fun BlokCePsaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val fontScaleMultiplier = when {
        screenWidthDp in 1..359 -> 0.94f
        screenWidthDp >= 600 -> 1.05f
        else -> 1.0f
    }
    val typography = remember(fontScaleMultiplier) {
        buildResponsiveTypography(fontScaleMultiplier)
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val adaptive = if (darkTheme) DarkAdaptiveColors else LightAdaptiveColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = AppShapes
    ) {
        CompositionLocalProvider(
            LocalAdaptiveColors provides adaptive,
            LocalContentColor provides colorScheme.onBackground
        ) {
            ProvideTextStyle(
                value = typography.bodyMedium.copy(color = colorScheme.onBackground)
            ) {
                content()
            }
        }
    }
}
