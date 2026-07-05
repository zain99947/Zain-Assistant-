package com.zain.assistant.ui.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.zain.assistant.ui.theme.CyanGlow
import com.zain.assistant.ui.theme.ElectricBlue
import com.zain.assistant.voice.ListeningState

@Composable
fun AvatarOrb(
    listeningState: ListeningState,
    modifier: Modifier = Modifier
) {
    val isBusy = listeningState == ListeningState.PROCESSING || listeningState == ListeningState.LISTENING_FOR_COMMAND

    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isBusy) 2500 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.size(180.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2.4f

        // Soft glowing core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ElectricBlue.copy(alpha = 0.55f), Color.Transparent),
                center = center,
                radius = radius * 1.6f
            ),
            radius = radius * 1.6f,
            center = center
        )

        // Rotating outer ring made of arcs
        rotate(rotation) {
            drawCircle(
                color = CyanGlow.copy(alpha = 0.8f),
                radius = radius,
                center = center,
                style = Stroke(width = 4f)
            )
        }
        rotate(-rotation * 0.6f) {
            drawCircle(
                color = ElectricBlue.copy(alpha = 0.5f),
                radius = radius * 0.75f,
                center = center,
                style = Stroke(width = 3f)
            )
        }

        // Solid inner core
        drawCircle(
            brush = Brush.radialGradient(listOf(ElectricBlue, CyanGlow)),
            radius = radius * 0.45f,
            center = center
        )
    }
}
