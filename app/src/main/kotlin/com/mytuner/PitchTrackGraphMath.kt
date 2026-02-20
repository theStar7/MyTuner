package com.mytuner

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow

data class PitchTrackNormalizedSample(
    val index: Int,
    val normalizedY: Float
)

private const val A4_MIDI = 69f
private const val A4_HZ = 440f
private const val PitchTrackMinPositiveHz = 0.0001f

fun pitchTrackXStep(width: Float, sampleCount: Int): Float {
    val safeSampleCount = sampleCount.coerceAtLeast(1)
    return width / safeSampleCount.toFloat()
}

fun hzToMidi(frequency: Float): Float? {
    if (frequency <= PitchTrackMinPositiveHz) return null
    return (12f * log2(frequency / A4_HZ)) + A4_MIDI
}

fun midiToHz(midi: Float): Float {
    return A4_HZ * 2f.pow((midi - A4_MIDI) / 12f)
}

fun pitchTrackPannedCenterMidi(
    currentCenterMidi: Float,
    latestMidi: Float,
    visibleSemitoneSpan: Int,
    edgePaddingSemitone: Float
): Float {
    val safeSpan = visibleSemitoneSpan.coerceAtLeast(1)
    val safePadding = edgePaddingSemitone.coerceIn(0f, safeSpan / 2f)
    val halfSpan = safeSpan / 2f
    val upperLimit = currentCenterMidi + halfSpan - safePadding
    val lowerLimit = currentCenterMidi - halfSpan + safePadding

    return when {
        latestMidi > upperLimit -> latestMidi - (halfSpan - safePadding)
        latestMidi < lowerLimit -> latestMidi + (halfSpan - safePadding)
        else -> currentCenterMidi
    }
}

fun pitchTrackSemitoneMarkers(centerMidi: Float, visibleSemitoneSpan: Int): List<Int> {
    val safeSpan = visibleSemitoneSpan.coerceAtLeast(1)
    val halfSpan = safeSpan / 2f
    val minMidi = centerMidi - halfSpan
    val maxMidi = centerMidi + halfSpan

    val start = floor(minMidi).toInt()
    val end = ceil(maxMidi).toInt()
    return (start..end).toList()
}

fun mapFrequenciesToNormalizedY(
    frequencies: List<Float>,
    centerMidi: Float,
    visibleSemitoneSpan: Int
): List<PitchTrackNormalizedSample> {
    val safeSpan = visibleSemitoneSpan.coerceAtLeast(1)
    val halfSpan = safeSpan / 2f
    val minMidi = centerMidi - halfSpan

    return frequencies.mapIndexedNotNull { index, frequency ->
        val midi = hzToMidi(frequency) ?: return@mapIndexedNotNull null
        PitchTrackNormalizedSample(
            index = index,
            normalizedY = 1f - ((midi - minMidi) / safeSpan)
        )
    }
}
