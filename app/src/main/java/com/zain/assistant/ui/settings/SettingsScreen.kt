package com.zain.assistant.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { SectionHeader("AI Provider") }
            item {
                var apiKey by remember(uiState.apiKey) { mutableStateOf(uiState.apiKey) }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { viewModel.setApiKey(apiKey) }) { Text("Save") }
                    }
                )
            }
            item { Spacer8() }
            item {
                var baseUrl by remember(uiState.apiBaseUrl) { mutableStateOf(uiState.apiBaseUrl) }
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("API Base URL (OpenAI-compatible)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { viewModel.setApiBaseUrl(baseUrl) }) { Text("Save") }
                    }
                )
            }
            item { Spacer8() }
            item {
                var model by remember(uiState.apiModel) { mutableStateOf(uiState.apiModel) }
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { viewModel.setApiModel(model) }) { Text("Save") }
                    }
                )
            }

            item { Divider(modifier = Modifier.padding(vertical = 20.dp)) }

            item { SectionHeader("Voice") }
            item {
                var wakeWord by remember(uiState.wakeWord) { mutableStateOf(uiState.wakeWord) }
                OutlinedTextField(
                    value = wakeWord,
                    onValueChange = { wakeWord = it },
                    label = { Text("Wake word") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { viewModel.setWakeWord(wakeWord) }) { Text("Save") }
                    }
                )
            }
            item { Spacer8() }
            item {
                VoiceSelector(
                    voices = uiState.availableVoices,
                    selected = uiState.voiceName,
                    onSelect = { viewModel.setVoiceName(it) }
                )
            }
            item { Spacer8() }
            item {
                LanguageSelector(
                    selected = uiState.language,
                    onSelect = { viewModel.setLanguage(it) }
                )
            }

            item { Divider(modifier = Modifier.padding(vertical = 20.dp)) }

            item { SectionHeader("Appearance") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark mode", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = uiState.darkMode, onCheckedChange = { viewModel.setDarkMode(it) })
                }
            }

            item { Divider(modifier = Modifier.padding(vertical = 20.dp)) }

            item { SectionHeader("Permissions") }
            item {
                TextButton(onClick = onOpenPermissions) {
                    Text("Manage app permissions")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun Spacer8() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))
}

@Composable
private fun VoiceSelector(voices: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = true }) {
            Text("Voice: $selected")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            voices.forEach { voice ->
                DropdownMenuItem(text = { Text(voice) }, onClick = {
                    onSelect(voice)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun LanguageSelector(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = SUPPORTED_LANGUAGES.firstOrNull { it.first == selected }?.second ?: selected
    Column {
        TextButton(onClick = { expanded = true }) {
            Text("Language: $label")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SUPPORTED_LANGUAGES.forEach { (tag, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    onSelect(tag)
                    expanded = false
                })
            }
        }
    }
}
