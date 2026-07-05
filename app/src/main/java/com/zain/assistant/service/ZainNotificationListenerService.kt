package com.zain.assistant.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ZainNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val key: String
)

/**
 * Notification Listener that captures active notifications so they can be read aloud
 * ("read my notifications") or dismissed on request. Like Accessibility Service access,
 * Android requires the user to manually grant Notification Listener access from
 * Settings > Notifications > Special app access — this cannot be auto-granted by the app.
 */
class ZainNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        refreshSnapshot()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshSnapshot()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshSnapshot()
    }

    private fun refreshSnapshot() {
        val list = try {
            activeNotifications.mapNotNull { sbn ->
                val extras = sbn.notification.extras
                val title = extras.getCharSequence("android.title")?.toString().orEmpty()
                val text = extras.getCharSequence("android.text")?.toString().orEmpty()
                if (title.isBlank() && text.isBlank()) null
                else ZainNotification(sbn.packageName, title, text, sbn.key)
            }
        } catch (e: Exception) {
            emptyList()
        }
        _notifications.value = list
    }

    fun dismiss(key: String) {
        try {
            cancelNotification(key)
        } catch (e: Exception) {
            // Some system notifications cannot be dismissed by a listener; ignored by design.
        }
    }

    companion object {
        private var instance: ZainNotificationListenerService? = null

        private val _notifications = MutableStateFlow<List<ZainNotification>>(emptyList())
        val notifications: StateFlow<List<ZainNotification>> = _notifications.asStateFlow()

        fun isConnected(): Boolean = instance != null

        fun dismissByKey(key: String) {
            instance?.dismiss(key)
        }
    }
}
