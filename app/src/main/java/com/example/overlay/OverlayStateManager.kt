package com.example.overlay

import android.content.Context
import com.example.service.FloatingBubbleHost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CapturePrivacyState {
    NOT_ACTIVE,
    RECORDING_ACTIVE,
    CAPTURE_ENDED
}

/**
 * Centralized overlay visibility coordinator and live screen capture privacy manager.
 * Guarantees Savior overlays are detached from WindowManager before capture begins,
 * and maintains the user-facing capture privacy state.
 */
object OverlayStateManager {

    private val _privacyState = MutableStateFlow(CapturePrivacyState.NOT_ACTIVE)
    val privacyState: StateFlow<CapturePrivacyState> = _privacyState.asStateFlow()

    fun onCaptureStarted(isRecording: Boolean) {
        _privacyState.value = CapturePrivacyState.RECORDING_ACTIVE
    }

    fun onCaptureEnded() {
        _privacyState.value = CapturePrivacyState.CAPTURE_ENDED
    }

    fun setNotActive() {
        _privacyState.value = CapturePrivacyState.NOT_ACTIVE
    }

    fun hideAllOverlays(context: Context) {
        FloatingBubbleHost.hideAllOverlays(context)
    }

    fun restoreAllOverlays(context: Context) {
        FloatingBubbleHost.restoreAllOverlays(context)
    }
}
