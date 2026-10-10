package com.example.livess

import android.net.Uri

enum class LiveSsMode(val label: String, val description: String) {
    LIVE_SCREENSHOT("Live Screenshot", "Clean instant screen capture with editor"),
    SCREEN_RECORDING("Screen Recording", "High-quality MP4 screen recording"),
    LIVE_SS_1("Live Screenshot", "Clean screen capture"),
    LIVE_SS_2("Screen Recording", "Screen recording")
}

data class LiveSsItem(
    val id: String,
    val title: String,
    val stillUri: Uri,
    val videoUri: Uri? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val durationSeconds: Float = 0f,
    val mode: LiveSsMode = LiveSsMode.LIVE_SCREENSHOT,
    val width: Int = 1080,
    val height: Int = 1920
) {
    val isVideo: Boolean
        get() = videoUri != null && (mode == LiveSsMode.SCREEN_RECORDING || mode == LiveSsMode.LIVE_SS_2 || durationSeconds > 0f)
}
