package com.example.model

import com.example.R

enum class QuickActionType(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val defaultEnabled: Boolean = true
) {
    VOLUME("volume", "Volume", "Up, down and mute", R.drawable.ic_action_volume_up, true),
    SCREENSHOT("screenshot", "Screenshot", "One-tap screen capture", R.drawable.ic_action_screenshot, true),
    BACKGROUND_PLAY("background_play", "Background Play", "Go Home, keep media playing", R.drawable.ic_action_background_play, true),
    LOCK_SCREEN("lock_screen", "Lock Screen", "Turn the screen off", R.drawable.ic_action_lock, true),
    VOLUME_UP("volume_up", "Volume +", "Increase sound level", R.drawable.ic_action_volume_up, false),
    VOLUME_DOWN("volume_down", "Volume -", "Decrease sound level", R.drawable.ic_action_volume_down, false),
    VOLUME_MUTE("volume_mute", "Mute", "Toggle volume mute", R.drawable.ic_action_volume_mute, false),
    SPLIT_SCREEN("split_screen", "Split Screen", "Toggle app split view", R.drawable.ic_action_split_screen, false),
    RECENTS("recents", "Recent Apps", "Switch running apps", R.drawable.ic_action_recents, false),
    QUICK_SETTINGS("quick_settings", "Quick Settings", "Open system toggles", R.drawable.ic_action_quick_settings, false),
    HOME("home", "Home", "Return to home screen", R.drawable.ic_action_home, false),
    NOTIFICATIONS("notifications", "Notifications", "Expand notification shade", R.drawable.ic_action_notifications, false),
    LIVE_SCREENSHOT("live_screenshot", "Live Screenshot", "Clean capture with preview pill & editor", R.drawable.ic_action_screenshot, true),
    SCREEN_RECORDER("screen_recorder", "Screen Recorder", "High-quality screen recorder & editor", R.drawable.ic_action_screen_recorder, true),
    MEDIA_PLAY_PAUSE("media_play_pause", "Play / Pause", "Toggle music playback", R.drawable.ic_action_media_play_pause, false),
    LIVE_SS_1("live_ss_1", "Live Screenshot", "Clean single-frame capture", R.drawable.ic_action_screenshot, false),
    LIVE_SS_2("live_ss_2", "Screen Recorder", "Record screen in high quality", R.drawable.ic_action_screen_recorder, false);

    companion object {
        val CORE_ACTIONS = listOf(VOLUME, LIVE_SCREENSHOT, SCREEN_RECORDER, SCREENSHOT, BACKGROUND_PLAY, LOCK_SCREEN)
        val DEFAULT_RADIAL_ACTIONS = CORE_ACTIONS

        fun fromId(id: String): QuickActionType? = when (id) {
            "live_ss_1" -> LIVE_SCREENSHOT
            "live_ss_2" -> SCREEN_RECORDER
            else -> values().firstOrNull { it.id == id }
        }
    }
}
