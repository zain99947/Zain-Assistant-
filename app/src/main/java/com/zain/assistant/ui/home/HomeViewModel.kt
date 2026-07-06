package com.zain.assistant.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zain.assistant.ZainApplication
import com.zain.assistant.actions.CommandExecutor
import com.zain.assistant.data.local.ConversationHistoryStore
import com.zain.assistant.data.repository.ChatRepository
import com.zain.assistant.domain.CommandParser
import com.zain.assistant.domain.model.Message
import com.zain.assistant.domain.model.VoiceCommand
import com.zain.assistant.voice.ListeningState
import com.zain.assistant.voice.SpeechRecognizerManager
import com.zain.assistant.voice.TextToSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val listeningState: ListeningState = ListeningState.IDLE,
    val partialTranscript: String = "",
    val messages: List<Message> = emptyList(),
    val statusText: String = "Tap the mic or say \"Hey Zain\""
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ZainApplication
    private val settingsRepository = app.settingsRepository
    private val historyStore = ConversationHistoryStore(application)
    private val chatRepository = ChatRepository(historyStore, settingsRepository)
    private val commandExecutor = CommandExecutor(application)

    private val speechManager = SpeechRecognizerManager.getInstance(application)
    private val ttsManager = TextToSpeechManager(application)

    private val _statusText = MutableStateFlow("Tap the mic or say \"Hey Zain\"")

    val uiState: StateFlow<HomeUiState> = combine(
        speechManager.state,
        speechManager.partialTranscript,
        chatRepository.messages,
        _statusText
    ) { listeningState, partial, messages, status ->
        HomeUiState(listeningState, partial, messages, status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            chatRepository.loadHistory()
        }
        viewModelScope.launch {
            settingsRepository.wakeWord.collect { speechManager.updateWakeWord(it) }
        }
        viewModelScope.launch {
            settingsRepository.language.collect { speechManager.updateLanguage(it) }
        }
        viewModelScope.launch {
            settingsRepository.voiceName.collect { name ->
                if (name.isNotBlank() && name != "Default") ttsManager.setVoiceByName(name)
            }
        }
        viewModelScope.launch {
            speechManager.recognizedCommand.collect { text ->
                handleRecognizedSpeech(text)
            }
        }
    }

    fun startContinuousListening() {
        speechManager.start()
        _statusText.value = "Listening for \"Hey Zain\"…"
    }

    fun stopListening() {
        speechManager.stop()
        _statusText.value = "Paused"
    }

    fun onMicButtonTapped() {
        speechManager.listenForCommandNow()
        _statusText.value = "Listening…"
    }

    private fun handleRecognizedSpeech(text: String) {
        viewModelScope.launch {
            when (val command = CommandParser.parse(text)) {
                is VoiceCommand.DeviceAction -> {
                    chatRepository.appendUserOnly(text)
                    _statusText.value = "Working on it…"
                    val result = commandExecutor.execute(command.action, command.rawText)
                    chatRepository.appendAssistantReply(result)
                    ttsManager.speak(result)
                    _statusText.value = "Listening for \"Hey Zain\"…"
                }
                is VoiceCommand.Conversation -> {
                    _statusText.value = "Thinking…"
                    val result = chatRepository.sendUserMessage(command.text)
                    val reply = when (result) {
                        is com.zain.assistant.data.repository.ChatResult.Success -> result.reply
                        is com.zain.assistant.data.repository.ChatResult.Failure -> result.message
                    }
                    ttsManager.speak(reply)
                    _statusText.value = "Listening for \"Hey Zain\"…"
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { chatRepository.clearHistory() }
    }

    override fun onCleared() {
        ttsManager.shutdown()
        super.onCleared()
    }
}
