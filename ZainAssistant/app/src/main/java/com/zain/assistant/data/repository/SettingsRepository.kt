package com.zain.assistant.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "zain_settings")

/**
 * Holds all user-configurable settings: API key/endpoint for the AI provider,
 * wake word, TTS voice, dark mode, and language. Backed by Jetpack DataStore so
 * values survive process death and are observed reactively via Flow.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val API_MODEL = stringPreferencesKey("api_model")
        val WAKE_WORD = stringPreferencesKey("wake_word")
        val VOICE_NAME = stringPreferencesKey("voice_name")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { it[Keys.API_KEY] ?: "" }
    val apiBaseUrl: Flow<String> = context.dataStore.data.map {
        it[Keys.API_BASE_URL] ?: DEFAULT_BASE_URL
    }
    val apiModel: Flow<String> = context.dataStore.data.map { it[Keys.API_MODEL] ?: DEFAULT_MODEL }
    val wakeWord: Flow<String> = context.dataStore.data.map { it[Keys.WAKE_WORD] ?: DEFAULT_WAKE_WORD }
    val voiceName: Flow<String> = context.dataStore.data.map { it[Keys.VOICE_NAME] ?: "Default" }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: true }
    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en-US" }

    suspend fun setApiKey(value: String) = context.dataStore.edit { it[Keys.API_KEY] = value }
    suspend fun setApiBaseUrl(value: String) = context.dataStore.edit { it[Keys.API_BASE_URL] = value }
    suspend fun setApiModel(value: String) = context.dataStore.edit { it[Keys.API_MODEL] = value }
    suspend fun setWakeWord(value: String) = context.dataStore.edit {
        it[Keys.WAKE_WORD] = value.lowercase().trim()
    }
    suspend fun setVoiceName(value: String) = context.dataStore.edit { it[Keys.VOICE_NAME] = value }
    suspend fun setDarkMode(value: Boolean) = context.dataStore.edit { it[Keys.DARK_MODE] = value }
    suspend fun setLanguage(value: String) = context.dataStore.edit { it[Keys.LANGUAGE] = value }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.openai.com/"
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_WAKE_WORD = "hey zain"
    }
}
