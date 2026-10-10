package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.BatteryMode
import com.example.model.DockEdge
import com.example.model.QuickActionType
import com.example.model.StreamType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ResumeMethod(val title: String, val description: String) {
    NONE("None (Notification Fallback Only)", "Do not attempt automated key dispatch; post notification with [Resume]"),
    AUDIO_MANAGER_PLAY("Candidate A (AudioManager KEYCODE_MEDIA_PLAY)", "Dispatches global media key event down+up via AudioManager"),
    MEDIA_CONTROLLER_PLAY("Candidate B (MediaController Button PLAY)", "Dispatches media button event directly to YouTube's MediaSession token"),
    TRANSPORT_CONTROLS_PLAY("Candidate C (TransportControls.play())", "Direct IPC play() call to YouTube's MediaSession")
}

class QuickCirclePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("quick_circle_preferences", Context.MODE_PRIVATE)

    // State Flows for reactive Compose UI & background services
    private val _isEnabled = MutableStateFlow(prefs.getBoolean(KEY_IS_ENABLED, true))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _isPermanent = MutableStateFlow(prefs.getBoolean(KEY_IS_PERMANENT, true))
    val isPermanent: StateFlow<Boolean> = _isPermanent.asStateFlow()

    private val _bubbleSizeDp = MutableStateFlow(prefs.getInt(KEY_BUBBLE_SIZE_DP, 52))
    val bubbleSizeDp: StateFlow<Int> = _bubbleSizeDp.asStateFlow()

    private val _bubbleOpacity = MutableStateFlow(prefs.getFloat(KEY_BUBBLE_OPACITY, 0.85f))
    val bubbleOpacity: StateFlow<Float> = _bubbleOpacity.asStateFlow()

    private val _dockEdge = MutableStateFlow(
        safeEnumValueOf(prefs.getString(KEY_DOCK_EDGE, null), DockEdge.AUTO)
    )
    val dockEdge: StateFlow<DockEdge> = _dockEdge.asStateFlow()

    private val _positionX = MutableStateFlow(prefs.getInt(KEY_POSITION_X, -1))
    val positionX: StateFlow<Int> = _positionX.asStateFlow()

    private val _positionY = MutableStateFlow(prefs.getInt(KEY_POSITION_Y, -1))
    val positionY: StateFlow<Int> = _positionY.asStateFlow()

    private val _isTripleTapEnabled = MutableStateFlow(prefs.getBoolean(KEY_TRIPLE_TAP_ENABLED, true))
    val isTripleTapEnabled: StateFlow<Boolean> = _isTripleTapEnabled.asStateFlow()

    private val _tripleTapSensitivityMs = MutableStateFlow(prefs.getLong(KEY_TRIPLE_TAP_SENSITIVITY, 800L))
    val tripleTapSensitivityMs: StateFlow<Long> = _tripleTapSensitivityMs.asStateFlow()

    private val _batteryMode = MutableStateFlow(
        safeEnumValueOf(prefs.getString(KEY_BATTERY_MODE, null), BatteryMode.NORMAL)
    )
    val batteryMode: StateFlow<BatteryMode> = _batteryMode.asStateFlow()

    private val _activeActions = MutableStateFlow(loadActions())
    val activeActions: StateFlow<List<QuickActionType>> = _activeActions.asStateFlow()

    private val _isSoftwareVolumeReplacementEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SOFTWARE_VOLUME_REPLACEMENT, true)
    )
    val isSoftwareVolumeReplacementEnabled: StateFlow<Boolean> = _isSoftwareVolumeReplacementEnabled.asStateFlow()

    private val _defaultStreamType = MutableStateFlow(
        safeEnumValueOf(prefs.getString(KEY_DEFAULT_STREAM_TYPE, null), StreamType.MUSIC)
    )
    val defaultStreamType: StateFlow<StreamType> = _defaultStreamType.asStateFlow()

    private val _isBackgroundPlayEnabled = MutableStateFlow(prefs.getBoolean(KEY_BG_PLAY_ENABLED, true))
    val isBackgroundPlayEnabled: StateFlow<Boolean> = _isBackgroundPlayEnabled.asStateFlow()

    private val _resumePausedMedia = MutableStateFlow(prefs.getBoolean(KEY_RESUME_PAUSED_MEDIA, false))
    val resumePausedMedia: StateFlow<Boolean> = _resumePausedMedia.asStateFlow()

    private val _autoPipOnHome = MutableStateFlow(prefs.getBoolean(KEY_AUTO_PIP_ON_HOME, false))
    val autoPipOnHome: StateFlow<Boolean> = _autoPipOnHome.asStateFlow()

    private val _keepAudioActiveInBackground = MutableStateFlow(prefs.getBoolean(KEY_KEEP_AUDIO_ACTIVE, true))
    val keepAudioActiveInBackground: StateFlow<Boolean> = _keepAudioActiveInBackground.asStateFlow()

    private val _mediaNotificationEnabled = MutableStateFlow(prefs.getBoolean(KEY_MEDIA_NOTIFICATION_ENABLED, true))
    val mediaNotificationEnabled: StateFlow<Boolean> = _mediaNotificationEnabled.asStateFlow()

    fun setAutoPipOnHome(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PIP_ON_HOME, enabled).apply()
        _autoPipOnHome.value = enabled
    }

    fun setKeepAudioActiveInBackground(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_AUDIO_ACTIVE, enabled).apply()
        _keepAudioActiveInBackground.value = enabled
    }

    fun setMediaNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MEDIA_NOTIFICATION_ENABLED, enabled).apply()
        _mediaNotificationEnabled.value = enabled
    }

    private val _bgPlayResumeMethod = MutableStateFlow(
        safeEnumValueOf(prefs.getString(KEY_BG_PLAY_RESUME_METHOD, null), ResumeMethod.NONE)
    )
    val bgPlayResumeMethod: StateFlow<ResumeMethod> = _bgPlayResumeMethod.asStateFlow()

    fun setBgPlayResumeMethod(method: ResumeMethod) {
        prefs.edit().putString(KEY_BG_PLAY_RESUME_METHOD, method.name).apply()
        _bgPlayResumeMethod.value = method
    }

    // Screen Recorder settings
    private val _recorderQuality = MutableStateFlow(
        safeEnumValueOf(prefs.getString(KEY_RECORDER_QUALITY, null), com.example.recorder.VideoQualityPreset.STANDARD)
    )
    val recorderQuality: StateFlow<com.example.recorder.VideoQualityPreset> = _recorderQuality.asStateFlow()

    fun setRecorderQuality(preset: com.example.recorder.VideoQualityPreset) {
        prefs.edit().putString(KEY_RECORDER_QUALITY, preset.name).apply()
        _recorderQuality.value = preset
    }

    private val _recorderAudioSource = MutableStateFlow(
        safeEnumValueOf(prefs.getString(KEY_RECORDER_AUDIO_SOURCE, null), com.example.recorder.AudioRecordingSource.NONE)
    )
    val recorderAudioSource: StateFlow<com.example.recorder.AudioRecordingSource> = _recorderAudioSource.asStateFlow()

    fun setRecorderAudioSource(source: com.example.recorder.AudioRecordingSource) {
        prefs.edit().putString(KEY_RECORDER_AUDIO_SOURCE, source.name).apply()
        _recorderAudioSource.value = source
    }

    private val _recorderCleanCapture = MutableStateFlow(prefs.getBoolean(KEY_RECORDER_CLEAN_CAPTURE, true))
    val recorderCleanCapture: StateFlow<Boolean> = _recorderCleanCapture.asStateFlow()

    fun setRecorderCleanCapture(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RECORDER_CLEAN_CAPTURE, enabled).apply()
        _recorderCleanCapture.value = enabled
    }

    private val _recorderControlsMode = MutableStateFlow(
        safeEnumValueOf(prefs.getString(KEY_RECORDER_CONTROLS_MODE, null), com.example.recorder.RecordingControlsMode.NOTIFICATION_ONLY)
    )
    val recorderControlsMode: StateFlow<com.example.recorder.RecordingControlsMode> = _recorderControlsMode.asStateFlow()

    fun setRecorderControlsMode(mode: com.example.recorder.RecordingControlsMode) {
        prefs.edit().putString(KEY_RECORDER_CONTROLS_MODE, mode.name).apply()
        _recorderControlsMode.value = mode
    }

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ENABLED, enabled).apply()
        _isEnabled.value = enabled
    }

    fun setPermanent(permanent: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PERMANENT, permanent).apply()
        _isPermanent.value = permanent
    }

    fun setBubbleSizeDp(size: Int) {
        prefs.edit().putInt(KEY_BUBBLE_SIZE_DP, size).apply()
        _bubbleSizeDp.value = size
    }

    fun setBubbleOpacity(opacity: Float) {
        prefs.edit().putFloat(KEY_BUBBLE_OPACITY, opacity).apply()
        _bubbleOpacity.value = opacity
    }

    fun setDockEdge(edge: DockEdge) {
        prefs.edit().putString(KEY_DOCK_EDGE, edge.name).apply()
        _dockEdge.value = edge
    }

    fun savePosition(x: Int, y: Int) {
        prefs.edit()
            .putInt(KEY_POSITION_X, x)
            .putInt(KEY_POSITION_Y, y)
            .apply()
        _positionX.value = x
        _positionY.value = y
    }

    fun setTripleTapEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TRIPLE_TAP_ENABLED, enabled).apply()
        _isTripleTapEnabled.value = enabled
    }

    fun setTripleTapSensitivityMs(ms: Long) {
        prefs.edit().putLong(KEY_TRIPLE_TAP_SENSITIVITY, ms).apply()
        _tripleTapSensitivityMs.value = ms
    }

    fun setBatteryMode(mode: BatteryMode) {
        prefs.edit().putString(KEY_BATTERY_MODE, mode.name).apply()
        _batteryMode.value = mode
    }

    fun setActiveActions(actions: List<QuickActionType>) {
        val ids = actions.joinToString(",") { it.id }
        prefs.edit().putString(KEY_ACTIVE_ACTIONS, ids).apply()
        _activeActions.value = actions
    }

    fun setSoftwareVolumeReplacementEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOFTWARE_VOLUME_REPLACEMENT, enabled).apply()
        _isSoftwareVolumeReplacementEnabled.value = enabled
    }

    fun setDefaultStreamType(streamType: StreamType) {
        prefs.edit().putString(KEY_DEFAULT_STREAM_TYPE, streamType.name).apply()
        _defaultStreamType.value = streamType
    }

    fun setBackgroundPlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BG_PLAY_ENABLED, enabled).apply()
        _isBackgroundPlayEnabled.value = enabled
    }

    fun setResumePausedMedia(resume: Boolean) {
        prefs.edit().putBoolean(KEY_RESUME_PAUSED_MEDIA, resume).apply()
        _resumePausedMedia.value = resume
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _hasCompletedOnboarding.value = completed
    }

    private fun loadActions(): List<QuickActionType> {
        val stored = prefs.getString(KEY_ACTIVE_ACTIONS, null)
        if (stored.isNullOrBlank()) {
            return QuickActionType.DEFAULT_RADIAL_ACTIONS
        }
        val actions = stored.split(",")
            .mapNotNull { QuickActionType.fromId(it.trim()) }
        return if (actions.isEmpty()) QuickActionType.DEFAULT_RADIAL_ACTIONS else actions
    }

    private inline fun <reified T : Enum<T>> safeEnumValueOf(value: String?, default: T): T {
        if (value == null) return default
        return try {
            java.lang.Enum.valueOf(T::class.java, value)
        } catch (e: IllegalArgumentException) {
            default
        }
    }

    companion object {
        private const val KEY_IS_ENABLED = "is_enabled"
        private const val KEY_IS_PERMANENT = "is_permanent"
        private const val KEY_BUBBLE_SIZE_DP = "bubble_size_dp"
        private const val KEY_BUBBLE_OPACITY = "bubble_opacity"
        private const val KEY_DOCK_EDGE = "dock_edge"
        private const val KEY_POSITION_X = "position_x"
        private const val KEY_POSITION_Y = "position_y"
        private const val KEY_TRIPLE_TAP_ENABLED = "triple_tap_enabled"
        private const val KEY_TRIPLE_TAP_SENSITIVITY = "triple_tap_sensitivity"
        private const val KEY_BATTERY_MODE = "battery_mode"
        private const val KEY_ACTIVE_ACTIONS = "active_actions"
        private const val KEY_SOFTWARE_VOLUME_REPLACEMENT = "software_volume_replacement"
        private const val KEY_DEFAULT_STREAM_TYPE = "default_stream_type"
        private const val KEY_BG_PLAY_ENABLED = "bg_play_enabled"
        private const val KEY_RESUME_PAUSED_MEDIA = "resume_paused_media"
        private const val KEY_AUTO_PIP_ON_HOME = "auto_pip_on_home"
        private const val KEY_KEEP_AUDIO_ACTIVE = "keep_audio_active"
        private const val KEY_MEDIA_NOTIFICATION_ENABLED = "media_notification_enabled"
        private const val KEY_BG_PLAY_RESUME_METHOD = "bg_play_resume_method"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_RECORDER_QUALITY = "recorder_quality"
        private const val KEY_RECORDER_AUDIO_SOURCE = "recorder_audio_source"
        private const val KEY_RECORDER_CLEAN_CAPTURE = "recorder_clean_capture"
        private const val KEY_RECORDER_CONTROLS_MODE = "recorder_controls_mode"

        @Volatile
        private var INSTANCE: QuickCirclePreferences? = null

        fun getInstance(context: Context): QuickCirclePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: QuickCirclePreferences(context.applicationContext).also { INSTANCE = it }
            }
        }

        @androidx.annotation.VisibleForTesting
        fun resetForTesting(context: Context) {
            synchronized(this) {
                context.applicationContext
                    .getSharedPreferences("quick_circle_preferences", Context.MODE_PRIVATE)
                    .edit().clear().apply()
                INSTANCE = null
            }
        }
    }
}
