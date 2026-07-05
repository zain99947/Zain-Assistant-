package com.zain.assistant.ui.permissions

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zain.assistant.service.ZainAccessibilityService
import com.zain.assistant.ui.theme.CyanGlow
import com.zain.assistant.ui.theme.DangerRed
import com.zain.assistant.util.Constants
import com.zain.assistant.util.PermissionUtils

private data class PermissionRow(
    val label: String,
    val isGranted: () -> Boolean,
    val onRequest: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshTick++ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val rows = remember(refreshTick) {
            listOf(
                PermissionRow("Microphone", { PermissionUtils.hasPermission(context, Manifest.permission.RECORD_AUDIO) }) {
                    multiplePermissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                },
                PermissionRow("Phone calls", { PermissionUtils.hasPermission(context, Manifest.permission.CALL_PHONE) }) {
                    multiplePermissionsLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                },
                PermissionRow("Contacts", { PermissionUtils.hasPermission(context, Manifest.permission.READ_CONTACTS) }) {
                    multiplePermissionsLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                },
                PermissionRow("SMS", { PermissionUtils.hasPermission(context, Manifest.permission.SEND_SMS) }) {
                    multiplePermissionsLauncher.launch(arrayOf(Manifest.permission.SEND_SMS))
                },
                PermissionRow("Camera", { PermissionUtils.hasPermission(context, Manifest.permission.CAMERA) }) {
                    multiplePermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                },
                PermissionRow("Notifications", {
                    android.os.Build.VERSION.SDK_INT < 33 || PermissionUtils.hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                }) {
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        multiplePermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                },
                PermissionRow("Modify system settings (brightness)", {
                    PermissionUtils.canWriteSystemSettings(context)
                }) {
                    PermissionUtils.openWriteSettingsScreen(context)
                },
                PermissionRow("Notification listener (read/dismiss notifications)", {
                    PermissionUtils.isNotificationListenerEnabled(context)
                }) {
                    PermissionUtils.openNotificationListenerSettings(context)
                },
                PermissionRow("Accessibility service (UI automation)", {
                    PermissionUtils.isAccessibilityServiceEnabled(
                        context,
                        "${context.packageName}/${ZainAccessibilityService::class.java.name}"
                    )
                }) {
                    PermissionUtils.openAccessibilitySettings(context)
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    "Android requires notification-listener and accessibility access to be turned on manually from system settings — no app can enable those for itself. Tap any item below to grant it.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            items(rows) { row ->
                PermissionListItem(row)
                Divider()
            }
        }
    }
}

@Composable
private fun PermissionListItem(row: PermissionRow) {
    val granted = row.isGranted()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (granted) CyanGlow else DangerRed
            )
            Text(row.label, modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
        }
        if (!granted) {
            TextButton(onClick = row.onRequest) { Text("Grant") }
        }
    }
}
