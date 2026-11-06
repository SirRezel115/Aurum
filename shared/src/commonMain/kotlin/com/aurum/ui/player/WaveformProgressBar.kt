package com.aurum.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

import com.aurum.ui.theme.AppFps

/**
 * Multi-style & Multi-color Waveform Progress Bar with thinned-out minimalist lines.
 */
@Composable
fun WaveformProgressBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    songId: String? = null,
    albumArtUri: String? = null,
    albumArtColors: List<Int> = emptyList(),
    waveformAmplitudes: List<Float> = emptyList(),
    fftBands: List<Float> = emptyList(),
    bassEnergy: Float = 0f,
    visualizerStyle: VisualizerStyle = VisualizerStyle.RAINBOW_WAVES,
    visualizerColorScheme: VisualizerColorScheme = VisualizerColorScheme.RAINBOW,
    appFps: AppFps = AppFps.FPS_60,
    progressDotStyle: ProgressDotStyle = ProgressDotStyle.ORB,
    height: Dp = 130.dp,
) {
    val lineCount = 12 // Stacked parallel wave lines
    val resolution = 160 // Points per line

    val themePrimary = MaterialTheme.colorScheme.primary
    val themeSecondary = MaterialTheme.colorScheme.secondary
    val themeTertiary = MaterialTheme.colorScheme.tertiary

    val extractedAlbumColors = remember(albumArtColors, songId, albumArtUri) {
        if (albumArtColors.isNotEmpty()) {
            val c = albumArtColors.map { Color(it) }
            c + c.first()
        } else {
            val seed = ((songId ?: "") + (albumArtUri ?: "")).hashCode()
            val hue1 = (seed and 0xFF).toFloat() / 255f * 360f
            val hue2 = (hue1 + 90f) % 360f
            val hue3 = (hue1 + 180f) % 360f
            val hue4 = (hue1 + 270f) % 360f
            listOf(
                Color.hsl(hue1, 0.85f, 0.55f),
                Color.hsl(hue2, 0.85f, 0.55f),
                Color.hsl(hue3, 0.85f, 0.55f),
                Color.hsl(hue4, 0.85f, 0.55f),
                Color.hsl(hue1, 0.85f, 0.55f)
            )
        }
    }

    var continuousTime by remember { mutableFloatStateOf(0f) }
    var smoothedAmplitudes by remember {
        mutableStateOf(List(160) { 0.5f })
    }

    val targetAmplitudes = remember(waveformAmplitudes) {
        waveformAmplitudes.ifEmpty { List(160) { 0.5f } }
    }

    val targetFps = appFps.targetFps
    val frameIntervalNanos = 1_000_000_000L / targetFps
    val fpsRatio = 60f / targetFps.toFloat()
    val attackRate = (0.22f * fpsRatio).coerceIn(0.05f, 0.8f)
    val decayRate = (0.045f * fpsRatio).coerceIn(0.01f, 0.3f)

    LaunchedEffect(appFps) {
        var lastNanos = 0L
        var accumulatedDeltaNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val frameDeltaNanos = now - lastNanos
                    accumulatedDeltaNanos += frameDeltaNanos

                    if (accumulatedDeltaNanos >= frameIntervalNanos) {
                        val dt = accumulatedDeltaNanos / 1_000_000_000f
                        continuousTime += dt
                        accumulatedDeltaNanos %= frameIntervalNanos

                        if (targetAmplitudes.isNotEmpty()) {
                            val currentList = smoothedAmplitudes
                            if (currentList.size != targetAmplitudes.size) {
                                smoothedAmplitudes = targetAmplitudes.toList()
                            } else {
                                val newList = FloatArray(targetAmplitudes.size)
                                for (i in targetAmplitudes.indices) {
                                    val tgt = targetAmplitudes[i]
                                    val cur = currentList.getOrElse(i) { tgt }
                                    val rate = if (tgt > cur) attackRate else decayRate
                                    newList[i] = cur + (tgt - cur) * rate
                                }
                                smoothedAmplitudes = newList.toList()
                            }
                        }
                    }
                }
                lastNanos = now
            }
        }
    }

    val progressFraction = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }
    val currentFraction = if (isDragging) dragProgressFraction else progressFraction

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .graphicsLayer { clip = false }
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (size.width > 0 && durationMs > 0) {
                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeek((fraction * durationMs).toLong())
                        }
                    }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            if (size.width > 0) {
                                dragProgressFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            }
                        },
                        onDragEnd = {
                            if (durationMs > 0) {
                                onSeek((dragProgressFraction * durationMs).toLong())
                            }
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            if (size.width > 0) {
                                dragProgressFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            }
                        }
                    )
                }
        ) {
            val width = size.width
            val centerY = size.height / 2f
            val maxAmp = size.height * 0.38f

            if (width <= 0) return@Canvas

            val progressRatio = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            val colorShift = (continuousTime * 120f) % width

            val activeBrush = when (visualizerColorScheme) {
                VisualizerColorScheme.RAINBOW -> {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00E5FF),
                            Color(0xFF00E676),
                            Color(0xFFFFEA00),
                            Color(0xFFFF9100),
                            Color(0xFFFF1744),
                            Color(0xFF9C27B0),
                            Color(0xFF00E5FF)
                        ),
                        start = Offset(colorShift, 0f),
                        end = Offset(colorShift + width, 0f),
                        tileMode = TileMode.Repeated
                    )
                }
                VisualizerColorScheme.BLACK -> SolidColor(Color.Black)
                VisualizerColorScheme.WHITE -> SolidColor(Color.White)
                VisualizerColorScheme.APP_THEME -> {
                    Brush.linearGradient(
                        colors = listOf(themePrimary, themeSecondary, themeTertiary, themePrimary),
                        start = Offset(0f, 0f),
                        end = Offset(width, 0f)
                    )
                }
                VisualizerColorScheme.ALBUM_ARTWORK -> {
                    Brush.linearGradient(
                        colors = extractedAlbumColors,
                        start = Offset(colorShift, 0f),
                        end = Offset(colorShift + width, 0f),
                        tileMode = TileMode.Repeated
                    )
                }
            }

            when (visualizerStyle) {
                VisualizerStyle.NONE -> {
                    val barThickness = 1.5.dp.toPx()
                    drawLine(
                        brush = activeBrush,
                        start = Offset(0f, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = barThickness,
                        cap = StrokeCap.Round,
                        alpha = 1.0f
                    )
                }

                VisualizerStyle.RAINBOW_WAVES -> {
                    val dx = width / (resolution - 1).toFloat()
                    for (lineIndex in 0 until lineCount) {
                        val layerFactor = 1f - (abs(lineIndex - lineCount / 2f) / (lineCount / 2f)) * 0.55f
                        val bandType = when { lineIndex < 4 -> 0; lineIndex < 8 -> 1; else -> 2 }
                        val waveFreq = when (bandType) { 0 -> 6.0; 1 -> 16.0; else -> 32.0 }
                        val speedMultiplier = when (bandType) { 0 -> 0.8f; 1 -> 1.8f; else -> 3.5f }

                        val topPath = Path().apply {
                            var started = false
                            for (i in 0 until resolution) {
                                val t = i.toFloat() / (resolution - 1)
                                val tFromCenter = abs(t - 0.5f) * 2f
                                val envelope = ((1f - tFromCenter * tFromCenter).coerceIn(0f, 1f) * abs(cos(tFromCenter * PI * 2.5)).toFloat()).coerceAtLeast(0.05f)
                                val baseAmp = if (smoothedAmplitudes.isNotEmpty()) {
                                    val sampleIdx = (t * (smoothedAmplitudes.size - 1)).toInt().coerceIn(0, smoothedAmplitudes.size - 1)
                                    smoothedAmplitudes[sampleIdx]
                                } else 0.6f

                                val distanceToPlayhead = abs(t - progressRatio)
                                val playheadBoost = if (isPlaying && distanceToPlayhead < 0.18f) 1.4f - (distanceToPlayhead / 0.18f) * 0.4f else 1.0f
                                val sineMod = sin(t * waveFreq * PI + continuousTime * speedMultiplier + lineIndex * 0.3f).toFloat()
                                val beatFactor = if (isPlaying) 0.85f + 0.35f * abs(sin(continuousTime * 12.0 + t * 6.0)).toFloat() else 1.0f
                                val amp = baseAmp * beatFactor * envelope * maxAmp * layerFactor * playheadBoost * (0.7f + 0.5f * sineMod)
                                val x = i * dx
                                val y = centerY - amp
                                if (!started) { moveTo(x, y); started = true } else { lineTo(x, y) }
                            }
                        }

                        val bottomPath = Path().apply {
                            var started = false
                            for (i in 0 until resolution) {
                                val t = i.toFloat() / (resolution - 1)
                                val tFromCenter = abs(t - 0.5f) * 2f
                                val envelope = ((1f - tFromCenter * tFromCenter).coerceIn(0f, 1f) * abs(cos(tFromCenter * PI * 2.5)).toFloat()).coerceAtLeast(0.05f)
                                val baseAmp = if (smoothedAmplitudes.isNotEmpty()) {
                                    val sampleIdx = (t * (smoothedAmplitudes.size - 1)).toInt().coerceIn(0, smoothedAmplitudes.size - 1)
                                    smoothedAmplitudes[sampleIdx]
                                } else 0.6f

                                val distanceToPlayhead = abs(t - progressRatio)
                                val playheadBoost = if (isPlaying && distanceToPlayhead < 0.18f) 1.4f - (distanceToPlayhead / 0.18f) * 0.4f else 1.0f
                                val sineMod = sin(t * waveFreq * PI + continuousTime * speedMultiplier + lineIndex * 0.3f).toFloat()
                                val beatFactor = if (isPlaying) 0.85f + 0.35f * abs(sin(continuousTime * 12.0 + t * 6.0)).toFloat() else 1.0f
                                val amp = baseAmp * beatFactor * envelope * maxAmp * layerFactor * playheadBoost * (0.7f + 0.5f * sineMod)
                                val x = i * dx
                                val y = centerY + amp
                                if (!started) { moveTo(x, y); started = true } else { lineTo(x, y) }
                            }
                        }

                        drawPath(topPath, brush = activeBrush, style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        drawPath(bottomPath, brush = activeBrush, style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }

                VisualizerStyle.CLASSIC_BARS -> {
                    val barCount = 60
                    val gap = 1.5.dp.toPx()
                    val barWidth = (width - (barCount - 1) * gap) / barCount

                    for (i in 0 until barCount) {
                        val t = i.toFloat() / (barCount - 1).coerceAtLeast(1)
                        val baseAmp = if (smoothedAmplitudes.isNotEmpty()) {
                            val sampleIdx = (t * (smoothedAmplitudes.size - 1)).toInt().coerceIn(0, smoothedAmplitudes.size - 1)
                            smoothedAmplitudes[sampleIdx]
                        } else 0.5f

                        val pulse = if (isPlaying) 1f + 0.2f * sin(continuousTime * 5f + i * 0.2f) else 1f
                        val beatFactor = if (isPlaying) 0.85f + 0.35f * abs(sin(continuousTime * 12.0 + t * 6.0)).toFloat() else 1.0f
                        val barHeight = (baseAmp * beatFactor * maxAmp * 1.8f * pulse).coerceIn(3.dp.toPx(), maxAmp * 1.8f)
                        val x = i * (barWidth + gap)

                        drawRect(
                            brush = activeBrush,
                            topLeft = Offset(x, centerY - barHeight / 2f),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }

                VisualizerStyle.MINIMAL_LINE -> {
                    val dx = width / (resolution - 1).toFloat()
                    val linePath = Path().apply {
                        var started = false
                        for (i in 0 until resolution) {
                            val t = i.toFloat() / (resolution - 1)
                            val baseAmp = if (smoothedAmplitudes.isNotEmpty()) {
                                val sampleIdx = (t * (smoothedAmplitudes.size - 1)).toInt().coerceIn(0, smoothedAmplitudes.size - 1)
                                smoothedAmplitudes[sampleIdx]
                            } else 0.5f

                            val pulse = if (isPlaying) sin(t * 12.0 * PI + continuousTime * 3f).toFloat() else 0f
                            val beatFactor = if (isPlaying) 0.85f + 0.35f * abs(sin(continuousTime * 12.0 + t * 6.0)).toFloat() else 1.0f
                            val y = centerY - (baseAmp * beatFactor * maxAmp * 0.8f + pulse * 6.dp.toPx())
                            val x = i * dx
                            if (!started) { moveTo(x, y); started = true } else { lineTo(x, y) }
                        }
                    }

                    drawPath(
                        path = linePath,
                        brush = activeBrush,
                        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                VisualizerStyle.SILVER_WAVE_LINES -> {
                    val dx = width / (resolution - 1).toFloat()
                    val waveCount = 10
                    for (w in 0 until waveCount) {
                        val phaseOffset = w * 0.4f
                        val freqMultiplier = 2.0f + w * 0.8f
                        val path = Path().apply {
                            var started = false
                            for (i in 0 until resolution) {
                                val t = i.toFloat() / (resolution - 1)
                                val baseAmp = if (smoothedAmplitudes.isNotEmpty()) {
                                    val sampleIdx = (t * (smoothedAmplitudes.size - 1)).toInt().coerceIn(0, smoothedAmplitudes.size - 1)
                                    smoothedAmplitudes[sampleIdx]
                                } else 0.5f

                                val waveMod = sin(t * freqMultiplier * PI + continuousTime * 1.5f + phaseOffset).toFloat()
                                val beatFactor = if (isPlaying) 0.85f + 0.35f * abs(sin(continuousTime * 12.0 + t * 6.0)).toFloat() else 1.0f
                                val y = centerY + waveMod * baseAmp * beatFactor * maxAmp * 0.7f + (w - waveCount / 2f) * 4.dp.toPx()
                                val x = i * dx
                                if (!started) { moveTo(x, y); started = true } else { lineTo(x, y) }
                            }
                        }

                        drawPath(
                            path = path,
                            brush = activeBrush,
                            style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                VisualizerStyle.SILVER_PARTICLE_WAVES -> {
                    val colCount = 55
                    val colWidth = width / colCount

                    for (c in 0 until colCount) {
                        val t = c.toFloat() / (colCount - 1).coerceAtLeast(1)
                        val baseAmp = if (smoothedAmplitudes.isNotEmpty()) {
                            val sampleIdx = (t * (smoothedAmplitudes.size - 1)).toInt().coerceIn(0, smoothedAmplitudes.size - 1)
                            smoothedAmplitudes[sampleIdx]
                        } else 0.5f

                        val pulse = if (isPlaying) 1f + 0.3f * sin(continuousTime * 6f + c * 0.3f) else 1f
                        val beatFactor = if (isPlaying) 0.85f + 0.35f * abs(sin(continuousTime * 12.0 + t * 6.0)).toFloat() else 1.0f
                        val heightSpan = baseAmp * beatFactor * maxAmp * 1.6f * pulse
                        val x = c * colWidth + colWidth / 2f

                        val dotCount = 12
                        for (d in 0 until dotCount) {
                            val dotProgress = d.toFloat() / (dotCount - 1)
                            val offsetY = (dotProgress - 0.5f) * heightSpan * 2f
                            val y = centerY + offsetY
                            val dotRadius = (1.0f + (1f - abs(dotProgress - 0.5f) * 2f) * 1.0f).dp.toPx()

                            drawCircle(
                                brush = activeBrush,
                                radius = dotRadius,
                                center = Offset(x, y),
                                alpha = 0.4f + 0.6f * (1f - abs(dotProgress - 0.5f))
                            )
                        }
                    }
                }

                VisualizerStyle.HARMONIC_WAVE_LINES -> {
                    val smoothResolution = 300
                    val dx = width / (smoothResolution - 1).toFloat()
                    val waveCount = 14
                    for (w in 0 until waveCount) {
                        val phaseOffset = w * 0.35f
                        val freqMultiplier = 1.6f + w * 0.4f
                        val path = Path().apply {
                            var started = false
                            for (i in 0 until smoothResolution) {
                                val t = i.toFloat() / (smoothResolution - 1)
                                val tFromCenter = abs(t - 0.5f) * 2f
                                val envelope = (1f - tFromCenter * tFromCenter).coerceIn(0f, 1f)

                                val baseAmp = if (smoothedAmplitudes.isNotEmpty()) {
                                    val exactIdx = t * (smoothedAmplitudes.size - 1)
                                    val idx1 = exactIdx.toInt().coerceIn(0, smoothedAmplitudes.size - 1)
                                    val idx2 = (idx1 + 1).coerceIn(0, smoothedAmplitudes.size - 1)
                                    val frac = exactIdx - idx1
                                    smoothedAmplitudes[idx1] * (1f - frac) + smoothedAmplitudes[idx2] * frac
                                } else 0.6f

                                val waveMod = sin(t * freqMultiplier * PI * 2.0 + continuousTime * 1.8 + phaseOffset).toFloat()
                                val beatFactor = if (isPlaying) 0.85f + 0.35f * abs(sin(continuousTime * 12.0 + t * 6.0)).toFloat() else 1.0f
                                val y = centerY + waveMod * baseAmp * beatFactor * maxAmp * envelope * 0.9f + (w - waveCount / 2f) * 3.dp.toPx()
                                val x = i * dx
                                if (!started) { moveTo(x, y); started = true } else { lineTo(x, y) }
                            }
                        }

                        drawPath(
                            path = path,
                            brush = activeBrush,
                            style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                VisualizerStyle.NEON_SPECTRUM -> {
                    val barCount = 48
                    val gap = 2.dp.toPx()
                    val barWidth = (width - (barCount - 1) * gap) / barCount

                    for (i in 0 until barCount) {
                        val t = i.toFloat() / (barCount - 1).coerceAtLeast(1)
                        val baseAmp = if (smoothedAmplitudes.isNotEmpty()) {
                            val sampleIdx = (t * (smoothedAmplitudes.size - 1)).toInt().coerceIn(0, smoothedAmplitudes.size - 1)
                            smoothedAmplitudes[sampleIdx]
                        } else 0.5f

                        val pulse = if (isPlaying) 1f + 0.25f * sin(continuousTime * 6f + i * 0.25f) else 1f
                        val barHeight = (baseAmp * maxAmp * 1.9f * pulse).coerceIn(4.dp.toPx(), maxAmp * 1.9f)
                        val x = i * (barWidth + gap)
                        val topY = centerY - barHeight / 2f

                        drawRoundRect(
                            brush = activeBrush,
                            topLeft = Offset(x, topY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                        )

                        val peakOffset = (5.dp.toPx() * (1f + 0.3f * sin(continuousTime * 4f + i * 0.15f))).coerceAtLeast(3.dp.toPx())
                        val peakY = (topY - peakOffset).coerceAtLeast(2.dp.toPx())
                        drawCircle(
                            brush = activeBrush,
                            radius = (barWidth / 2f).coerceIn(1.5.dp.toPx(), 3.dp.toPx()),
                            center = Offset(x + barWidth / 2f, peakY)
                        )
                    }
                }

                VisualizerStyle.AURORA_LIQUID_BLOB -> {
                    val dx = width / (resolution - 1).toFloat()
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        var started = false
                        for (i in 0 until resolution) {
                            val t = i.toFloat() / (resolution - 1)
                            val baseAmp = if (smoothedAmplitudes.isNotEmpty()) {
                                val sampleIdx = (t * (smoothedAmplitudes.size - 1)).toInt().coerceIn(0, smoothedAmplitudes.size - 1)
                                smoothedAmplitudes[sampleIdx]
                            } else 0.5f

                            val wave1 = sin(t * 8.0 * PI + continuousTime * 2.2).toFloat()
                            val wave2 = cos(t * 14.0 * PI - continuousTime * 1.5).toFloat()
                            val y = centerY - (baseAmp * maxAmp * 0.85f + (wave1 + wave2) * 5.dp.toPx())
                            val x = i * dx
                            if (!started) { lineTo(x, y); started = true } else { lineTo(x, y) }
                        }
                        lineTo(width, size.height)
                        close()
                    }

                    drawPath(
                        path = path,
                        brush = activeBrush,
                        alpha = 0.65f
                    )
                }

                VisualizerStyle.CIRCULAR_RADIAL_BURST -> {
                    val center = Offset(width / 2f, centerY)
                    val liveBass = if (isPlaying) bassEnergy.coerceIn(0.1f, 1.0f) else 0.1f
                    val baseRadius = size.height * (0.16f + 0.14f * liveBass)
                    val rayCount = 40
                    val angleStep = (2f * PI / rayCount).toFloat()

                    drawCircle(
                        brush = activeBrush,
                        radius = baseRadius * (0.75f + 0.25f * liveBass),
                        center = center,
                        alpha = 0.2f + 0.5f * liveBass
                    )

                    drawCircle(
                        brush = activeBrush,
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = (1.5f + liveBass * 2.5f).dp.toPx())
                    )

                    val activeBands = fftBands.ifEmpty { smoothedAmplitudes }

                    for (r in 0 until rayCount) {
                        val angle = r * angleStep + continuousTime * (0.2f + liveBass * 0.6f)
                        val t = r.toFloat() / rayCount
                        val sampleIdx = (t * (activeBands.size - 1)).toInt().coerceIn(0, activeBands.size - 1)
                        val bandAmp = activeBands[sampleIdx]

                        val rayLength = bandAmp * maxAmp * 0.85f * (if (isPlaying) 0.6f + 0.8f * liveBass else 0.3f)
                        val startX = center.x + cos(angle.toDouble()).toFloat() * baseRadius
                        val startY = center.y + sin(angle.toDouble()).toFloat() * baseRadius
                        val endX = center.x + cos(angle.toDouble()).toFloat() * (baseRadius + rayLength)
                        val endY = center.y + sin(angle.toDouble()).toFloat() * (baseRadius + rayLength)

                        drawLine(
                            brush = activeBrush,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = (1.2f + bandAmp * 2f).dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                VisualizerStyle.PARTICLE_ORBIT_FIELD -> {
                    val center = Offset(width / 2f, centerY)
                    val liveBass = if (isPlaying) bassEnergy.coerceIn(0.1f, 1.0f) else 0.1f
                    val activeBands = fftBands.ifEmpty { smoothedAmplitudes }
                    val particleCount = 52

                    for (p in 0 until particleCount) {
                        val t = p.toFloat() / particleCount
                        val sampleIdx = (t * (activeBands.size - 1)).toInt().coerceIn(0, activeBands.size - 1)
                        val bandAmp = activeBands[sampleIdx]

                        val angle = p * 0.32f + continuousTime * (0.4f + liveBass * 1.8f)
                        val orbitRadius = (16.dp.toPx() + p * 1.6.dp.toPx() + (bandAmp + liveBass) * maxAmp * 0.4f).coerceAtLeast(6.dp.toPx())
                        val px = center.x + cos(angle.toDouble()).toFloat() * orbitRadius * (width / (size.height * 2.2f))
                        val py = center.y + sin(angle.toDouble()).toFloat() * orbitRadius * 0.85f
                        val radius = (1.5f + bandAmp * 2.5f + liveBass * 3.5f).dp.toPx()

                        drawCircle(
                            brush = activeBrush,
                            radius = radius * 1.8f,
                            center = Offset(px, py),
                            alpha = (0.15f + 0.4f * liveBass * bandAmp).coerceIn(0.1f, 0.7f)
                        )
                        drawCircle(
                            brush = activeBrush,
                            radius = radius,
                            center = Offset(px, py),
                            alpha = (0.4f + 0.6f * bandAmp).coerceIn(0.2f, 1f)
                        )
                    }
                }

                VisualizerStyle.CONCENTRIC_RIPPLE_RINGS -> {
                    val center = Offset(width / 2f, centerY)
                    val liveBass = if (isPlaying) bassEnergy.coerceIn(0.1f, 1.0f) else 0.1f
                    val ringCount = 6

                    drawCircle(
                        brush = activeBrush,
                        radius = (5.dp.toPx() + liveBass * 16.dp.toPx()),
                        center = center,
                        alpha = 0.3f + 0.6f * liveBass
                    )

                    for (r in 0 until ringCount) {
                        val speed = 0.5f + liveBass * 1.2f
                        val ringPhase = (continuousTime * speed + r * (1.0f / ringCount)) % 1.0f
                        val radius = ringPhase * size.height * 0.75f + 4.dp.toPx()
                        val alpha = (1.0f - ringPhase).coerceIn(0f, 1f)

                        val strokeW = (1.2f + liveBass * 5.0f * (1f - ringPhase)).dp.toPx()

                        drawCircle(
                            brush = activeBrush,
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeW),
                            alpha = alpha * (0.3f + 0.7f * liveBass)
                        )
                    }
                }

                else -> {}
            }

            // Draw playhead indicator thumb according to progressDotStyle
            val playheadX = currentFraction * width
            val orbColor = when (visualizerColorScheme) {
                VisualizerColorScheme.BLACK -> Color.White
                VisualizerColorScheme.WHITE -> Color.Black
                VisualizerColorScheme.APP_THEME -> themePrimary
                VisualizerColorScheme.RAINBOW -> Color.Black
                VisualizerColorScheme.ALBUM_ARTWORK -> extractedAlbumColors.firstOrNull() ?: themePrimary
            }

            when (progressDotStyle) {
                ProgressDotStyle.ORB -> {
                    val orbRadius = 4.5.dp.toPx()
                    drawCircle(
                        color = orbColor.copy(alpha = 0.4f),
                        radius = orbRadius * 1.2f,
                        center = Offset(playheadX, centerY + 1f)
                    )
                    drawCircle(
                        color = orbColor,
                        radius = orbRadius,
                        center = Offset(playheadX, centerY)
                    )
                }
                ProgressDotStyle.GLOWING_PULSE -> {
                    val pulse = if (isPlaying) 1f + 0.25f * abs(sin(continuousTime * 8.0)).toFloat() else 1f
                    val orbRadius = 5.dp.toPx() * pulse
                    drawCircle(
                        color = orbColor.copy(alpha = 0.3f),
                        radius = orbRadius * 1.6f,
                        center = Offset(playheadX, centerY)
                    )
                    drawCircle(
                        color = orbColor,
                        radius = orbRadius,
                        center = Offset(playheadX, centerY)
                    )
                }
                ProgressDotStyle.MINIMAL_BAR -> {
                    val barW = 1.5.dp.toPx()
                    val barH = 16.dp.toPx()
                    drawRoundRect(
                        color = orbColor,
                        topLeft = Offset(playheadX - barW / 2f, centerY - barH / 2f),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                }
                ProgressDotStyle.NONE -> {
                    // No progress dot
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun WaveformProgressBarPreview() {
    com.aurum.ui.theme.AurumTheme {
        WaveformProgressBar(
            positionMs = 45000L,
            durationMs = 210000L,
            onSeek = {},
            isPlaying = true,
            songId = "preview_1",
            modifier = Modifier.fillMaxWidth().height(48.dp)
        )
    }
}
