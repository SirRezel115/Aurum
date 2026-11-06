package com.aurum.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Top-level Compose theme for Aurum.
 */
@Composable
fun AurumTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        LocalInspectionMode.current -> LightAurumColors
        darkTheme -> DarkAurumColors
        else -> LightAurumColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = rememberAurumTypography(),
        content = content,
    )
}
