package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.AppThemePreset

// 1. EMERALD AURORA
private val EmeraldDarkScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldOnPrimaryContainerDark,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SlateSecondaryContainerDark,
    onSecondaryContainer = SlateOnSecondaryContainerDark,
    tertiary = CyanTertiaryDark,
    background = SurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

private val EmeraldLightScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldPrimaryContainerLight,
    onPrimaryContainer = EmeraldOnPrimaryContainerLight,
    secondary = SlateSecondaryLight,
    onSecondary = SlateOnSecondaryLight,
    secondaryContainer = SlateSecondaryContainerLight,
    onSecondaryContainer = SlateOnSecondaryContainerLight,
    tertiary = CyanTertiaryLight,
    background = SurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

// 2. OCEAN INDIGO
private val OceanDarkScheme = darkColorScheme(
    primary = OceanPrimaryDark,
    onPrimary = OceanOnPrimaryDark,
    primaryContainer = OceanPrimaryContainerDark,
    onPrimaryContainer = OceanOnPrimaryContainerDark,
    secondary = OceanSecondaryDark,
    tertiary = OceanTertiaryDark,
    background = OceanSurfaceDark,
    surface = OceanSurfaceDark,
    surfaceVariant = OceanSurfaceVariantDark
)

private val OceanLightScheme = lightColorScheme(
    primary = OceanPrimaryLight,
    onPrimary = OceanOnPrimaryLight,
    primaryContainer = OceanPrimaryContainerLight,
    onPrimaryContainer = OceanOnPrimaryContainerLight,
    secondary = OceanSecondaryLight,
    tertiary = OceanTertiaryLight,
    background = Color(0xFFF6F9FF),
    surface = Color(0xFFF6F9FF),
    surfaceVariant = Color(0xFFE0E8F5)
)

// 3. SUNSET GLOW
private val SunsetDarkScheme = darkColorScheme(
    primary = SunsetPrimaryDark,
    onPrimary = SunsetOnPrimaryDark,
    primaryContainer = SunsetPrimaryContainerDark,
    onPrimaryContainer = SunsetOnPrimaryContainerDark,
    secondary = SunsetSecondaryDark,
    tertiary = SunsetTertiaryDark,
    background = SunsetSurfaceDark,
    surface = SunsetSurfaceDark,
    surfaceVariant = SunsetSurfaceVariantDark
)

private val SunsetLightScheme = lightColorScheme(
    primary = SunsetPrimaryLight,
    onPrimary = SunsetOnPrimaryLight,
    primaryContainer = SunsetPrimaryContainerLight,
    onPrimaryContainer = SunsetOnPrimaryContainerLight,
    secondary = SunsetSecondaryLight,
    tertiary = SunsetTertiaryLight,
    background = Color(0xFFFFF8F7),
    surface = Color(0xFFFFF8F7),
    surfaceVariant = Color(0xFFF5E1E3)
)

// 4. MIDNIGHT NEBULA
private val NebulaDarkScheme = darkColorScheme(
    primary = NebulaPrimaryDark,
    onPrimary = NebulaOnPrimaryDark,
    primaryContainer = NebulaPrimaryContainerDark,
    onPrimaryContainer = NebulaOnPrimaryContainerDark,
    secondary = NebulaSecondaryDark,
    tertiary = NebulaTertiaryDark,
    background = NebulaSurfaceDark,
    surface = NebulaSurfaceDark,
    surfaceVariant = NebulaSurfaceVariantDark
)

private val NebulaLightScheme = lightColorScheme(
    primary = NebulaPrimaryLight,
    onPrimary = NebulaOnPrimaryLight,
    primaryContainer = NebulaPrimaryContainerLight,
    onPrimaryContainer = NebulaOnPrimaryContainerLight,
    secondary = NebulaSecondaryLight,
    tertiary = NebulaTertiaryLight,
    background = Color(0xFFFAF7FF),
    surface = Color(0xFFFAF7FF),
    surfaceVariant = Color(0xFFECE2F5)
)

// 5. GOLDEN LUXE
private val LuxeDarkScheme = darkColorScheme(
    primary = LuxePrimaryDark,
    onPrimary = LuxeOnPrimaryDark,
    primaryContainer = LuxePrimaryContainerDark,
    onPrimaryContainer = LuxeOnPrimaryContainerDark,
    secondary = LuxeSecondaryDark,
    tertiary = LuxeTertiaryDark,
    background = LuxeSurfaceDark,
    surface = LuxeSurfaceDark,
    surfaceVariant = LuxeSurfaceVariantDark
)

private val LuxeLightScheme = lightColorScheme(
    primary = LuxePrimaryLight,
    onPrimary = LuxeOnPrimaryLight,
    primaryContainer = LuxePrimaryContainerLight,
    onPrimaryContainer = LuxeOnPrimaryContainerLight,
    secondary = LuxeSecondaryLight,
    tertiary = LuxeTertiaryLight,
    background = Color(0xFFFFFBF5),
    surface = Color(0xFFFFFBF5),
    surfaceVariant = Color(0xFFF2E8D5)
)

// 6. CYBERPUNK NEON
private val CyberDarkScheme = darkColorScheme(
    primary = CyberPrimaryDark,
    onPrimary = CyberOnPrimaryDark,
    primaryContainer = CyberPrimaryContainerDark,
    onPrimaryContainer = CyberOnPrimaryContainerDark,
    secondary = CyberSecondaryDark,
    tertiary = CyberTertiaryDark,
    background = CyberSurfaceDark,
    surface = CyberSurfaceDark,
    surfaceVariant = CyberSurfaceVariantDark
)

private val CyberLightScheme = lightColorScheme(
    primary = CyberPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F3FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = CyberSecondaryLight,
    tertiary = CyberTertiaryLight,
    background = Color(0xFFF4FAFC),
    surface = Color(0xFFF4FAFC),
    surfaceVariant = Color(0xFFDCEBF0)
)

// 7. TITANIUM SLATE
private val SlateDarkScheme = darkColorScheme(
    primary = SlatePrimaryDark,
    onPrimary = SlateOnPrimaryDark,
    primaryContainer = SlatePrimaryContainerDark,
    onPrimaryContainer = SlateOnPrimaryContainerDark,
    background = SlateSurfaceDarkSolid,
    surface = SlateSurfaceDarkSolid,
    surfaceVariant = SlateSurfaceVariantDarkSolid
)

private val SlateLightScheme = lightColorScheme(
    primary = SlatePrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF0F172A),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    },
    themePreset: AppThemePreset = AppThemePreset.EMERALD_AURORA,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when (themePreset) {
        AppThemePreset.MATERIAL_YOU -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) EmeraldDarkScheme else EmeraldLightScheme
            }
        }
        AppThemePreset.EMERALD_AURORA -> if (darkTheme) EmeraldDarkScheme else EmeraldLightScheme
        AppThemePreset.OCEAN_INDIGO -> if (darkTheme) OceanDarkScheme else OceanLightScheme
        AppThemePreset.SUNSET_GLOW -> if (darkTheme) SunsetDarkScheme else SunsetLightScheme
        AppThemePreset.MIDNIGHT_NEBULA -> if (darkTheme) NebulaDarkScheme else NebulaLightScheme
        AppThemePreset.GOLDEN_LUXE -> if (darkTheme) LuxeDarkScheme else LuxeLightScheme
        AppThemePreset.CYBERPUNK_NEON -> if (darkTheme) CyberDarkScheme else CyberLightScheme
        AppThemePreset.TITANIUM_SLATE -> if (darkTheme) SlateDarkScheme else SlateLightScheme
    }

    MaterialTheme(
        colorScheme = if (themePreset == AppThemePreset.MATERIAL_YOU) colorScheme else colorScheme.withTonalSurfaces(darkTheme),
        typography = Typography,
        shapes = FinanceShapes,
        content = content
    )
}

/** One corner-radius scale for every card, sheet, chip and button. */
val FinanceShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * The palettes only define a few colours; Material 3 falls back to its default (purple-grey) tones for
 * surface containers and outlines, which made panels look mismatched. Derive every container tone from
 * the palette's own surface, lightly tinted with its primary colour, so all panels share one family.
 */
private fun ColorScheme.withTonalSurfaces(dark: Boolean): ColorScheme {
    fun tone(lift: Float, tint: Float): Color {
        val lifted = (if (dark) Color.White else Color.Black).copy(alpha = lift).compositeOver(surface)
        return primary.copy(alpha = tint).compositeOver(lifted)
    }
    return if (dark) copy(
        surfaceContainerLowest = tone(0.00f, 0.00f),
        surfaceContainerLow = tone(0.035f, 0.02f),
        surfaceContainer = tone(0.055f, 0.03f),
        surfaceContainerHigh = tone(0.08f, 0.04f),
        surfaceContainerHighest = tone(0.11f, 0.05f),
        surfaceBright = tone(0.14f, 0.05f),
        surfaceDim = surface,
        outline = onSurface.copy(alpha = 0.32f).compositeOver(surface),
        outlineVariant = onSurface.copy(alpha = 0.12f).compositeOver(surface),
        surfaceTint = primary
    ) else copy(
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = tone(0.015f, 0.015f),
        surfaceContainer = tone(0.03f, 0.025f),
        surfaceContainerHigh = tone(0.045f, 0.035f),
        surfaceContainerHighest = tone(0.06f, 0.045f),
        surfaceBright = Color.White,
        surfaceDim = tone(0.08f, 0.03f),
        outline = onSurface.copy(alpha = 0.38f).compositeOver(surface),
        outlineVariant = onSurface.copy(alpha = 0.12f).compositeOver(surface),
        surfaceTint = primary
    )
}
