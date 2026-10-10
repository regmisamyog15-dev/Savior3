package com.example.service

import android.content.Context
import android.provider.Settings
import com.example.data.QuickCirclePreferences
import com.example.overlay.FloatingBubbleManager

/**
 * Coordinates Floating Bubble lifecycle across AccessibilityService and standalone overlay mode.
 * Guarantees that the assistive Orb and on-screen Volume Panel work immediately once Overlay
 * permission is granted, even before AccessibilityService is configured.
 */
object FloatingBubbleHost {

    private var standaloneBubbleManager: FloatingBubbleManager? = null

    fun getBubbleManager(context: Context): FloatingBubbleManager? {
        val a11yManager = QuickCircleAccessibilityService.instance?.bubbleManager
        if (a11yManager != null) {
            return a11yManager
        }

        if (standaloneBubbleManager == null && Settings.canDrawOverlays(context)) {
            val controller = AppSystemActionController(context.applicationContext)
            standaloneBubbleManager = FloatingBubbleManager(context.applicationContext, controller)
            com.example.livess.LiveSsCaptureService.instance?.attachBubbleManager(standaloneBubbleManager!!)
        }
        return standaloneBubbleManager
    }

    fun syncState(context: Context) {
        val prefs = QuickCirclePreferences.getInstance(context)
        val shouldBeVisible = prefs.isEnabled.value

        // If AccessibilityService is connected, let it control the primary bubble
        val a11yService = QuickCircleAccessibilityService.instance
        if (a11yService != null) {
            standaloneBubbleManager?.hideBubble()
            standaloneBubbleManager = null
            if (shouldBeVisible) {
                a11yService.bubbleManager?.showBubble()
            } else {
                a11yService.bubbleManager?.hideBubble()
            }
            return
        }

        // Standalone overlay mode
        if (shouldBeVisible && Settings.canDrawOverlays(context)) {
            val bm = getBubbleManager(context)
            bm?.showBubble()
        } else {
            standaloneBubbleManager?.hideBubble()
            standaloneBubbleManager = null
        }
    }

    fun showVolumeHud(context: Context) {
        val bm = getBubbleManager(context)
        val controller = AppSystemActionController(context)
        val info = controller.getVolumeInfo()
        bm?.showVolumePanel(info.first, info.second, info.third)
    }

    fun showMediaHud(context: Context) {
        val bm = getBubbleManager(context)
        if (bm != null) {
            bm.showMediaPanel()
        } else {
            com.example.browser.SaviorBrowserActivity.launch(context)
        }
    }

    fun hideAllOverlays(context: Context) {
        getBubbleManager(context)?.hideAllOverlays()
    }

    fun restoreAllOverlays(context: Context) {
        getBubbleManager(context)?.restoreAllOverlays()
    }
}
