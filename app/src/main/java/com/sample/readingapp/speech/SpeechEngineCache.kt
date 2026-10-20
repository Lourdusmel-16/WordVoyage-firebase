package com.sample.readingapp.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object SpeechEngineCache {
    var cachedSherpaManager: SherpaSpeechManager? = null
    var cachedTts: TextToSpeech? = null
    var isTtsReady = false

    fun initTts(context: Context) {
        if (cachedTts == null) {
            cachedTts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    cachedTts?.language = Locale.US
                    cachedTts?.setSpeechRate(0.75f)
                    isTtsReady = true
                }
            }
        }
    }

    fun prewarmSherpa(context: Context) {
        if (cachedSherpaManager == null) {
            cachedSherpaManager = SherpaSpeechManager(context.applicationContext) {}
        }
    }

    fun getOrCreateSherpa(context: Context, onPartial: (List<String>) -> Unit): SherpaSpeechManager {
        val existing = cachedSherpaManager
        return if (existing != null) {
            existing.updateCallback(onPartial)
            existing
        } else {
            val created = SherpaSpeechManager(context.applicationContext, onPartial)
            cachedSherpaManager = created
            created
        }
    }
}
