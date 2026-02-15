package com.mytuner

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class AudioEngine {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    private var audioRecord: AudioRecord? = null
    
    suspend fun start(onPitchDetected: (Float) -> Unit) = withContext(Dispatchers.IO) {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        val readBuffer = ShortArray(2048)
        audioRecord?.startRecording()

        try {
            while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
                if (read > 0) {
                    val pitch = detectPitchYin(readBuffer, read, sampleRate)
                    onPitchDetected(pitch)
                }
                delay(10) 
            }
        } finally {
            stop()
        }
    }

    fun stop() {
        audioRecord?.apply {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
            release()
        }
        audioRecord = null
    }

    private fun detectPitchYin(buffer: ShortArray, size: Int, sampleRate: Int): Float {
        var sum = 0.0
        for (i in 0 until size) sum += buffer[i] * buffer[i]
        if (sqrt(sum / size) < 400.0) return 0f 

        val tauMax = 500 
        val tauMin = 40  
        val yinBuffer = FloatArray(tauMax)
        
        for (tau in 0 until tauMax) {
            var diff = 0f
            for (i in 0 until size - tauMax) {
                val delta = buffer[i].toFloat() - buffer[i + tau].toFloat()
                diff += delta * delta
            }
            yinBuffer[tau] = diff
        }

        var bestTau = -1
        val threshold = 0.15f
        var minDiff = Float.MAX_VALUE
        
        for (tau in tauMin until tauMax) {
            if (yinBuffer[tau] < minDiff) {
                minDiff = yinBuffer[tau]
                bestTau = tau
            }
            if (yinBuffer[tau] < yinBuffer[0] * threshold) {
                bestTau = tau
                break
            }
        }

        return if (bestTau > 0) sampleRate.toFloat() / bestTau else 0f
    }
}
