package com.zain.assistant.actions

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.StatFs
import android.text.format.Formatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Provides spoken-friendly answers for device status queries. */
class SystemInfoProvider(private val context: Context) {

    fun currentDate(): String {
        val format = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        return "Today is ${format.format(Date())}."
    }

    fun currentTime(): String {
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        return "It's ${format.format(Date())}."
    }

    fun batteryPercentage(): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "Battery is at $level percent."
    }

    fun storageInfo(): String {
        val stat = StatFs(context.filesDir.path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val usedBytes = totalBytes - availableBytes
        val total = Formatter.formatShortFileSize(context, totalBytes)
        val available = Formatter.formatShortFileSize(context, availableBytes)
        val used = Formatter.formatShortFileSize(context, usedBytes)
        return "You've used $used of $total storage, with $available free."
    }

    fun ramInfo(): String {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalMem = Formatter.formatShortFileSize(context, memInfo.totalMem)
        val availMem = Formatter.formatShortFileSize(context, memInfo.availMem)
        val usedMem = Formatter.formatShortFileSize(context, memInfo.totalMem - memInfo.availMem)
        return "You're using $usedMem of $totalMem RAM, with $availMem available."
    }

    fun networkStatus(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "You're currently offline."
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "You're currently offline."
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "You're connected to Wi-Fi and online."
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "You're connected via mobile data and online."
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "You're connected via Ethernet and online."
            else -> "You're online."
        }
    }
}
