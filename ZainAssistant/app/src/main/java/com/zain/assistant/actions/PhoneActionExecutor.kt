package com.zain.assistant.actions

import android.Manifest
import android.app.AlarmManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Executes device/phone-control actions. Every method here is defensive: it checks
 * permissions/availability first and returns a speakable result string describing what
 * happened (or why it couldn't), which the caller feeds straight into text-to-speech.
 */
class PhoneActionExecutor(private val context: Context) {

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun startActivitySafely(intent: Intent, failureMessage: String): String {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Done."
        } catch (e: Exception) {
            failureMessage
        }
    }

    // ---------- Calls & Contacts ----------

    fun callContact(name: String): String {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            return "I need contacts permission to find $name. Please grant it in Settings."
        }
        val number = findContactNumber(name) ?: return "I couldn't find a contact named $name."
        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            return "I need call permission to phone $name. Please grant it in Settings."
        }
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        return startActivitySafely(intent, "I couldn't place the call to $name.")
            .let { if (it == "Done.") "Calling $name." else it }
    }

    private fun findContactNumber(name: String): String? {
        val resolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex >= 0) return it.getString(numberIndex)
            }
        }
        return null
    }

    fun openContacts(): String {
        val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
        return startActivitySafely(intent, "I couldn't open Contacts.")
    }

    fun sendSms(name: String): String {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            return "I need contacts permission to message $name."
        }
        val number = findContactNumber(name)
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${number ?: ""}"))
        return startActivitySafely(intent, "I couldn't open messaging for $name.")
            .let { if (it == "Done.") "Opening a message to $name." else it }
    }

    // ---------- App launching ----------

    fun openApp(appName: String): String {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val match = apps.firstOrNull { app ->
            pm.getApplicationLabel(app).toString().contains(appName, ignoreCase = true)
        }
        val launchIntent = match?.let { pm.getLaunchIntentForPackage(it.packageName) }
            ?: return "I couldn't find an app called $appName."
        return startActivitySafely(launchIntent, "I couldn't open $appName.")
            .let { if (it == "Done.") "Opening $appName." else it }
    }

    fun openByPackage(packageName: String, humanName: String): String {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return "$humanName isn't installed on this device."
        return startActivitySafely(intent, "I couldn't open $humanName.")
            .let { if (it == "Done.") "Opening $humanName." else it }
    }

    fun openCamera(): String = startActivitySafely(Intent(MediaStore.ACTION_IMAGE_CAPTURE), "I couldn't open the camera.")
        .let { if (it == "Done.") "Opening Camera." else it }

    fun openGallery(): String {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "image/*"
        }
        return startActivitySafely(intent, "I couldn't open the gallery.")
            .let { if (it == "Done.") "Opening Gallery." else it }
    }

    fun openChrome(): String = openByPackage("com.android.chrome", "Chrome")

    fun openSystemSettings(): String = startActivitySafely(Intent(Settings.ACTION_SETTINGS), "I couldn't open Settings.")
        .let { if (it == "Done.") "Opening Settings." else it }

    fun openCalculator(): String {
        val candidates = listOf(
            "com.google.android.calculator",
            "com.android.calculator2",
            "com.miui.calculator",
            "com.sec.android.app.popupcalculator"
        )
        for (pkg in candidates) {
            context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                return startActivitySafely(it, "I couldn't open the calculator.")
                    .let { r -> if (r == "Done.") "Opening Calculator." else r }
            }
        }
        return "I couldn't find a calculator app on this device."
    }

    fun openCalendar(): String {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = ContentUris.appendId(Uri.parse("content://com.android.calendar/time"), System.currentTimeMillis()).build()
        }
        return startActivitySafely(intent, "I couldn't open the calendar.")
            .let { if (it == "Done.") "Opening Calendar." else it }
    }

    fun openClock(): String {
        val candidates = listOf("com.google.android.deskclock", "com.android.deskclock")
        for (pkg in candidates) {
            context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                return startActivitySafely(it, "I couldn't open the clock.")
                    .let { r -> if (r == "Done.") "Opening Clock." else r }
            }
        }
        return startActivitySafely(Intent(AlarmClock.ACTION_SHOW_ALARMS), "I couldn't open the clock.")
    }

    fun openFiles(): String {
        val candidates = listOf("com.google.android.apps.nbu.files", "com.android.documentsui")
        for (pkg in candidates) {
            context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                return startActivitySafely(it, "I couldn't open Files.")
                    .let { r -> if (r == "Done.") "Opening Files." else r }
            }
        }
        return "I couldn't find a file manager on this device."
    }

    fun openWhatsApp(): String = openByPackage("com.whatsapp", "WhatsApp")

    fun openYouTube(): String = openByPackage("com.google.android.youtube", "YouTube")

    fun openMaps(): String = openByPackage("com.google.android.apps.maps", "Maps")

    // ---------- Alarms, timers, reminders ----------

    fun setAlarm(spec: String): String {
        val (hour, minute) = parseTimeSpec(spec) ?: (8 to 0)
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        return startActivitySafely(intent, "I couldn't set that alarm.")
            .let { if (it == "Done.") "Alarm set for ${formatTime(hour, minute)}." else it }
    }

    fun setTimer(spec: String): String {
        val seconds = parseDurationSeconds(spec) ?: 300
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        return startActivitySafely(intent, "I couldn't start that timer.")
            .let { if (it == "Done.") "Timer started for ${seconds / 60} minutes." else it }
    }

    fun createReminder(text: String): String {
        // No universal reminder intent exists across all launchers/calendar apps, so we
        // create a calendar event a few minutes from now carrying the reminder text.
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = android.provider.CalendarContract.Events.CONTENT_URI
            putExtra(android.provider.CalendarContract.Events.TITLE, "Reminder: $text")
            putExtra(
                android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                System.currentTimeMillis() + 5 * 60 * 1000
            )
        }
        return startActivitySafely(intent, "I couldn't create that reminder.")
            .let { if (it == "Done.") "Reminder created: $text." else it }
    }

    private fun parseTimeSpec(spec: String): Pair<Int, Int>? {
        val cleaned = spec.lowercase().trim()
        val regex = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""")
        val match = regex.find(cleaned) ?: return null
        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: 0
        val meridian = match.groupValues[3]
        if (meridian == "pm" && hour < 12) hour += 12
        if (meridian == "am" && hour == 12) hour = 0
        return hour.coerceIn(0, 23) to minute.coerceIn(0, 59)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour % 12 == 0) 12 else hour % 12
        val suffix = if (hour < 12) "AM" else "PM"
        return String.format("%d:%02d %s", h, minute, suffix)
    }

    private fun parseDurationSeconds(spec: String): Int? {
        val cleaned = spec.lowercase().trim()
        val minuteMatch = Regex("""(\d+)\s*min""").find(cleaned)
        val secondMatch = Regex("""(\d+)\s*sec""").find(cleaned)
        val hourMatch = Regex("""(\d+)\s*hour""").find(cleaned)
        var total = 0
        hourMatch?.groupValues?.get(1)?.toIntOrNull()?.let { total += it * 3600 }
        minuteMatch?.groupValues?.get(1)?.toIntOrNull()?.let { total += it * 60 }
        secondMatch?.groupValues?.get(1)?.toIntOrNull()?.let { total += it }
        if (total == 0) {
            Regex("""(\d+)""").find(cleaned)?.groupValues?.get(1)?.toIntOrNull()?.let { total = it * 60 }
        }
        return if (total > 0) total else null
    }

    // ---------- Flashlight ----------

    fun setFlashlight(on: Boolean): String {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return "Flashlight isn't available on this device."
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return "This device doesn't have a flash."
            cameraManager.setTorchMode(cameraId, on)
            if (on) "Flashlight turned on." else "Flashlight turned off."
        } catch (e: Exception) {
            "I couldn't control the flashlight."
        }
    }

    // ---------- Volume & brightness ----------

    fun adjustVolume(up: Boolean): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val direction = if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        return if (up) "Volume increased." else "Volume decreased."
    }

    fun adjustBrightness(up: Boolean): String {
        if (!Settings.System.canWrite(context)) {
            return "I need the 'modify system settings' permission to change brightness. Please grant it in Settings."
        }
        return try {
            val current = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            val step = 40
            val newValue = (if (up) current + step else current - step).coerceIn(10, 255)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, newValue)
            if (up) "Brightness increased." else "Brightness decreased."
        } catch (e: Exception) {
            "I couldn't change the brightness on this device."
        }
    }

    // ---------- System info ----------

    fun openWriteSettingsPermissionScreen(): String {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
        return startActivitySafely(intent, "I couldn't open the permission screen.")
    }
}
