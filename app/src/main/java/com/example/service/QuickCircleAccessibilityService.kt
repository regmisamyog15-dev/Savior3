package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.example.data.QuickCirclePreferences
import com.example.model.QuickActionType
import com.example.overlay.FloatingBubbleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class QuickCircleAccessibilityService : AccessibilityService(), SystemActionController {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var preferences: QuickCirclePreferences
    private lateinit var audioManager: AudioManager
    var bubbleManager: FloatingBubbleManager? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = QuickCirclePreferences.getInstance(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceRunning = true

        val bm = FloatingBubbleManager(this, this)
        bubbleManager = bm
        com.example.livess.LiveSsCaptureService.instance?.attachBubbleManager(bm)

        // Observe preference changes to update bubble dynamically
        preferences.isEnabled
            .onEach { enabled ->
                if (enabled) {
                    bubbleManager?.showBubble()
                } else {
                    bubbleManager?.hideBubble()
                }
            }
            .launchIn(serviceScope)

        preferences.bubbleSizeDp
            .onEach { bubbleManager?.updateAppearance() }
            .launchIn(serviceScope)

        preferences.bubbleOpacity
            .onEach { bubbleManager?.updateAppearance() }
            .launchIn(serviceScope)

        if (preferences.isEnabled.value) {
            bubbleManager?.showBubble()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event != null) {
            PiPDiagnosticManager.onAccessibilityEvent(event)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "QuickCircleAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        if (instance === this) {
            instance = null
        }
        bubbleManager?.hideBubble()
        bubbleManager = null
        serviceScope.cancel()
    }

    override fun performAction(
        action: QuickActionType,
        callback: (current: Int, max: Int, isMuted: Boolean) -> Unit
    ) {
        when (action) {
            QuickActionType.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            QuickActionType.RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            QuickActionType.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            QuickActionType.QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            QuickActionType.LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
            QuickActionType.SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
            }
            QuickActionType.SPLIT_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                }
            }
            QuickActionType.BACKGROUND_PLAY -> {
                if (bubbleManager != null) {
                    bubbleManager?.showMediaPanel()
                } else {
                    com.example.browser.SaviorBrowserActivity.launch(this)
                }
            }
            QuickActionType.VOLUME -> {
                val info = getVolumeInfo()
                callback(info.first, info.second, info.third)
                bubbleManager?.showVolumePanel(info.first, info.second, info.third)
            }
            QuickActionType.VOLUME_UP -> {
                val stream = preferences.defaultStreamType.value.streamId
                audioManager.adjustStreamVolume(
                    stream,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
                )
                val info = getVolumeInfo()
                callback(info.first, info.second, info.third)
            }
            QuickActionType.VOLUME_DOWN -> {
                val stream = preferences.defaultStreamType.value.streamId
                audioManager.adjustStreamVolume(
                    stream,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
                )
                val info = getVolumeInfo()
                callback(info.first, info.second, info.third)
            }
            QuickActionType.VOLUME_MUTE -> {
                toggleMute(callback)
            }
            QuickActionType.MEDIA_PLAY_PAUSE -> {
                val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                val up = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                audioManager.dispatchMediaKeyEvent(down)
                audioManager.dispatchMediaKeyEvent(up)
            }
            QuickActionType.LIVE_SCREENSHOT,
            QuickActionType.LIVE_SS_1 -> {
                com.example.livess.LiveSsPermissionActivity.launchForScreenshot(this)
            }
            QuickActionType.SCREEN_RECORDER,
            QuickActionType.LIVE_SS_2 -> {
                com.example.livess.LiveSsPermissionActivity.launchForRecording(this)
            }
        }
    }

    override fun getVolumeInfo(): Triple<Int, Int, Boolean> {
        val stream = preferences.defaultStreamType.value.streamId
        val current = audioManager.getStreamVolume(stream)
        val max = audioManager.getStreamMaxVolume(stream)
        val isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.isStreamMute(stream) || current == 0
        } else {
            current == 0
        }
        return Triple(current, max, isMuted)
    }

    override fun setVolume(
        newVol: Int,
        playSound: Boolean,
        callback: (current: Int, max: Int, isMuted: Boolean) -> Unit
    ) {
        val stream = preferences.defaultStreamType.value.streamId
        val flags = AudioManager.FLAG_SHOW_UI or (if (playSound) AudioManager.FLAG_PLAY_SOUND else 0)
        audioManager.setStreamVolume(stream, newVol, flags)
        val info = getVolumeInfo()
        callback(info.first, info.second, info.third)
    }

    override fun toggleMute(callback: (current: Int, max: Int, isMuted: Boolean) -> Unit) {
        val stream = preferences.defaultStreamType.value.streamId
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val isMuted = audioManager.isStreamMute(stream)
            val dir = if (isMuted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE
            audioManager.adjustStreamVolume(stream, dir, AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND)
        } else {
            val current = audioManager.getStreamVolume(stream)
            if (current > 0) {
                audioManager.setStreamVolume(stream, 0, AudioManager.FLAG_SHOW_UI)
            } else {
                audioManager.setStreamVolume(stream, audioManager.getStreamMaxVolume(stream) / 2, AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND)
            }
        }
        val info = getVolumeInfo()
        callback(info.first, info.second, info.third)
    }

    companion object {
        private const val TAG = "QuickCircleA11y"
        var isServiceRunning: Boolean = false
            private set
        var instance: QuickCircleAccessibilityService? = null
            private set
    }
}
