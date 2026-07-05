package com.zain.assistant.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility Service used for UI automation Android otherwise restricts to the user
 * themselves — for example, tapping a "dismiss" button inside a notification shade item,
 * or reading on-screen text content for context. Android requires the user to manually
 * enable this in Settings > Accessibility; no app can turn this on for itself, by design,
 * since it is a highly privileged capability.
 */
class ZainAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally minimal: this hook is where targeted UI automation (e.g. locating
        // and clicking a specific on-screen control by text/content-description) would be
        // implemented on demand for a given command, using findAccessibilityNodeInfosByText
        // on `rootInActiveWindow` and AccessibilityNodeInfo.performAction(...).
    }

    /** Attempts to find and click a node whose visible text or content description contains
     *  [label] on the current screen. Returns true if a matching, clickable node was found. */
    fun clickNodeByLabel(label: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val matches = root.findAccessibilityNodeInfosByText(label)
        val target = matches.firstOrNull { it.isClickable } ?: matches.firstOrNull()?.let { findClickableAncestor(it) }
        return target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    private fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    override fun onInterrupt() {}
}
