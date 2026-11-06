package com.aurum.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom bespoke Topographic Contour Sound Wave Placeholder Artwork
 * rendered natively on Canvas when no album artwork is available.
 */
@Composable
fun TopographicContourPlaceholder(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0 || h <= 0) return@Canvas

            val strokeWidthPx = 1.2.dp.toPx()
            val ringCount = 15
            val pointsPerRing = 120

            // 4 organic center points corresponding to the 4 topographic contour clusters in the reference art
            val centers = listOf(
                Pair(w * 0.35f, h * 0.38f),
                Pair(w * 0.65f, h * 0.38f),
                Pair(w * 0.35f, h * 0.65f),
                Pair(w * 0.65f, h * 0.65f),
            )

            centers.forEachIndexed { clusterIdx, (cx, cy) ->
                val baseRadius = (w * 0.025f)
                val lobedMod = when (clusterIdx % 4) {
                    0 -> 4 // 4-lobed clover
                    1 -> 3 // 3-lobed organic oval
                    2 -> 5 // 5-lobed fluid star
                    else -> 2 // Organic egg/contour
                }
                val phase = clusterIdx * 0.8f

                for (r in 0 until ringCount) {
                    val ringRadius = baseRadius + r * (w * 0.012f)
                    val path = Path()
                    var started = false

                    for (p in 0 until pointsPerRing) {
                        val angle = (p.toFloat() / pointsPerRing) * 2f * PI.toFloat()
                        // Organic topographic wave modulation
                        val wave1 = sin(angle * lobedMod + phase) * 0.22f
                        val wave2 = cos(angle * (lobedMod + 1) - phase * 0.5f) * 0.12f
                        val modulatedRadius = ringRadius * (1f + wave1 + wave2)

                        val x = cx + modulatedRadius * cos(angle)
                        val y = cy + modulatedRadius * sin(angle)

                        if (!started) {
                            path.moveTo(x, y)
                            started = true
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    path.close()

                    val ringAlpha = (1f - (r.toFloat() / ringCount) * 0.65f).coerceIn(0.15f, 0.9f)
                    drawPath(
                        path = path,
                        color = accentColor.copy(alpha = ringAlpha),
                        style = Stroke(
                            width = strokeWidthPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
