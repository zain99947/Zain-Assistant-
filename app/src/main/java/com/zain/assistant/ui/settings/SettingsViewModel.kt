package com.zain.assistant.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zain.assistant.ZainApplication
import com.zain.assistant.voice.TextToSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val apiBaseUrl: String = "",
    val apiModel: String = "",
    val wakeWord: String = "",
    val voiceName: String = "Default",
    val darkMode: Boolean = true,
    val language: String = "en-US",
    val availableVoices: List<String> = emptyList()
)

val SUPPORTED_LANGUAGES = listOf(
    "en-US" to "English (US)",
    "en-GB" to "English (UK)",
    "ur-PK" to "Urdu",
    "ar-SA" to "Arabic",
    "es-ES" to "Spanish",
    "fr-FR" to "French",
    "hi-IN" to "Hindi"
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = (application as ZainApplication).settingsRepository
    private val ttsManager = TextToSpeechManager(application)

    private val _availableVoices = MutableStateFlow<List<String>>(listOf("Default"))

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.apiKey,
        settingsRepository.apiBaseUrl,
        settingsRepository.apiModel,
        settingsRepository.wakeWord,
        settingsRepository.voiceName,
        settingsRepository.darkMode,
        settingsRepository.language,
    ) { values ->
        SettingsUiState(
            apiKey = values[0] as String,
            apiBaseUrl = values[1] as String,
            apiModel = values[2] as String,
            wakeWord = values[3] as String,
            voiceName = values[4] as String,
            darkMode = values[5] as Boolean,
            language = values[6] as String,
            availableVoices = _availableVoices.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        viewModelScope.launch {
            // Give the TTS engine a moment to initialize before reading voices.
            kotlinx.coroutines.delay(800)
            val voices = ttsManager.availableVoiceNames()
            _availableVoices.value = listOf("Default") + voices
        }
    }

    fun setApiKey(value: String) = viewModelScope.launch { settingsRepository.setApiKey(value) }
    fun setApiBaseUrl(value: String) = viewModelScope.launch { settingsRepository.setApiBaseUrl(value) }
    fun setApiModel(value: String) = viewModelScope.launch { settingsRepository.setApiModel(value) }
    fun setWakeWord(value: String) = viewModelScope.launch { settingsRepository.setWakeWord(value) }
    fun setVoiceName(value: String) = viewModelScope.launch { settingsRepository.setVoiceName(value) }
    fun setDarkMode(value: Boolean) = viewModelScope.launch { settingsRepository.setDarkMode(value) }
    fun setLanguage(value: String) = viewModelScope.launch { settingsRepository.setLanguage(value) }

    override fun onCleared() {
        ttsManager.shutdown()
        super.onCleared()
    }
}
