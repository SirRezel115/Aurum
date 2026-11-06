package com.aurum.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Dark color scheme - Premium Black & Metallic Silver.
 */
val DarkAurumColors = darkColorScheme(
    primary = MutedSilver,
    onPrimary = Black,
    primaryContainer = MutedSilver,
    onPrimaryContainer = PureBlack,
    secondary = MutedSilver,
    onSecondary = Black,
    secondaryContainer = CharcoalSilver,
    onSecondaryContainer = PureWhite,
    tertiary = MutedSilver,
    onTertiary = Black,
    tertiaryContainer = DarkCharcoal,
    onTertiaryContainer = PureWhite,
    error = DarkError,
    onError = PureWhite,
    background = DeepBlack,
    onBackground = MutedSilver,
    surface = DarkCharcoal,
    onSurface = PureWhite,
    surfaceVariant = CharcoalSilver,
    onSurfaceVariant = Color(0xFFC0C8D0),
    surfaceContainer = CharcoalSilver,
    surfaceContainerHigh = DarkCharcoal,
    surfaceContainerHighest = DarkCharcoal,
    outline = NeutralSilverGray,
    outlineVariant = CharcoalSilver,
)

/**
 * Light color scheme - Metallic Silver on Pure White.
 */
val LightAurumColors = lightColorScheme(
    primary = MutedSilver,
    onPrimary = PureWhite,
    primaryContainer = LightRailSilver,
    onPrimaryContainer = PureBlack,
    secondary = MutedSilver,
    onSecondary = PureWhite,
    secondaryContainer = LightRailSilver,
    onSecondaryContainer = PureBlack,
    tertiary = MutedSilver,
    onTertiary = PureWhite,
    tertiaryContainer = LightRailSilver,
    onTertiaryContainer = PureBlack,
    error = LightError,
    onError = PureWhite,
    background = PureWhite,
    onBackground = MutedSilver,
    surface = PureWhite,
    onSurface = PureBlack,
    surfaceVariant = LightRailSilver,
    onSurfaceVariant = NeutralSilverGray,
    surfaceContainer = LightRailSilver,
    surfaceContainerHigh = PureWhite,
    surfaceContainerHighest = PureWhite,
    outline = LightRailSilver,
    outlineVariant = LightRailSilver,
)
