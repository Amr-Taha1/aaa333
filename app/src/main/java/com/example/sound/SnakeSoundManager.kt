package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SnakeSoundManager(private val context: Context) {
    private var soundPool: SoundPool? = null
    private var eatSoundId = -1
    private var gameOverSoundId = -1
    private var isMuted = false

    companion object {
        private const val TAG = "SnakeSoundManager"
    }

    init {
        try {
            // Configure SoundPool using Material 3 / Modern Sonification guidelines
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build()

            // Programmatically generate beautiful retro synth sounds
            val eatFile = File(context.cacheDir, "retro_eat.wav")
            generateEatSoundWav(eatFile)
            eatSoundId = soundPool?.load(eatFile.absolutePath, 1) ?: -1

            val gameOverFile = File(context.cacheDir, "retro_gameover.wav")
            generateGameOverSoundWav(gameOverFile)
            gameOverSoundId = soundPool?.load(gameOverFile.absolutePath, 1) ?: -1

            soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                Log.d(TAG, "Loaded sound ID: $sampleId with status $status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SnakeSoundManager", e)
        }
    }

    fun playEatSound() {
        if (isMuted) return
        try {
            if (eatSoundId != -1) {
                soundPool?.play(eatSoundId, 0.82f, 0.82f, 1, 0, 1.0f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing eat sound", e)
        }
    }

    fun playGameOverSound() {
        if (isMuted) return
        try {
            if (gameOverSoundId != -1) {
                soundPool?.play(gameOverSoundId, 0.90f, 0.90f, 1, 0, 1.0f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing gameover sound", e)
        }
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun isCurrentlyMuted(): Boolean = isMuted

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing sound pool", e)
        }
    }

    /**
     * Synthesizes a fast retro sweep upward in frequency for the eat action.
     */
    private fun generateEatSoundWav(file: File) {
        val sampleRate = 22050
        val durationMs = 120
        val numSamples = (sampleRate * durationMs / 1000)
        val dataSize = numSamples * 2
        val totalSize = 36 + dataSize

        val buffer = ByteBuffer.allocate(44 + dataSize).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put("RIFF".toByteArray())
            putInt(totalSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // subchunk1Size
            putShort(1.toShort()) // PCM audio format
            putShort(1.toShort()) // Mono
            putInt(sampleRate)
            putInt(sampleRate * 2) // byteRate
            putShort(2.toShort()) // blockAlign (channels * bits/sample / 8)
            putShort(16.toShort()) // bits per sample
            put("data".toByteArray())
            putInt(dataSize)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Sweeps upward from 400Hz to 1100Hz
                val progress = i.toDouble() / numSamples
                val currentFreq = 400.0 + progress * 700.0
                val angle = 2.0 * Math.PI * currentFreq * t
                
                // Add retro chime square/triangle hybrid vibe
                val sine = Math.sin(angle)
                val isSquare = if (sine >= 0) 0.5 else -0.5
                val value = ((sine * 0.4 + isSquare * 0.15) * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                
                // Fade out smoothly
                val decay = 1.0 - progress
                putShort((value * decay).toInt().toShort())
            }
        }

        FileOutputStream(file).use { out ->
            out.write(buffer.array())
        }
    }

    /**
     * Synthesizes a retro multi-tone dynamic power down falling sweep sound for Game Over.
     */
    private fun generateGameOverSoundWav(file: File) {
        val sampleRate = 22050
        val durationMs = 600
        val numSamples = (sampleRate * durationMs / 1000)
        val dataSize = numSamples * 2
        val totalSize = 36 + dataSize

        val buffer = ByteBuffer.allocate(44 + dataSize).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put("RIFF".toByteArray())
            putInt(totalSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // subchunk1Size
            putShort(1.toShort()) // PCM
            putShort(1.toShort()) // Mono
            putInt(sampleRate)
            putInt(sampleRate * 2) // byteRate
            putShort(2.toShort()) // blockAlign
            putShort(16.toShort()) // bits per sample
            put("data".toByteArray())
            putInt(dataSize)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                
                // Sweeps downward and creates a binary stutter step representing arcade retro failure
                val currentBaseFreq = (700.0 - progress * 560.0).coerceAtLeast(100.0)
                
                // Stutter/vibrato: modulate frequency with a low-frequency oscillator (LFO)
                val lfo = Math.sin(2.0 * Math.PI * 18.0 * t) // 18Hz vibrato
                val activeFreq = currentBaseFreq + (lfo * 50.0 * (1.0 - progress))
                
                val angle = 2.0 * Math.PI * activeFreq * t
                val sine = Math.sin(angle)
                // Use a standard retro square wave for a crunchier, vintage arcade feeling
                val square = if (sine >= 0) 0.4 else -0.4
                val value = (square * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                
                // Volume envelope decays linearly to zero
                val decay = 1.0 - progress
                putShort((value * decay).toInt().toShort())
            }
        }

        FileOutputStream(file).use { out ->
            out.write(buffer.array())
        }
    }
}
