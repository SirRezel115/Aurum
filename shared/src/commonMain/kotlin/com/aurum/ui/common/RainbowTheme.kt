package com.aurum.ui.common

import androidx.compose.animation.core.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.aurum.ui.player.VisualizerColorScheme

/**
 * CompositionLocal providing the current visualizer color scheme across all screens.
 */
val LocalVisualizerColorScheme = compositionLocalOf { VisualizerColorScheme.RAINBOW }

/**
 * Standardized multi-color Rainbow Palette matching the visualizer.
 */
val RainbowGradientColors = listOf(
    Color(0xFF00E5FF),
    Color(0xFF00E676),
    Color(0xFFFFEA00),
    Color(0xFFFF9100),
    Color(0xFFFF1744),
    Color(0xFF9C27B0),
    Color(0xFF00E5FF)
)

val RainbowBrush = Brush.linearGradient(colors = RainbowGradientColors)

/**
 * Smoothly animated horizontal Rainbow Brush matching the visualizer.
 */
@Composable
fun rememberAnimatedRainbowBrush(): Brush {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    return remember(animatedOffset) {
        Brush.linearGradient(
            colors = RainbowGradientColors,
            start = Offset(animatedOffset, 0f),
            end = Offset(animatedOffset + 600f, 0f),
            tileMode = TileMode.Repeated
        )
    }
}

/**
 * Modifier extension that applies the animated horizontal Rainbow gradient tint during draw phase.
 * Operates on the GPU draw layer without triggering Composable recompositions on every frame.
 */
fun Modifier.rainbowTint(enabled: Boolean = true): Modifier = composed {
    val isRainbow = LocalVisualizerColorScheme.current == VisualizerColorScheme.RAINBOW && enabled
    if (!isRainbow) return@composed this

    val infiniteTransition = rememberInfiniteTransition()
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    this.drawWithCache {
        val brush = Brush.linearGradient(
            colors = RainbowGradientColors,
            start = Offset(animatedOffset, 0f),
            end = Offset(animatedOffset + 600f, 0f),
            tileMode = TileMode.Repeated
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush, blendMode = BlendMode.SrcIn)
        }
    }
}

/**
 * Icon component that renders vector icons with the animated horizontal Rainbow gradient.
 */
@Composable
fun RainbowIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val isRainbow = LocalVisualizerColorScheme.current == VisualizerColorScheme.RAINBOW
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = if (isRainbow) Color.White else tint,
        modifier = modifier.rainbowTint(isRainbow)
    )
}

/**
 * Text component that renders text using the animated horizontal Rainbow gradient in draw phase.
 */
@Composable
fun RainbowText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    val isRainbow = LocalVisualizerColorScheme.current == VisualizerColorScheme.RAINBOW
    Text(
        text = text,
        style = style,
        color = if (isRainbow) Color.White else MaterialTheme.colorScheme.primary,
        modifier = modifier.rainbowTint(isRainbow),
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
    )
}
