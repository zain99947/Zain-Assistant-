package com.zain.assistant.util

import android.Manifest
import android.os.Build

object Constants {

    /** Standard runtime permissions requested up front via the normal permission dialog flow. */
    val RUNTIME_PERMISSIONS: List<String> = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }

    const val PLAY_STORE_URL_PREFIX = "https://play.google.com/store/apps/details?id="
}
