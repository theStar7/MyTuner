package com.mytuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchTrackGraphMathTest {
    @Test
    fun `pitchTrackXStep returns width divided by sample count`() {
        val xStep = pitchTrackXStep(width = 120f, sampleCount = 6)

        assertEquals(20f, xStep, 0.0001f)
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
}
