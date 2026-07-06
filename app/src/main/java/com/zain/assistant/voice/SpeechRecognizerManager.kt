package com.zain.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ListeningState { IDLE, LISTENING_FOR_WAKE_WORD, LISTENING_FOR_COMMAND, PROCESSING, SPEAKING, ERROR }

/**
 * Manages Android's on-device SpeechRecognizer in a continuously-restarting loop to
 * approximate always-on wake-word listening ("Hey Zain"). A single SpeechRecognizer
 * instance is reused across cycles (rather than destroyed and recreated every time) and
 * restarts are given a short delay — some OEM speech services (notably Samsung/One UI)
 * silently fail if a new recognition request starts immediately after the previous ends.
 */
class SpeechRecognizerManager(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var wakeWord: String = "hey zain"
    private var isCommandMode = false
    private var shouldContinue = false
    private var language: String = "en-US"

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingRestart: Runnable? = null

    private val _state = MutableStateFlow(ListeningState.IDLE)
    val state: StateFlow<ListeningState> = _state.asStateFlow()

    private val _recognizedCommand = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val recognizedCommand: SharedFlow<String> = _recognizedCommand.asSharedFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    fun updateWakeWord(word: String) {
        wakeWord = word.lowercase().trim()
    }

    fun updateLanguage(languageTag: String) {
        language = languageTag
    }

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = ListeningState.ERROR
            return
        }
        shouldContinue = true
        isCommandMode = false
        ensureRecognizerCreated()
        beginListening()
    }

    fun stop() {
        shouldContinue = false
        cancelPendingRestart()
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        _state.value = ListeningState.IDLE
    }

    fun listenForCommandNow() {
        shouldContinue = true
        isCommandMode = true
        ensureRecognizerCreated()
        cancelPendingRestart()
        recognizer?.cancel()
        scheduleRestart(150)
    }

    private fun ensureRecognizerCreated() {
        if (recognizer != null) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = if (isCommandMode) ListeningState.LISTENING_FOR_COMMAND else ListeningState.LISTENING_FOR_WAKE_WORD
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _state.value = ListeningState.PROCESSING
                }

                override fun onError(error: Int) {
                    Log.w(TAG, "SpeechRecognizer error code: $error")
                    if (shouldContinue) scheduleRestart(400) else _state.value = ListeningState.IDLE
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim().orEmpty()
                    handleRecognized(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    _partialTranscript.value = matches?.firstOrNull().orEmpty()
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun beginListening() {
        if (!shouldContinue) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recognizer", e)
            if (shouldContinue) scheduleRestart(500)
        }
    }

    private fun scheduleRestart(delayMillis: Long) {
        cancelPendingRestart()
        val runnable = Runnable {
            if (shouldContinue) beginListening()
        }
        pendingRestart = runnable
        mainHandler.postDelayed(runnable, delayMillis)
    }

    private fun cancelPendingRestart() {
        pendingRestart?.let { mainHandler.removeCallbacks(it) }
        pendingRestart = null
    }

    private fun handleRecognized(text: String) {
        if (text.isBlank()) {
            if (shouldContinue) scheduleRestart(200)
            return
        }
        val lower = text.lowercase()

        if (isCommandMode) {
            isCommandMode = false
            _recognizedCommand.tryEmit(text)
            if (shouldContinue) scheduleRestart(200)
            return
        }

        if (lower.contains(wakeWord)) {
            val remainder = lower.substringAfter(wakeWord).trim()
            if (remainder.isNotBlank()) {
                _recognizedCommand.tryEmit(remainder)
                if (shouldContinue) scheduleRestart(200)
                return
            } else {
                isCommandMode = true
                if (shouldContinue) scheduleRestart(150)
                return
            }
        }

        if (shouldContinue) scheduleRestart(200)
    }

    companion object {
        private const val TAG = "SpeechRecognizerManager"

        @Volatile
        private var instance: SpeechRecognizerManager? = null

        fun getInstance(context: Context): SpeechRecognizerManager {
            return instance ?: synchronized(this) {
                instance ?: SpeechRecognizerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
