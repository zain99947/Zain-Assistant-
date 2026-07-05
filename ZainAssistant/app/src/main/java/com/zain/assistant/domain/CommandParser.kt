package com.zain.assistant.domain

import com.zain.assistant.actions.ActionType
import com.zain.assistant.domain.model.VoiceCommand

/**
 * Lightweight, deterministic offline command parser. Runs before anything is sent to the
 * AI provider so common device actions (calling, opening apps, system info, alarms, etc.)
 * work instantly and without network access. If nothing matches, the text is forwarded
 * to the AI conversation model instead.
 */
object CommandParser {

    data class ParsedCommand(val action: ActionType, val parameter: String? = null)

    fun parse(rawText: String): VoiceCommand {
        val text = rawText.lowercase().trim()
        val parsed = matchCommand(text)
        return if (parsed != null) {
            VoiceCommand.DeviceAction(parsed.action, parsed.parameter ?: rawText)
        } else {
            VoiceCommand.Conversation(rawText)
        }
    }

    private fun matchCommand(text: String): ParsedCommand? {
        // Calling a contact: "call ali", "phone ali", "dial ali"
        contactNameAfter(text, listOf("call ", "phone ", "dial "))?.let {
            return ParsedCommand(ActionType.CALL_CONTACT, it)
        }

        // SMS: "send a message to ali", "text ali", "send sms to ali"
        contactNameAfter(text, listOf("send sms to ", "send a message to ", "send message to ", "text "))?.let {
            return ParsedCommand(ActionType.SEND_SMS, it)
        }

        if (containsAny(text, "open contacts", "show contacts")) return ParsedCommand(ActionType.OPEN_CONTACTS)
        if (containsAny(text, "open camera")) return ParsedCommand(ActionType.OPEN_CAMERA)
        if (containsAny(text, "open gallery", "open photos")) return ParsedCommand(ActionType.OPEN_GALLERY)
        if (containsAny(text, "open chrome", "open browser")) return ParsedCommand(ActionType.OPEN_CHROME)
        if (containsAny(text, "open settings")) return ParsedCommand(ActionType.OPEN_SETTINGS)
        if (containsAny(text, "open calculator")) return ParsedCommand(ActionType.OPEN_CALCULATOR)
        if (containsAny(text, "open calendar")) return ParsedCommand(ActionType.OPEN_CALENDAR)
        if (containsAny(text, "open clock")) return ParsedCommand(ActionType.OPEN_CLOCK)
        if (containsAny(text, "open files", "open file manager")) return ParsedCommand(ActionType.OPEN_FILES)
        if (containsAny(text, "open whatsapp")) return ParsedCommand(ActionType.OPEN_WHATSAPP)
        if (containsAny(text, "open youtube")) return ParsedCommand(ActionType.OPEN_YOUTUBE)
        if (containsAny(text, "open maps", "open google maps")) return ParsedCommand(ActionType.OPEN_MAPS)

        // Generic "open <app name>" fallback (checked after specific apps above)
        if (text.startsWith("open ") || text.startsWith("launch ") || text.startsWith("start ")) {
            val appName = text.substringAfter("open ", "")
                .ifBlank { text.substringAfter("launch ", "") }
                .ifBlank { text.substringAfter("start ", "") }
                .trim()
            if (appName.isNotBlank()) return ParsedCommand(ActionType.OPEN_APP, appName)
        }

        if (containsAny(text, "set an alarm", "set alarm")) {
            return ParsedCommand(ActionType.SET_ALARM, extractAfter(text, listOf("set an alarm for ", "set alarm for ", "set an alarm ", "set alarm ")))
        }
        if (containsAny(text, "set a timer", "set timer", "start a timer", "start timer")) {
            return ParsedCommand(ActionType.SET_TIMER, extractAfter(text, listOf("set a timer for ", "set timer for ", "start a timer for ", "for ")))
        }
        if (containsAny(text, "remind me", "create a reminder", "set a reminder")) {
            return ParsedCommand(ActionType.CREATE_REMINDER, extractAfter(text, listOf("remind me to ", "remind me ", "create a reminder to ", "set a reminder to ")))
        }

        if (containsAny(text, "what's today's date", "what is today's date", "today's date", "what day is it", "show me the date", "show date")) {
            return ParsedCommand(ActionType.SHOW_DATE)
        }
        if (containsAny(text, "what's the time", "what is the time", "current time", "show me the time", "show time", "what time is it")) {
            return ParsedCommand(ActionType.SHOW_TIME)
        }
        if (containsAny(text, "battery percentage", "battery level", "how much battery", "show battery")) {
            return ParsedCommand(ActionType.SHOW_BATTERY)
        }
        if (containsAny(text, "storage information", "how much storage", "show storage", "storage space")) {
            return ParsedCommand(ActionType.SHOW_STORAGE)
        }
        if (containsAny(text, "ram usage", "show ram", "memory usage")) {
            return ParsedCommand(ActionType.SHOW_RAM)
        }
        if (containsAny(text, "internet status", "am i online", "network status", "wifi status")) {
            return ParsedCommand(ActionType.SHOW_NETWORK_STATUS)
        }

        if (containsAny(text, "turn on flashlight", "flashlight on", "turn on the flashlight", "turn on the torch")) {
            return ParsedCommand(ActionType.FLASHLIGHT_ON)
        }
        if (containsAny(text, "turn off flashlight", "flashlight off", "turn off the flashlight", "turn off the torch")) {
            return ParsedCommand(ActionType.FLASHLIGHT_OFF)
        }

        if (containsAny(text, "increase volume", "volume up", "turn up the volume", "raise the volume")) {
            return ParsedCommand(ActionType.VOLUME_UP)
        }
        if (containsAny(text, "decrease volume", "volume down", "turn down the volume", "lower the volume")) {
            return ParsedCommand(ActionType.VOLUME_DOWN)
        }
        if (containsAny(text, "increase brightness", "brightness up", "brighten the screen")) {
            return ParsedCommand(ActionType.BRIGHTNESS_UP)
        }
        if (containsAny(text, "decrease brightness", "brightness down", "dim the screen")) {
            return ParsedCommand(ActionType.BRIGHTNESS_DOWN)
        }

        if (containsAny(text, "read my notifications", "read notifications")) {
            return ParsedCommand(ActionType.READ_NOTIFICATIONS)
        }

        if (containsAny(text, "tell me a joke", "tell a joke", "make me laugh")) {
            return ParsedCommand(ActionType.TELL_JOKE)
        }
        if (containsAny(text, "search the web", "search for", "google")) {
            return ParsedCommand(ActionType.SEARCH_WEB, extractAfter(text, listOf("search the web for ", "search for ", "google ")))
        }

        return null
    }

    private fun containsAny(text: String, vararg phrases: String): Boolean = phrases.any { text.contains(it) }

    private fun extractAfter(text: String, prefixes: List<String>): String {
        for (prefix in prefixes) {
            if (text.contains(prefix)) return text.substringAfter(prefix).trim()
        }
        return text
    }

    private fun contactNameAfter(text: String, prefixes: List<String>): String? {
        for (prefix in prefixes) {
            if (text.startsWith(prefix)) {
                val name = text.removePrefix(prefix).trim()
                if (name.isNotBlank()) return name
            }
        }
        return null
    }
}
