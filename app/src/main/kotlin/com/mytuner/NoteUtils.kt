package com.mytuner

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

object NoteUtils {
    val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun calculateNoteAndCents(frequency: Float): Pair<String, Int> {
        if (frequency <= 0) return Pair("-", 0)
        
        val midiNote = (12 * log2(frequency / 440.0) + 69.0)
        val roundedNote = midiNote.roundToInt()
        val cents = ((midiNote - roundedNote) * 100).toInt()
        
        val noteIndex = roundedNote % 12
        val octave = (roundedNote / 12) - 1
        
        val name = if (noteIndex >= 0) {
            "${NOTE_NAMES[noteIndex]}$octave"
        } else {
            "-"
        }
        
        return Pair(name, cents)
    }
}
