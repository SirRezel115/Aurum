package com.aurum.ui.theme

import androidx.compose.ui.graphics.Brush

/**
 * Gradient brushes for Aurum's Metallic Silver theme.
 */
val AurumBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        MidnightPurple900,
        MidnightPurple950,
    )
)

val AurumSilverGradient = Brush.horizontalGradient(
    colors = listOf(
        Silver400,
        Silver600,
    )
)

val AurumGoldGradient = AurumSilverGradient

val AurumCardGradient = Brush.linearGradient(
    colors = listOf(
        MidnightPurple800,
        MidnightPurple900,
    )
)
