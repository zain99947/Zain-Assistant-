package com.zain.assistant.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zain.assistant.ui.theme.CyanGlow
import com.zain.assistant.ui.theme.DangerRed
import com.zain.assistant.ui.theme.TextSecondary
import com.zain.assistant.voice.ListeningState

@Composable
fun StatusIndicator(
    listeningState: ListeningState,
    statusText: String,
    modifier: Modifier = Modifier
) {
    val dotColor = when (listeningState) {
        ListeningState.LISTENING_FOR_WAKE_WORD, ListeningState.LISTENING_FOR_COMMAND -> CyanGlow
        ListeningState.PROCESSING, ListeningState.SPEAKING -> com.zain.assistant.ui.theme.ElectricBlue
        ListeningState.ERROR -> DangerRed
        ListeningState.IDLE -> TextSecondary
    }

    Row(
        modifier = modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
