package com.zain.assistant

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.zain.assistant.service.VoiceForegroundService
import com.zain.assistant.ui.navigation.ZainNavGraph
import com.zain.assistant.ui.theme.ZainAssistantTheme
import com.zain.assistant.util.Constants
import com.zain.assistant.util.PermissionUtils

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        startVoiceServiceIfReady()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestAllPermissionsIfNeeded()

        setContent {
            val app = application as ZainApplication
            var darkMode by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                app.settingsRepository.darkMode.collect { darkMode = it }
            }

            ZainAssistantTheme(darkTheme = darkMode) {
                ZainNavGraph()
            }
        }
    }

    private fun requestAllPermissionsIfNeeded() {
        val missing = Constants.RUNTIME_PERMISSIONS.filter {
            !PermissionUtils.hasPermission(this, it)
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            startVoiceServiceIfReady()
        }
    }

    private fun startVoiceServiceIfReady() {
        if (PermissionUtils.hasPermission(this, android.Manifest.permission.RECORD_AUDIO)) {
            val intent = Intent(this, VoiceForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                startService(intent)
            }
        }
    }
}
