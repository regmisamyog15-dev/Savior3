package com.example.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import com.example.data.QuickCirclePreferences
import com.example.model.QuickActionType
import com.example.model.StreamType

/**
 * Universal system and audio action controller for Savior.
 * Operates reliably across the entire app (HomeScreen, SettingsScreen, Savior Browser,
 * Floating Orb, and standalone services), whether or not AccessibilityService is connected.
 */
class AppSystemActionController(private val context: Context) : SystemActionController {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val preferences = QuickCirclePreferences.getInstance(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun performAction(
        action: QuickActionType,
        callback: (current: Int, max: Int, isMuted: Boolean) -> Unit
    ) {
        val a11y = QuickCircleAccessibilityService.instance
        if (a11y != null) {
            a11y.performAction(action, callback)
            return
        }

        // Standalone fallback when accessibility service is not connected
        when (action) {
            QuickActionType.VOLUME -> {
                val info = getVolumeInfo()
                callback(info.first, info.second, info.third)
                showVolumeHud()
            }
            QuickActionType.VOLUME_UP -> {
                adjustVolume(AudioManager.ADJUST_RAISE, callback)
            }
            QuickActionType.VOLUME_DOWN -> {
                adjustVolume(AudioManager.ADJUST_LOWER, callback)
            }
            QuickActionType.VOLUME_MUTE -> {
                toggleMute(callback)
            }
            QuickActionType.BACKGROUND_PLAY -> {
                showMediaHud()
            }
            QuickActionType.MEDIA_PLAY_PAUSE -> {
                val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                val up = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                audioManager.dispatchMediaKeyEvent(down)
                audioManager.dispatchMediaKeyEvent(up)
            }
            QuickActionType.LIVE_SCREENSHOT,
            QuickActionType.LIVE_SS_1 -> {
                com.example.livess.LiveSsPermissionActivity.launchForScreenshot(context)
            }
            QuickActionType.SCREEN_RECORDER,
            QuickActionType.LIVE_SS_2 -> {
                com.example.livess.LiveSsPermissionActivity.launchForRecording(context)
            }
            QuickActionType.SCREENSHOT,
            QuickActionType.LOCK_SCREEN,
            QuickActionType.HOME,
            QuickActionType.RECENTS,
            QuickActionType.NOTIFICATIONS,
            QuickActionType.QUICK_SETTINGS,
            QuickActionType.SPLIT_SCREEN -> {
                mainHandler.post {
                    Toast.makeText(
                        context,
                        "Enable Savior in Accessibility Settings for hardware/system gestures",
                        Toast.LENGTH_LONG
                    ).show()
                }
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun adjustVolume(
        direction: Int,
        callback: (current: Int, max: Int, isMuted: Boolean) -> Unit = { _, _, _ -> }
    ) {
        val stream = preferences.defaultStreamType.value.streamId
        audioManager.adjustStreamVolume(
            stream,
            direction,
            AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
        )
        val info = getVolumeInfo()
        callback(info.first, info.second, info.third)
    }

    override fun getVolumeInfo(): Triple<Int, Int, Boolean> {
        val stream = preferences.defaultStreamType.value.streamId
        return getStreamVolumeInfo(stream)
    }

    fun getStreamVolumeInfo(streamId: Int): Triple<Int, Int, Boolean> {
        val current = audioManager.getStreamVolume(streamId)
        val max = audioManager.getStreamMaxVolume(streamId)
        val isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.isStreamMute(streamId) || current == 0
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
        setStreamVolume(stream, newVol, playSound, callback)
    }

    fun setStreamVolume(
        streamId: Int,
        newVol: Int,
        playSound: Boolean = true,
        callback: (current: Int, max: Int, isMuted: Boolean) -> Unit = { _, _, _ -> }
    ) {
        val flags = AudioManager.FLAG_SHOW_UI or (if (playSound) AudioManager.FLAG_PLAY_SOUND else 0)
        audioManager.setStreamVolume(streamId, newVol, flags)
        val info = getStreamVolumeInfo(streamId)
        callback(info.first, info.second, info.third)
    }

    override fun toggleMute(callback: (current: Int, max: Int, isMuted: Boolean) -> Unit) {
        val stream = preferences.defaultStreamType.value.streamId
        toggleStreamMute(stream, callback)
    }

    fun toggleStreamMute(
        streamId: Int,
        callback: (current: Int, max: Int, isMuted: Boolean) -> Unit = { _, _, _ -> }
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val isMuted = audioManager.isStreamMute(streamId)
            val dir = if (isMuted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE
            audioManager.adjustStreamVolume(
                streamId,
                dir,
                AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
            )
        } else {
            val current = audioManager.getStreamVolume(streamId)
            if (current > 0) {
                audioManager.setStreamVolume(streamId, 0, AudioManager.FLAG_SHOW_UI)
            } else {
                val defaultVol = (audioManager.getStreamMaxVolume(streamId) / 2).coerceAtLeast(1)
                audioManager.setStreamVolume(streamId, defaultVol, AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND)
            }
        }
        val info = getStreamVolumeInfo(streamId)
        callback(info.first, info.second, info.third)
    }

    fun showVolumeHud() {
        val bm = FloatingBubbleHost.getBubbleManager(context)
        val info = getVolumeInfo()
        bm?.showVolumePanel(info.first, info.second, info.third)
    }

    fun showMediaHud() {
        val bm = FloatingBubbleHost.getBubbleManager(context)
        if (bm != null) {
            bm.showMediaPanel()
        } else {
            com.example.browser.SaviorBrowserActivity.launch(context)
        }
    }
}
