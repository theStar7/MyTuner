package com.mytuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchTrackGraphMathTest {
    @Test
    fun `pitchTrackXStep returns width divided by sample intervals`() {
        val xStep = pitchTrackXStep(width = 120f, sampleCount = 6)

        assertEquals(24f, xStep, 0.0001f)
    }

    @Test
    fun `pitchTrackXStep coerces non-positive sample count to one`() {
        val zeroCountStep = pitchTrackXStep(width = 120f, sampleCount = 0)
        val negativeCountStep = pitchTrackXStep(width = 120f, sampleCount = -5)

        assertEquals(120f, zeroCountStep, 0.0001f)
        assertEquals(120f, negativeCountStep, 0.0001f)
    }

    @Test
    fun `hzToMidi and midiToHz are mutually consistent`() {
        val midi = hzToMidi(440f)
        requireNotNull(midi)

        assertEquals(69f, midi, 0.0001f)
        assertEquals(440f, midiToHz(midi), 0.0001f)
    }

    @Test
    fun `hzToMidi returns null for non-positive frequency`() {
        assertEquals(null, hzToMidi(0f))
        assertEquals(null, hzToMidi(-1f))
    }

    @Test
    fun `pitchTrackPannedCenterMidi keeps center when latest remains in visible bounds`() {
        val center = pitchTrackPannedCenterMidi(
            currentCenterMidi = 69f,
            latestMidi = 72f,
            visibleSemitoneSpan = 12,
            edgePaddingSemitone = 1.5f
        )

        assertEquals(69f, center, 0.0001f)
    }

    @Test
    fun `pitchTrackPannedCenterMidi moves up when latest exceeds upper bound`() {
        val center = pitchTrackPannedCenterMidi(
            currentCenterMidi = 69f,
            latestMidi = 75f,
            visibleSemitoneSpan = 12,
            edgePaddingSemitone = 1.5f
        )

        assertEquals(70.5f, center, 0.0001f)
    }

    @Test
    fun `pitchTrackPannedCenterMidi moves down when latest exceeds lower bound`() {
        val center = pitchTrackPannedCenterMidi(
            currentCenterMidi = 69f,
            latestMidi = 62f,
            visibleSemitoneSpan = 12,
            edgePaddingSemitone = 1.5f
        )

        assertEquals(66.5f, center, 0.0001f)
    }

    @Test
    fun `pitchTrackSemitoneMarkers returns all semitone lines across window`() {
        val markers = pitchTrackSemitoneMarkers(centerMidi = 69f, visibleSemitoneSpan = 12)

        assertEquals(13, markers.size)
        assertEquals(63, markers.first())
        assertEquals(75, markers.last())
    }

    @Test
    fun `mapFrequenciesToNormalizedY uses fixed semitone span and skips invalid values`() {
        val samples = mapFrequenciesToNormalizedY(
            frequencies = listOf(0f, 440f, 880f, 220f),
            centerMidi = 69f,
            visibleSemitoneSpan = 12
        )

        assertEquals(listOf(1, 2, 3), samples.map { it.index })
        assertEquals(0.5f, samples[0].normalizedY, 0.0001f)
        assertEquals(-0.5f, samples[1].normalizedY, 0.0001f)
        assertEquals(1.5f, samples[2].normalizedY, 0.0001f)
    }

    @Test
    fun `mapFrequenciesToNormalizedY returns empty when no positive values exist`() {
        assertTrue(mapFrequenciesToNormalizedY(emptyList(), centerMidi = 69f, visibleSemitoneSpan = 12).isEmpty())
        assertTrue(mapFrequenciesToNormalizedY(listOf(0f, -2f), centerMidi = 69f, visibleSemitoneSpan = 12).isEmpty())
    }

    @Test
    fun `pitchTrackContinuousSegments splits samples on index gaps`() {
        val segments = pitchTrackContinuousSegments(
            listOf(
                PitchTrackNormalizedSample(index = 0, normalizedY = 0.1f),
                PitchTrackNormalizedSample(index = 1, normalizedY = 0.2f),
                PitchTrackNormalizedSample(index = 3, normalizedY = 0.3f),
                PitchTrackNormalizedSample(index = 4, normalizedY = 0.4f),
                PitchTrackNormalizedSample(index = 7, normalizedY = 0.5f)
            )
        )

        assertEquals(3, segments.size)
        assertEquals(listOf(0, 1), segments[0].map { it.index })
        assertEquals(listOf(3, 4), segments[1].map { it.index })
        assertEquals(listOf(7), segments[2].map { it.index })
    }

    @Test
    fun `pitchTrackContinuousSegments keeps one segment for contiguous samples`() {
        val segments = pitchTrackContinuousSegments(
            listOf(
                PitchTrackNormalizedSample(index = 2, normalizedY = 0.1f),
                PitchTrackNormalizedSample(index = 3, normalizedY = 0.2f),
                PitchTrackNormalizedSample(index = 4, normalizedY = 0.3f)
            )
        )

        assertEquals(1, segments.size)
        assertEquals(listOf(2, 3, 4), segments[0].map { it.index })
    }

    @Test
    fun `pitchTrackHeadFrequency returns last positive frequency`() {
        val headFrequency = pitchTrackHeadFrequency(listOf(440f, 0f, -1f, 466.16f, 0f))

        assertEquals(466.16f, headFrequency ?: 0f, 0.0001f)
    }

    @Test
    fun `pitchTrackHeadFrequency returns null when no valid sample exists`() {
        val headFrequency = pitchTrackHeadFrequency(listOf(0f, -2f, 0f))

        assertEquals(null, headFrequency)
    }

    @Test
    fun `pitchTrackTimeMarkerX stays at three quarter width`() {
        val markerX = pitchTrackTimeMarkerX(waveWidth = 300f)

        assertEquals(225f, markerX, 0.0001f)
    }

    @Test
    fun `pitchTrackWaveAnchorX uses marker anchor while running`() {
        val anchorX = pitchTrackWaveAnchorX(waveWidth = 300f, isRunning = true)

        assertEquals(225f, anchorX, 0.0001f)
    }

    @Test
    fun `pitchTrackWaveAnchorX uses right edge while paused`() {
        val anchorX = pitchTrackWaveAnchorX(waveWidth = 300f, isRunning = false)

        assertEquals(300f, anchorX, 0.0001f)
    }

    @Test
    fun `pitchTrackPauseCompensationPx keeps paused anchor aligned with running anchor`() {
        val runningAnchor = pitchTrackWaveAnchorX(waveWidth = 300f, isRunning = true)
        val pausedAnchor = pitchTrackWaveAnchorX(waveWidth = 300f, isRunning = false)
        val pauseCompensation = pitchTrackPauseCompensationPx(waveWidth = 300f)

        assertEquals(runningAnchor, pausedAnchor + pauseCompensation, 0.0001f)
    }

    @Test
    fun `pitchTrackMarkerFrequencyAtX picks nearest valid sample`() {
        val markerFrequency = pitchTrackMarkerFrequencyAtX(
            frequencies = listOf(440f, 450f, 460f),
            markerX = 22f,
            trailingOffsetPx = 0f,
            stepPx = 10f
        )

        assertEquals(460f, markerFrequency ?: 0f, 0.0001f)
    }

    @Test
    fun `pitchTrackMarkerFrequencyAtX ignores invalid samples`() {
        val markerFrequency = pitchTrackMarkerFrequencyAtX(
            frequencies = listOf(440f, 0f, -1f, 470f),
            markerX = 10f,
            trailingOffsetPx = 0f,
            stepPx = 10f
        )

        assertEquals(440f, markerFrequency ?: 0f, 0.0001f)
    }

    @Test
    fun `pitchTrackPanMinOffset matches marker right slack`() {
        assertEquals(-18, pitchTrackPanMinOffset(visibleSampleCount = 72))
    }

    @Test
    fun `pitchTrackPanMaxOffset includes marker left slack`() {
        assertEquals(59, pitchTrackPanMaxOffset(historySize = 78, visibleSampleCount = 72))
    }

    @Test
    fun `pitchTrackVisibleWindow returns latest samples when offset is zero`() {
        val window = pitchTrackVisibleWindow(
            frequencies = (1..10).map { it.toFloat() },
            visibleSampleCount = 4,
            offsetFromLatest = 0
        )

        assertEquals(listOf(7f, 8f, 9f, 10f), window)
    }

    @Test
    fun `pitchTrackVisibleWindow returns earlier samples when offset increases`() {
        val window = pitchTrackVisibleWindow(
            frequencies = (1..10).map { it.toFloat() },
            visibleSampleCount = 4,
            offsetFromLatest = 3
        )

        assertEquals(listOf(4f, 5f, 6f, 7f), window)
    }

    @Test
    fun `pitchTrackVisibleWindow clamps offset to available history range`() {
        val window = pitchTrackVisibleWindow(
            frequencies = (1..10).map { it.toFloat() },
            visibleSampleCount = 4,
            offsetFromLatest = 50
        )

        assertEquals(listOf(1f, 2f, 3f, 4f), window)
    }

    @Test
    fun `pitchTrackMaxOffset matches available history beyond visible window`() {
        assertEquals(6, pitchTrackMaxOffset(historySize = 10, visibleSampleCount = 4))
        assertEquals(0, pitchTrackMaxOffset(historySize = 4, visibleSampleCount = 4))
        assertEquals(0, pitchTrackMaxOffset(historySize = 2, visibleSampleCount = 4))
    }

    @Test
    fun `pitchTrackNextPanOffset accumulates sub-step drags smoothly`() {
        val first = pitchTrackNextPanOffset(
            currentOffset = 0,
            dragAmountPx = 1.4f,
            stepPx = 2f,
            minOffset = -18,
            maxOffset = 10,
            carryPx = 0f
        )
        val second = pitchTrackNextPanOffset(
            currentOffset = first.offset,
            dragAmountPx = 1.0f,
            stepPx = 2f,
            minOffset = -18,
            maxOffset = 10,
            carryPx = first.carryPx
        )

        assertEquals(0, first.offset)
        assertEquals(1.4f, first.carryPx, 0.0001f)
        assertEquals(1, second.offset)
        assertEquals(0.4f, second.carryPx, 0.0001f)
    }

    @Test
    fun `pitchTrackNextPanOffset clamps at boundaries`() {
        val toOlder = pitchTrackNextPanOffset(
            currentOffset = 9,
            dragAmountPx = 8f,
            stepPx = 2f,
            minOffset = -18,
            maxOffset = 10,
            carryPx = 0f
        )
        val toNewer = pitchTrackNextPanOffset(
            currentOffset = -17,
            dragAmountPx = -8f,
            stepPx = 2f,
            minOffset = -18,
            maxOffset = 10,
            carryPx = 0f
        )

        assertEquals(10, toOlder.offset)
        assertEquals(-18, toNewer.offset)
    }

    @Test
    fun `pitchTrackNextVerticalPanOffset converts drag pixels to semitone offset`() {
        val nextOffset = pitchTrackNextVerticalPanOffset(
            currentOffsetSemitone = 0f,
            dragAmountPx = 15f,
            graphHeightPx = 80f,
            visibleSemitoneSpan = 8,
            maxAbsOffsetSemitone = 24f
        )

        assertEquals(1.5f, nextOffset, 0.0001f)
    }

    @Test
    fun `pitchTrackNextVerticalPanOffset clamps at configured bounds`() {
        val upperBound = pitchTrackNextVerticalPanOffset(
            currentOffsetSemitone = 3f,
            dragAmountPx = 100f,
            graphHeightPx = 80f,
            visibleSemitoneSpan = 8,
            maxAbsOffsetSemitone = 4f
        )
        val lowerBound = pitchTrackNextVerticalPanOffset(
            currentOffsetSemitone = -3f,
            dragAmountPx = -100f,
            graphHeightPx = 80f,
            visibleSemitoneSpan = 8,
            maxAbsOffsetSemitone = 4f
        )

        assertEquals(4f, upperBound, 0.0001f)
        assertEquals(-4f, lowerBound, 0.0001f)
    }

    @Test
    fun `pitchTrackTargetCenterMidi keeps center unchanged while paused`() {
        val center = pitchTrackTargetCenterMidi(
            currentCenterMidi = 70f,
            latestMidi = 76f,
            isRunning = false,
            visibleSemitoneSpan = 8,
            edgePaddingSemitone = 1.5f
        )

        assertEquals(70f, center, 0.0001f)
    }

    @Test
    fun `pitchTrackTargetCenterMidi follows pitch while running`() {
        val center = pitchTrackTargetCenterMidi(
            currentCenterMidi = 69f,
            latestMidi = 75f,
            isRunning = true,
            visibleSemitoneSpan = 8,
            edgePaddingSemitone = 1.5f
        )

        assertEquals(72.5f, center, 0.0001f)
    }
}
