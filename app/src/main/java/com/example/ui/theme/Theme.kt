package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    tertiary = OceanTertiaryLight
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
    tertiary = SunsetTertiaryLight
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
    tertiary = NebulaTertiaryLight
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
    tertiary = LuxeTertiaryLight
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
    secondary = CyberSecondaryLight,
    tertiary = CyberTertiaryLight
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
    primary = SlatePrimaryLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
