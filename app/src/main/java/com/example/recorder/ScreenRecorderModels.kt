package com.example.recorder

enum class VideoQualityPreset(
    val label: String,
    val description: String,
    val targetHeight: Int,
    val targetFps: Int,
    val targetBitrate: Int
) {
    STANDARD("Standard", "1080p • 30 FPS • Balanced bitrate", 1080, 30, 8_000_000),
    HIGH("High", "1080p • 60 FPS where supported • Higher bitrate", 1080, 60, 14_000_000),
    MAXIMUM("Maximum", "Native resolution • Max supported FPS • High bitrate", 0, 60, 20_000_000)
}

enum class AudioRecordingSource(
    val label: String,
    val description: String
) {
    NONE("No Audio", "Muted screen recording"),
    DEVICE("Device Audio", "Internal app & media sound (Android 10+)"),
    MIC("Microphone", "External microphone voice recording"),
    DEVICE_AND_MIC("Device + Microphone", "Internal system audio mixed with mic")
}

enum class RecordingControlsMode(
    val label: String,
    val description: String
) {
    NOTIFICATION_ONLY("Notification only", "Cleanest capture; controls inside Android notification"),
    FLOATING_AND_NOTIFICATION("Floating controls + notification", "Small on-screen recording timer pill")
}

data class ScreenRecorderConfig(
    val quality: VideoQualityPreset = VideoQualityPreset.STANDARD,
    val audioSource: AudioRecordingSource = AudioRecordingSource.NONE,
    val cleanRecording: Boolean = true,
    val controlsMode: RecordingControlsMode = RecordingControlsMode.NOTIFICATION_ONLY
)
