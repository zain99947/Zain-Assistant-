package com.zain.assistant.actions

import android.content.Context
import android.content.Intent

/** Ties a parsed [ActionType] + parameter to real device behavior and a spoken result string. */
class CommandExecutor(private val context: Context) {

    private val phoneActions = PhoneActionExecutor(context)
    private val systemInfo = SystemInfoProvider(context)

    fun execute(action: ActionType, parameter: String): String {
        return when (action) {
            ActionType.CALL_CONTACT -> phoneActions.callContact(parameter)
            ActionType.OPEN_CONTACTS -> phoneActions.openContacts()
            ActionType.SEND_SMS -> phoneActions.sendSms(parameter)
            ActionType.OPEN_APP -> phoneActions.openApp(parameter)
            ActionType.OPEN_CAMERA -> phoneActions.openCamera()
            ActionType.OPEN_GALLERY -> phoneActions.openGallery()
            ActionType.OPEN_CHROME -> phoneActions.openChrome()
            ActionType.OPEN_SETTINGS -> phoneActions.openSystemSettings()
            ActionType.OPEN_CALCULATOR -> phoneActions.openCalculator()
            ActionType.OPEN_CALENDAR -> phoneActions.openCalendar()
            ActionType.OPEN_CLOCK -> phoneActions.openClock()
            ActionType.OPEN_FILES -> phoneActions.openFiles()
            ActionType.OPEN_WHATSAPP -> phoneActions.openWhatsApp()
            ActionType.OPEN_YOUTUBE -> phoneActions.openYouTube()
            ActionType.OPEN_MAPS -> phoneActions.openMaps()
            ActionType.SET_ALARM -> phoneActions.setAlarm(parameter)
            ActionType.SET_TIMER -> phoneActions.setTimer(parameter)
            ActionType.CREATE_REMINDER -> phoneActions.createReminder(parameter)
            ActionType.SHOW_DATE -> systemInfo.currentDate()
            ActionType.SHOW_TIME -> systemInfo.currentTime()
            ActionType.SHOW_BATTERY -> systemInfo.batteryPercentage()
            ActionType.SHOW_STORAGE -> systemInfo.storageInfo()
            ActionType.SHOW_RAM -> systemInfo.ramInfo()
            ActionType.SHOW_NETWORK_STATUS -> systemInfo.networkStatus()
            ActionType.FLASHLIGHT_ON -> phoneActions.setFlashlight(true)
            ActionType.FLASHLIGHT_OFF -> phoneActions.setFlashlight(false)
            ActionType.VOLUME_UP -> phoneActions.adjustVolume(true)
            ActionType.VOLUME_DOWN -> phoneActions.adjustVolume(false)
            ActionType.BRIGHTNESS_UP -> phoneActions.adjustBrightness(true)
            ActionType.BRIGHTNESS_DOWN -> phoneActions.adjustBrightness(false)
            ActionType.READ_NOTIFICATIONS -> readNotifications()
            ActionType.TELL_JOKE -> JokeBank.random()
            ActionType.SEARCH_WEB -> searchWeb(parameter)
            ActionType.UNKNOWN -> "I'm not sure how to help with that yet."
        }
    }

    private fun readNotifications(): String {
        val notifications = com.zain.assistant.service.ZainNotificationListenerService.notifications.value
        if (notifications.isEmpty()) return "You have no active notifications."
        val spoken = notifications.take(5).joinToString(". ") { "${it.title}: ${it.text}" }
        return "You have ${notifications.size} notifications. $spoken"
    }

    private fun searchWeb(query: String): String {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Searching the web for $query."
        } catch (e: Exception) {
            "I couldn't search for $query."
        }
    }
}

object JokeBank {
    private val jokes = listOf(
        "Why don't scientists trust atoms? Because they make up everything.",
        "I told my computer I needed a break, and it said no problem — it froze immediately.",
        "Why did the smartphone go to therapy? It lost its contacts.",
        "I'm reading a book about anti-gravity. It's impossible to put down.",
        "Why do programmers prefer dark mode? Because light attracts bugs."
    )

    fun random(): String = jokes.random()
}
