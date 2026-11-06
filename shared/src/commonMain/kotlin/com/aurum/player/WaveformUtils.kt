package com.aurum.player

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates high-frequency wiggly audio waveform samples for a song seed.
 */
fun generateWigglyWaveformSamples(seed: String, sampleCount: Int = 180, animPhase: Float = 0f): List<Float> {
    val hash = seed.hashCode()
    val random = Random(hash)

    val freq = 20.0 + (random.nextDouble() * 20.0)
    val basePhase = random.nextDouble() * 2 * PI

    return List(sampleCount) { i ->
        val t = i.toDouble() / (sampleCount - 1).coerceAtLeast(1)
        val edgeFade = sin(t * PI).coerceIn(0.3, 1.0)
        
        val wave = 0.5 + 0.35 * sin(freq * t * 2 * PI + basePhase + animPhase) + 0.15 * sin(freq * 1.5 * t * 2 * PI - animPhase * 0.5)
        val noise = 0.7 + (random.nextDouble() * 0.3)

        (wave * noise * edgeFade).toFloat().coerceIn(0.12f, 1.0f)
    }
}
