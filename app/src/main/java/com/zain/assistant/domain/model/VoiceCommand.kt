package com.zain.assistant.domain.model

/** Result of parsing recognized speech: either a recognized offline device command, or free text
 *  that should be forwarded to the AI conversation model. */
sealed class VoiceCommand {
    data class DeviceAction(val action: com.zain.assistant.actions.ActionType, val rawText: String) : VoiceCommand()
    data class Conversation(val text: String) : VoiceCommand()
}
