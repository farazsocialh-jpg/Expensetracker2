package com.expensetracker.presentation.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand colors
val PrimaryGreen = Color(0xFF00C853)
val PrimaryGreenDark = Color(0xFF00BFA5)
val OnPrimary = Color(0xFF003822)
val SurfaceDark = Color(0xFF0F1A14)
val SurfaceVariantDark = Color(0xFF1A2E20)
val CardDark = Color(0xFF1E3528)
val BackgroundDark = Color(0xFF0A1410)

val SurfaceLight = Color(0xFFF5FBF5)
val BackgroundLight = Color(0xFFEDF7ED)
val CardLight = Color(0xFFFFFFFF)

// Category colors
val CategoryColors = mapOf(
    "FOOD" to Color(0xFFFF6B35),
    "GROCERY" to Color(0xFF4CAF50),
    "TRANSPORT" to Color(0xFF2196F3),
    "FUEL" to Color(0xFFFF9800),
    "SHOPPING" to Color(0xFFE91E63),
    "BILLS" to Color(0xFF9C27B0),
    "HEALTH" to Color(0xFFF44336),
    "ENTERTAINMENT" to Color(0xFF00BCD4),
    "EDUCATION" to Color(0xFF3F51B5),
    "TRAVEL" to Color(0xFF009688),
    "OTHER" to Color(0xFF607D8B)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFF003822),
    onPrimaryContainer = Color(0xFF9DFFB8),
    secondary = PrimaryGreenDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = Color(0xFFE0F2E8),
    onSurface = Color(0xFFE0F2E8),
    onSurfaceVariant = Color(0xFFA0BFA8),
    error = Color(0xFFFF6B6B),
    outline = Color(0xFF3A5C42)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00875A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8F5D0),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF00796B),
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFDCEEE0),
    onBackground = Color(0xFF0D2015),
    onSurface = Color(0xFF0D2015),
    onSurfaceVariant = Color(0xFF3D5C46),
    error = Color(0xFFBA1A1A),
    outline = Color(0xFF6C9E76)
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
