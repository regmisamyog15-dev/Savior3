package com.example.model

enum class BatteryMode(val title: String, val description: String) {
    NORMAL("Normal", "Floating button active, gesture detection active, smooth animations."),
    BATTERY_SAVER("Battery Saver", "Gesture detection paused, animations reduced. Bubble remains available on tap."),
    ULTRA_BATTERY("Ultra Battery", "Gesture detection disabled. Controls respond only to direct tap. Note: The accessibility service remains active as required by Android.")
}

enum class DockEdge(val title: String) {
    AUTO("Auto (Nearest Edge)"),
    LEFT("Left Edge"),
    RIGHT("Right Edge")
}

enum class StreamType(val title: String, val streamId: Int) {
    MUSIC("Media", 3),
    RING("Ringtone", 2),
    NOTIFICATION("Notification", 5),
    VOICE_CALL("Call", 0)
}
