package com.zain.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import java.util.Locale

enum class ListeningState { IDLE, LISTENING_FOR_WAKE_WORD, LISTENING_FOR_COMMAND, PROCESSING, SPEAKING, ERROR }

/**
 * Manages Android's on-device SpeechRecognizer in a continuously-restarting loop to
 * approximate always-on wake-word listening ("Hey Zain"). This is a pragmatic, no-extra-
 * dependency approach: recognized phrases are checked for the wake word; if found, the
 * remainder (or the next recognized utterance) is treated as the command.
 *
 * Note: true low-power always-on wake-word detection (e.g. via Porcupine) runs a small
 * dedicated model instead of the full speech recognizer and uses far less battery. This
 * class documents that trade-off and can be swapped for such an engine later without
 * changing the public API (start/stop/state/results flows).
 */
class SpeechRecognizerManager(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var wakeWord: String = "hey zain"
    private var isCommandMode = false
    private var shouldContinue = false
    private var language: String = "en-US"

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
        listenCycle()
    }

    fun stop() {
        shouldContinue = false
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
        _state.value = ListeningState.IDLE
    }

    /** Call this to force the next recognition cycle to treat speech as a direct command
     *  (used when the user taps the mic button instead of relying on the wake word). */
    fun listenForCommandNow() {
        shouldContinue = true
        isCommandMode = true
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
        listenCycle()
    }

    private fun listenCycle() {
        if (!shouldContinue) return

        recognizer?.destroy()
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
                    // Restart the cycle to keep the "continuous listening" behavior alive.
                    if (shouldContinue) listenCycle() else _state.value = ListeningState.IDLE
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

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recognizer", e)
            if (shouldContinue) listenCycle()
        }
    }

    private fun handleRecognized(text: String) {
        if (text.isBlank()) {
            if (shouldContinue) listenCycle()
            return
        }
        val lower = text.lowercase()

        if (isCommandMode) {
            // Already primed for a command (e.g. user tapped the mic button).
            isCommandMode = false
            _recognizedCommand.tryEmit(text)
            return
        }

        if (lower.contains(wakeWord)) {
            val remainder = lower.substringAfter(wakeWord).trim()
            if (remainder.isNotBlank()) {
                // "Hey Zain, call Ali" said in one breath.
                _recognizedCommand.tryEmit(remainder)
            } else {
                // Wake word alone — listen again immediately for the actual command.
                isCommandMode = true
                if (shouldContinue) listenCycle()
                return
            }
        }

        if (shouldContinue) listenCycle()
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
