package com.sample.readingapp.speech

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.k2fsa.sherpa.onnx.*
import kotlinx.coroutines.*

class SherpaSpeechManager(
    private val context: Context,
    private var onPartialCandidates: (List<String>) -> Unit
) {
    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val initJob: Job

    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    fun updateCallback(newCallback: (List<String>) -> Unit) {
        this.onPartialCandidates = newCallback
    }

    suspend fun awaitInitialization() {
        initJob.join()
    }

    fun isReady(): Boolean = recognizer != null && stream != null

    init {
        initJob = coroutineScope.launch {
            try {
                val config = OnlineRecognizerConfig(
                    featConfig = FeatureConfig(sampleRate = sampleRate, featureDim = 80),
                    modelConfig = OnlineModelConfig(
                        transducer = OnlineTransducerModelConfig(
                            encoder = "sherpa-model/encoder-epoch-99-avg-1.onnx",
                            decoder = "sherpa-model/decoder-epoch-99-avg-1.onnx",
                            joiner = "sherpa-model/joiner-epoch-99-avg-1.onnx"
                        ),
                        tokens = "sherpa-model/tokens.txt",
                        numThreads = 2,
                        debug = false
                    ),
                    endpointConfig = EndpointConfig(),
                    enableEndpoint = true
                )
                recognizer = OnlineRecognizer(assetManager = context.assets, config = config)
                stream = recognizer?.createStream()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isRecording) {
            try {
                if (recognizer != null && stream != null) {
                    recognizer?.reset(stream!!)
                }
            } catch (_: Exception) {}
            return
        }
        isRecording = true

        coroutineScope.launch {
            try {
                initJob.join()

                if (recognizer != null && stream != null) {
                    recognizer?.reset(stream!!)
                }

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize * 2
                )
                audioRecord?.startRecording()

                val buffer = ShortArray(1024)
                while (isRecording) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readCount > 0 && recognizer != null && stream != null) {
                        val samples = FloatArray(readCount) { buffer[it] / 32768.0f }
                        stream?.acceptWaveform(samples, sampleRate)

                        while (recognizer?.isReady(stream!!) == true) {
                            recognizer?.decode(stream!!)
                        }

                        val result = recognizer?.getResult(stream!!)?.text?.trim()
                        if (!result.isNullOrBlank()) {
                            withContext(Dispatchers.Main) {
                                onPartialCandidates(listOf(result))
                            }
                        }

                        if (recognizer?.isEndpoint(stream!!) == true) {
                            recognizer?.reset(stream!!)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopListening() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    fun destroy() {
        stopListening()
        stream?.release()
        recognizer?.release()
        coroutineScope.cancel()
    }
}
