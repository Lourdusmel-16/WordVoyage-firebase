package com.sample.readingapp.speech

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechManager(
    private val context: Context,
    private val onPartialCandidates: (List<String>) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var lastAudioReceivedTime = System.currentTimeMillis()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val watchdogRunnable = object : Runnable {
        override fun run() {
            if (isListening && System.currentTimeMillis() - lastAudioReceivedTime > 3500L) {
                restartListeningSession()
            }
            if (isListening) {
                mainHandler.postDelayed(this, 1500L)
            }
        }
    }

    private fun setMuteSystemSounds(mute: Boolean) {
        try {
            val direction = if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, direction, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, direction, 0)
        } catch (_: Exception) {}
    }

    fun startListening() {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) return@post

            isListening = true
            lastAudioReceivedTime = System.currentTimeMillis()
            setMuteSystemSounds(true)

            initializeRecognizer()
            startListeningIntentOnly()

            mainHandler.removeCallbacks(watchdogRunnable)
            mainHandler.postDelayed(watchdogRunnable, 1500L)
        }
    }

    private fun initializeRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null

        speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                lastAudioReceivedTime = System.currentTimeMillis()
            }
            override fun onBeginningOfSpeech() {
                lastAudioReceivedTime = System.currentTimeMillis()
            }
            override fun onRmsChanged(rmsdB: Float) {
                if (rmsdB > 1.2f) {
                    lastAudioReceivedTime = System.currentTimeMillis()
                }
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                if (!isListening) return
                mainHandler.postDelayed({
                    if (isListening) restartListeningSession()
                }, 50L)
            }

            override fun onResults(results: Bundle?) {
                handleSpeechBundle(results)
                if (isListening) {
                    mainHandler.postDelayed({
                        if (isListening) startListeningIntentOnly()
                    }, 30L)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                lastAudioReceivedTime = System.currentTimeMillis()
                handleSpeechBundle(partialResults)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun restartListeningSession() {
        if (!isListening) return
        try {
            speechRecognizer?.cancel()
            startListeningIntentOnly()
        } catch (_: Exception) {
            initializeRecognizer()
            startListeningIntentOnly()
        }
    }

    private fun startListeningIntentOnly() {
        if (!isListening) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
            lastAudioReceivedTime = System.currentTimeMillis()
        } catch (_: Exception) {
            initializeRecognizer()
        }
    }

    private fun handleSpeechBundle(bundle: Bundle?) {
        val matches = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?: bundle?.getStringArrayList("android.speech.extra.UNSTABLE_TEXT")
        if (!matches.isNullOrEmpty()) {
            onPartialCandidates(matches)
        }
    }

    fun stopListening() {
        isListening = false
        mainHandler.removeCallbacks(watchdogRunnable)
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
            setMuteSystemSounds(false)
        }
    }
}
