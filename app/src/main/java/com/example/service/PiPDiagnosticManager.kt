package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.example.data.QuickCirclePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

enum class PiPResult(val displayLabel: String) {
    PIP_OBSERVED("PIP_OBSERVED"),
    NO_PIP_OBSERVED("NO_PIP_OBSERVED"),
    UNKNOWN("UNKNOWN")
}

data class PiPAttemptRecord(
    val attemptNumber: Int,
    val timestamp: String,
    val youtubeStateBefore: String,
    val videoTitle: String,
    val homeDispatched: Boolean,
    val pipObserved: Boolean,
    val timingMs: Long,
    val finalResult: PiPResult,
    val rawTimeline: String
)

object PiPDiagnosticManager {
    const val TAG = "SaviorPiPDiag"
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val attemptCounter = AtomicInteger(0)

    private val _attemptHistory = MutableStateFlow<List<PiPAttemptRecord>>(emptyList())
    val attemptHistory: StateFlow<List<PiPAttemptRecord>> = _attemptHistory.asStateFlow()

    private val _currentTimelineLog = MutableStateFlow("")
    val currentTimelineLog: StateFlow<String> = _currentTimelineLog.asStateFlow()

    private val _isTestRunning = MutableStateFlow(false)
    val isTestRunning: StateFlow<Boolean> = _isTestRunning.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(0)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    private val _lastPipEventTimestamp = MutableStateFlow<Long?>(null)
    private val _lastPipEventDetails = MutableStateFlow<String?>(null)

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val isPipChange = (event.windowChanges and AccessibilityEvent.WINDOWS_CHANGE_PIP) != 0
                if (isPipChange) {
                    val pkg = event.packageName?.toString() ?: "unknown"
                    val time = System.currentTimeMillis()
                    _lastPipEventTimestamp.value = time
                    val details = "WINDOWS_CHANGE_PIP fired from pkg=$pkg at ${timeFormat.format(Date(time))}"
                    _lastPipEventDetails.value = details
                    Log.i(TAG, "[A11Y_EVENT] $details")
                }
            }
        }
    }

    /**
     * Executes a single diagnostic PiP test attempt with full timeline logging.
     * Can be triggered from UI button (with optional countdown) or floating bubble.
     */
    fun runDiagnosticAttempt(
        context: Context,
        triggerSource: String = "MANUAL_TEST",
        settleDelayBeforeHomeMs: Long = 0L,
        onComplete: (PiPAttemptRecord) -> Unit = {}
    ) {
        if (_isTestRunning.value) {
            Log.w(TAG, "Test already in progress, skipping")
            return
        }

        _isTestRunning.value = true
        val attemptNum = attemptCounter.incrementAndGet()
        val timelineBuilder = StringBuilder()

        fun logLine(line: String) {
            timelineBuilder.appendLine(line)
            _currentTimelineLog.value = timelineBuilder.toString()
            Log.i(TAG, line)
        }

        val startTime = System.currentTimeMillis()
        val startDateStr = timeFormat.format(Date(startTime))

        logLine("=== BG_PLAY_START (Attempt #$attemptNum | Source: $triggerSource) ===")
        logLine("Timestamp: $startDateStr")

        // 1. Accessibility Service availability check
        val a11yService = QuickCircleAccessibilityService.instance
        val a11yAvailable = a11yService != null && QuickCircleAccessibilityService.isServiceRunning
        logLine("Accessibility available = $a11yAvailable")

        // 2. YouTube Detection & MediaSession pre-check
        val controllers = QuickCircleNotificationListenerService.getActiveControllers(context)
        val ytController = controllers.firstOrNull { it.packageName == YOUTUBE_PACKAGE }
        val youtubeDetected = ytController != null
        logLine("YouTube detected = $youtubeDetected")
        logLine("YouTube package = $YOUTUBE_PACKAGE")

        val stateCodeBefore = ytController?.playbackState?.state ?: PlaybackState.STATE_NONE
        val stateStrBefore = playbackStateToString(stateCodeBefore)
        val titleBefore = ytController?.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "No Title"
        val artistBefore = ytController?.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"

        logLine("YouTube session before Home = ${if (youtubeDetected) "\"$titleBefore\" by $artistBefore" else "None"}")
        logLine("YouTube playback state before Home = $stateStrBefore ($stateCodeBefore)")

        // 3. Check Window state before Home
        val preHomeWindowObs = inspectWindows(a11yService)
        logLine("Window state before Home = $preHomeWindowObs")

        // Reset PiP event listener state
        _lastPipEventTimestamp.value = null
        _lastPipEventDetails.value = null

        val executeHome = {
            val reqTime = System.currentTimeMillis()
            val reqTimeStr = timeFormat.format(Date(reqTime))
            logLine("HOME_REQUESTED: timestamp=$reqTimeStr")

            val homeDispatched = if (a11yService != null) {
                try {
                    a11yService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                } catch (e: Exception) {
                    Log.e(TAG, "Error invoking GLOBAL_ACTION_HOME", e)
                    false
                }
            } else {
                false
            }

            logLine("HOME_RETURN_VALUE = $homeDispatched")
            logLine("GLOBAL_ACTION_HOME dispatched = $homeDispatched")

            if (!homeDispatched) {
                logLine("BG_PLAY_RESULT = UNKNOWN (Home action failed to dispatch)")
                val record = PiPAttemptRecord(
                    attemptNumber = attemptNum,
                    timestamp = startDateStr,
                    youtubeStateBefore = stateStrBefore,
                    videoTitle = titleBefore,
                    homeDispatched = false,
                    pipObserved = false,
                    timingMs = System.currentTimeMillis() - startTime,
                    finalResult = PiPResult.UNKNOWN,
                    rawTimeline = timelineBuilder.toString()
                )
                saveRecord(record)
                _isTestRunning.value = false
                onComplete(record)
            } else {
                // Sample immediately after Home
                mainHandler.postDelayed({
                    val winStateAfter = inspectWindows(a11yService)
                    logLine("YouTube activity/window state after Home = $winStateAfter")
                }, 100L)

                // Sample at +500ms
                mainHandler.postDelayed({
                    val s500 = sampleSessionState(context)
                    logLine("YouTube session after 500ms = $s500")
                }, 500L)

                // Sample at +1000ms
                mainHandler.postDelayed({
                    val s1000 = sampleSessionState(context)
                    val w1000 = inspectWindows(a11yService)
                    logLine("YouTube session after 1000ms = $s1000 (Windows: $w1000)")
                }, 1000L)

                // Sample at +2000ms
                mainHandler.postDelayed({
                    val s2000 = sampleSessionState(context)
                    logLine("YouTube session after 2000ms = $s2000")
                }, 2000L)

                // Sample at +3000ms & compute Final Verdict
                mainHandler.postDelayed({
                    val s3000 = sampleSessionState(context)
                    logLine("YouTube session after 3000ms = $s3000")

                    // PiP-related lifecycle / state observation
                    val finalWindowObs = inspectWindows(a11yService)
                    val pipEventFired = _lastPipEventTimestamp.value != null
                    val pipEventStr = _lastPipEventDetails.value ?: "None recorded"
                    logLine("PiP-related lifecycle/state observation = $finalWindowObs | Event: $pipEventStr")

                    // Compute outcome
                    val isPipExplicit = finalWindowObs.isPiPMode
                    val isPipLikelyBounds = finalWindowObs.youtubeWindowFound && finalWindowObs.isFloatingSmallWindow
                    val pipObserved = isPipExplicit || (pipEventFired && s3000.contains("STATE_PLAYING")) || isPipLikelyBounds

                    val verdict = when {
                        pipObserved -> PiPResult.PIP_OBSERVED
                        !youtubeDetected -> PiPResult.UNKNOWN
                        else -> PiPResult.NO_PIP_OBSERVED
                    }

                    logLine("BG_PLAY_RESULT = ${verdict.displayLabel}")
                    logLine("=== END ATTEMPT #$attemptNum ===")

                    val record = PiPAttemptRecord(
                        attemptNumber = attemptNum,
                        timestamp = startDateStr,
                        youtubeStateBefore = stateStrBefore,
                        videoTitle = titleBefore,
                        homeDispatched = homeDispatched,
                        pipObserved = pipObserved,
                        timingMs = System.currentTimeMillis() - startTime,
                        finalResult = verdict,
                        rawTimeline = timelineBuilder.toString()
                    )
                    saveRecord(record)
                    _isTestRunning.value = false
                    onComplete(record)
                }, 3000L)
            }
        }

        if (settleDelayBeforeHomeMs > 0) {
            logLine("Applying settle delay before Home: ${settleDelayBeforeHomeMs}ms")
            mainHandler.postDelayed({ executeHome() }, settleDelayBeforeHomeMs)
        } else {
            executeHome()
        }
    }

    /**
     * Starts a test with a 3-second countdown so the tester can switch to YouTube
     * if triggering from the app screen.
     */
    fun startTestWithCountdown(context: Context, onComplete: (PiPAttemptRecord) -> Unit = {}) {
        if (_isTestRunning.value) return
        _isTestRunning.value = true
        _countdownSeconds.value = 3

        fun tick(sec: Int) {
            if (sec > 0) {
                _countdownSeconds.value = sec
                mainHandler.postDelayed({ tick(sec - 1) }, 1000L)
            } else {
                _countdownSeconds.value = 0
                _isTestRunning.value = false
                runDiagnosticAttempt(context, triggerSource = "IN_APP_COUNTDOWN_TEST", onComplete = onComplete)
            }
        }
        tick(3)
    }

    data class WindowInspectionResult(
        val youtubeWindowFound: Boolean = false,
        val isPiPMode: Boolean = false,
        val isFloatingSmallWindow: Boolean = false,
        val details: String = "No windows retrieved"
    )

    private fun inspectWindows(service: AccessibilityService?): WindowInspectionResult {
        if (service == null) return WindowInspectionResult(details = "AccessibilityService null")

        return try {
            val windows = service.windows
            if (windows.isNullOrEmpty()) {
                return WindowInspectionResult(details = "Windows list empty (flagRetrieveInteractiveWindows active?)")
            }

            var ytFound = false
            var isPip = false
            var isFloating = false
            val detailsList = mutableListOf<String>()

            for (w in windows) {
                val rootNode = try { w.root } catch (e: Exception) { null }
                val pkg = rootNode?.packageName?.toString() ?: ""
                val outBounds = Rect()
                w.getBoundsInScreen(outBounds)

                val isYt = pkg == YOUTUBE_PACKAGE
                if (isYt) {
                    ytFound = true
                }

                var pipFlag = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (w.isInPictureInPictureMode) {
                        pipFlag = true
                        isPip = true
                    }
                }

                // Check bounds: PiP window is typically small (width <= 800px, height <= 600px)
                val isSmall = outBounds.width() in 100..900 && outBounds.height() in 100..700
                if (isYt && isSmall) {
                    isFloating = true
                }

                if (isYt || pipFlag || isSmall) {
                    detailsList.add("Window[pkg=$pkg, pip=$pipFlag, bounds=${outBounds.width()}x${outBounds.height()}, focus=${w.isFocused}]")
                }
            }

            WindowInspectionResult(
                youtubeWindowFound = ytFound,
                isPiPMode = isPip,
                isFloatingSmallWindow = isFloating,
                details = if (detailsList.isNotEmpty()) detailsList.joinToString("; ") else "Active windows: ${windows.size}"
            )
        } catch (e: Exception) {
            WindowInspectionResult(details = "Inspection error: ${e.message}")
        }
    }

    private fun sampleSessionState(context: Context): String {
        val controllers = QuickCircleNotificationListenerService.getActiveControllers(context)
        val yt = controllers.firstOrNull { it.packageName == YOUTUBE_PACKAGE }
        if (yt == null) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val musicActive = audioManager?.isMusicActive ?: false
            return "YouTube session not found (System audio active: $musicActive)"
        }

        val stateCode = yt.playbackState?.state ?: PlaybackState.STATE_NONE
        val stateStr = playbackStateToString(stateCode)
        return "State=$stateStr ($stateCode), pos=${yt.playbackState?.position ?: -1}ms"
    }

    private fun saveRecord(record: PiPAttemptRecord) {
        val current = _attemptHistory.value.toMutableList()
        current.add(0, record) // newest first
        _attemptHistory.value = current
    }

    fun clearHistory() {
        _attemptHistory.value = emptyList()
        _currentTimelineLog.value = ""
        attemptCounter.set(0)
    }

    fun playbackStateToString(state: Int): String {
        return when (state) {
            PlaybackState.STATE_NONE -> "STATE_NONE"
            PlaybackState.STATE_STOPPED -> "STATE_STOPPED"
            PlaybackState.STATE_PAUSED -> "STATE_PAUSED"
            PlaybackState.STATE_PLAYING -> "STATE_PLAYING"
            PlaybackState.STATE_FAST_FORWARDING -> "STATE_FAST_FORWARDING"
            PlaybackState.STATE_REWINDING -> "STATE_REWINDING"
            PlaybackState.STATE_BUFFERING -> "STATE_BUFFERING"
            PlaybackState.STATE_ERROR -> "STATE_ERROR"
            PlaybackState.STATE_CONNECTING -> "STATE_CONNECTING"
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS -> "STATE_SKIPPING_TO_PREV"
            PlaybackState.STATE_SKIPPING_TO_NEXT -> "STATE_SKIPPING_TO_NEXT"
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> "STATE_SKIPPING_TO_QUEUE"
            else -> "STATE_UNKNOWN($state)"
        }
    }
}
