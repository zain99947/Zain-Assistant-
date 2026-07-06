package com.zain.assistant.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.zain.assistant.MainActivity
import com.zain.assistant.R
import com.zain.assistant.ZainApplication
import com.zain.assistant.voice.SpeechRecognizerManager

/**
 * Foreground service that owns a [SpeechRecognizerManager] instance so wake-word listening
 * keeps running even when the app is backgrounded. Required on modern Android for any
 * long-running microphone use outside of a foreground activity.
 */
class VoiceForegroundService : Service() {

    private var recognizerManager: SpeechRecognizerManager? = null

    override fun onCreate() {
        super.onCreate()
        recognizerManager = SpeechRecognizerManager.getInstance(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        recognizerManager?.start()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, ZainApplication.CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_listening_title))
            .setContentText(getString(R.string.notification_listening_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        recognizerManager?.stop()
        recognizerManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        private const val NOTIFICATION_ID = 42
    }
}
