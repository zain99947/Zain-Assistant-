package com.zain.assistant.ui.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zain.assistant.ui.theme.CyanGlow
import com.zain.assistant.ui.theme.CyanGlowSoft
import com.zain.assistant.ui.theme.ElectricBlue
import com.zain.assistant.voice.ListeningState

@Composable
fun MicButton(
    listeningState: ListeningState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = listeningState == ListeningState.LISTENING_FOR_COMMAND ||
        listeningState == ListeningState.LISTENING_FOR_WAKE_WORD

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ring, pulses when actively listening
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(pulseScale)
                .background(
                    brush = Brush.radialGradient(listOf(CyanGlowSoft, Color.Transparent)),
                    shape = CircleShape
                )
        )

        // Core button
        Box(
            modifier = Modifier
                .size(96.dp)
                .clickable(onClick = onClick)
                .background(
                    brush = Brush.linearGradient(listOf(ElectricBlue, CyanGlow)),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Microphone",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
