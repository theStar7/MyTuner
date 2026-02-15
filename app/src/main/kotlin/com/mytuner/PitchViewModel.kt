package com.mytuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.log2
import kotlin.math.roundToInt

data class PitchState(
    val pitch: Float = 0.0f,
    val noteName: String = "-",
    val centsOff: Int = 0,
    val isRunning: Boolean = false
)

class PitchViewModel : ViewModel() {
    private val audioEngine = AudioEngine()
    private val _uiState = MutableStateFlow(PitchState())
    val uiState = _uiState.asStateFlow()

    fun toggleEngine() {
        if (_uiState.value.isRunning) {
            audioEngine.stop()
            _uiState.value = _uiState.value.copy(isRunning = false, pitch = 0f, noteName = "-")
        } else {
            _uiState.value = _uiState.value.copy(isRunning = true)
            viewModelScope.launch {
                audioEngine.start { pitch ->
                    updatePitch(pitch)
                }
            }
        }
    }

    private fun updatePitch(pitch: Float) {
        if (pitch > 0) {
            val (note, cents) = NoteUtils.calculateNoteAndCents(pitch)
            _uiState.value = _uiState.value.copy(
                pitch = pitch,
                noteName = note,
                centsOff = cents
            )
        } else {
            _uiState.value = _uiState.value.copy(
                pitch = 0f
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }
}
