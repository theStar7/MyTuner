package com.mytuner

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

data class PitchTrackNormalizedSample(
    val index: Int,
    val normalizedY: Float
)

data class PitchTrackPanState(
    val offset: Int,
    val carryPx: Float
)

private const val A4_MIDI = 69f
private const val A4_HZ = 440f
private const val PitchTrackMinPositiveHz = 0.0001f
private const val PitchTrackTimeMarkerRatio = 0.75f

fun pitchTrackXStep(width: Float, sampleCount: Int): Float {
    val safeSampleCount = sampleCount.coerceAtLeast(1)
    if (safeSampleCount == 1) return width
    return width / (safeSampleCount - 1).toFloat()
}

fun pitchTrackMaxOffset(historySize: Int, visibleSampleCount: Int): Int {
    val safeHistorySize = historySize.coerceAtLeast(0)
    val safeVisibleSampleCount = visibleSampleCount.coerceAtLeast(1)
    return (safeHistorySize - safeVisibleSampleCount).coerceAtLeast(0)
}

fun pitchTrackPanMinOffset(visibleSampleCount: Int): Int {
    val intervals = (visibleSampleCount.coerceAtLeast(1) - 1).coerceAtLeast(0)
    val rightSlack = ((1f - PitchTrackTimeMarkerRatio) * intervals).roundToInt()
    return -rightSlack
}

fun pitchTrackPanMaxOffset(historySize: Int, visibleSampleCount: Int): Int {
    val maxOffset = pitchTrackMaxOffset(historySize, visibleSampleCount)
    val intervals = (visibleSampleCount.coerceAtLeast(1) - 1).coerceAtLeast(0)
    val leftSlack = (PitchTrackTimeMarkerRatio * intervals).roundToInt()
    return maxOffset + leftSlack
}

fun pitchTrackVisibleWindow(
    frequencies: List<Float>,
    visibleSampleCount: Int,
    offsetFromLatest: Int
): List<Float> {
    if (frequencies.isEmpty()) return emptyList()

    val safeVisibleSampleCount = visibleSampleCount.coerceAtLeast(1)
    val maxOffset = pitchTrackMaxOffset(frequencies.size, safeVisibleSampleCount)
    val clampedOffset = offsetFromLatest.coerceIn(0, maxOffset)
    val endExclusive = frequencies.size - clampedOffset
    val startInclusive = (endExclusive - safeVisibleSampleCount).coerceAtLeast(0)
    return frequencies.subList(startInclusive, endExclusive)
}

fun pitchTrackNextPanOffset(
    currentOffset: Int,
    dragAmountPx: Float,
    stepPx: Float,
    minOffset: Int,
    maxOffset: Int,
    carryPx: Float
): PitchTrackPanState {
    val safeMinOffset = minOffset.coerceAtMost(maxOffset)
    val safeMaxOffset = maxOffset.coerceAtLeast(minOffset)
    val safeOffset = currentOffset.coerceIn(safeMinOffset, safeMaxOffset)
    val safeStepPx = stepPx.coerceAtLeast(0.0001f)

    val logicalDragPx = dragAmountPx
    val combinedPx = carryPx + logicalDragPx
    val sampleDelta = (combinedPx / safeStepPx).toInt()
    val nextCarryPx = combinedPx - (sampleDelta * safeStepPx)

    if (sampleDelta == 0) {
        return PitchTrackPanState(offset = safeOffset, carryPx = nextCarryPx)
    }

    val unclampedOffset = safeOffset + sampleDelta
    val clampedOffset = unclampedOffset.coerceIn(safeMinOffset, safeMaxOffset)
    val boundaryCarryPx = if (clampedOffset != unclampedOffset) 0f else nextCarryPx
    return PitchTrackPanState(offset = clampedOffset, carryPx = boundaryCarryPx)
}

fun pitchTrackNextVerticalPanOffset(
    currentOffsetSemitone: Float,
    dragAmountPx: Float,
    graphHeightPx: Float,
    visibleSemitoneSpan: Int,
    maxAbsOffsetSemitone: Float
): Float {
    val safeGraphHeightPx = graphHeightPx.coerceAtLeast(0.0001f)
    val safeVisibleSpan = visibleSemitoneSpan.coerceAtLeast(1).toFloat()
    val semitonePerPixel = safeVisibleSpan / safeGraphHeightPx
    val deltaSemitone = dragAmountPx * semitonePerPixel
    val safeMaxAbsOffsetSemitone = maxAbsOffsetSemitone.coerceAtLeast(0f)
    return (currentOffsetSemitone + deltaSemitone)
        .coerceIn(-safeMaxAbsOffsetSemitone, safeMaxAbsOffsetSemitone)
}

fun pitchTrackTargetCenterMidi(
    currentCenterMidi: Float,
    latestMidi: Float?,
    isRunning: Boolean,
    visibleSemitoneSpan: Int,
    edgePaddingSemitone: Float
): Float {
    if (!isRunning || latestMidi == null) return currentCenterMidi
    return pitchTrackPannedCenterMidi(
        currentCenterMidi = currentCenterMidi,
        latestMidi = latestMidi,
        visibleSemitoneSpan = visibleSemitoneSpan,
        edgePaddingSemitone = edgePaddingSemitone
    )
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

fun pitchTrackHeadFrequency(frequencies: List<Float>): Float? {
    return frequencies.lastOrNull { it > PitchTrackMinPositiveHz }
}

fun pitchTrackTimeMarkerX(waveWidth: Float): Float {
    val safeWaveWidth = waveWidth.coerceAtLeast(0f)
    return safeWaveWidth * PitchTrackTimeMarkerRatio
}

fun pitchTrackWaveAnchorX(waveWidth: Float, isRunning: Boolean): Float {
    val safeWaveWidth = waveWidth.coerceAtLeast(0f)
    return if (isRunning) pitchTrackTimeMarkerX(safeWaveWidth) else safeWaveWidth
}

fun pitchTrackPauseCompensationPx(waveWidth: Float): Float {
    val safeWaveWidth = waveWidth.coerceAtLeast(0f)
    return pitchTrackWaveAnchorX(safeWaveWidth, isRunning = true) -
        pitchTrackWaveAnchorX(safeWaveWidth, isRunning = false)
}

fun pitchTrackMarkerFrequencyAtX(
    frequencies: List<Float>,
    markerX: Float,
    trailingOffsetPx: Float,
    stepPx: Float
): Float? {
    if (frequencies.isEmpty()) return null

    val safeStepPx = stepPx.coerceAtLeast(0.0001f)
    val validIndices = frequencies.indices.filter { index -> frequencies[index] > PitchTrackMinPositiveHz }
    if (validIndices.isEmpty()) return null

    val markerLocalIndex = (markerX - trailingOffsetPx) / safeStepPx
    val nearestIndex = validIndices.minByOrNull { index -> abs(index - markerLocalIndex) } ?: return null
    return frequencies[nearestIndex]
}

fun pitchTrackContinuousSegments(
    samples: List<PitchTrackNormalizedSample>
): List<List<PitchTrackNormalizedSample>> {
    if (samples.isEmpty()) return emptyList()

    val segments = mutableListOf<MutableList<PitchTrackNormalizedSample>>()
    var currentSegment = mutableListOf(samples.first())

    for (index in 1 until samples.size) {
        val previous = samples[index - 1]
        val current = samples[index]
        if (current.index == previous.index + 1) {
            currentSegment.add(current)
        } else {
            segments.add(currentSegment)
            currentSegment = mutableListOf(current)
        }
    }

    segments.add(currentSegment)
    return segments
}
