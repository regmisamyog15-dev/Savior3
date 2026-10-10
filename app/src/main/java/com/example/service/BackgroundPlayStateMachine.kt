package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.QuickCirclePreferences
import com.example.data.ResumeMethod
import com.example.receiver.BackgroundPlayBroadcastReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

enum class BackgroundPlayState(val label: String) {
    IDLE("Idle"),
    YOUTUBE_CHECK("Verifying YouTube Session"),
    HOME_ATTEMPTED("Attempting Home Navigation for PiP"),
    OUTCOME_VERIFIED("Session Maintained in Background/PiP"),
    PASSIVE_MONITOR("Monitoring Active Playback"),
    PAUSE_ANALYSIS("Playback Interrupted / Paused"),
    CANDIDATE_DISPATCH("Dispatching Selected Candidate"),
    VERIFICATION("Verifying Playback State"),
    CONFIRMED("Playback Confirmed (RESUME_CONFIRMED)"),
    FAILED("Resume Unconfirmed (Remained Paused)"),
    UNAVAILABLE("YouTube Session Not Found")
}

object BackgroundPlayStateMachine {
    private const val TAG = "BgPlayStateMachine"
    private const val CHANNEL_ID = "savior_bg_play"
    private const val NOTIFICATION_ID = 2048

    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessionGeneration = AtomicInteger(0)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val _currentState = MutableStateFlow(BackgroundPlayState.IDLE)
    val currentState: StateFlow<BackgroundPlayState> = _currentState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var isMonitoringActive = false

    const val YOUTUBE_PACKAGE = "com.google.android.youtube"

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

    private fun updateState(state: BackgroundPlayState, message: String) {
        _currentState.value = state
        _statusMessage.value = message
        Log.d(TAG, "[STATE] ${state.name}: $message")
    }

    /**
     * User taps Background Play.
     * Navigates Home immediately so Android places the active video into Picture-in-Picture mode!
     */
    fun startBackgroundPlay(
        context: Context,
        performHomeNavigation: () -> Boolean,
        preferences: QuickCirclePreferences
    ) {
        val myGen = sessionGeneration.incrementAndGet()

        // 1. Trigger Home Navigation immediately so Android automatically transitions
        // the active video player (YouTube, etc.) into Picture-in-Picture mode!
        updateState(
            BackgroundPlayState.HOME_ATTEMPTED,
            "Navigating Home to enter Picture-in-Picture..."
        )

        val homeSuccess = performHomeNavigation()
        if (!homeSuccess) {
            updateState(BackgroundPlayState.FAILED, "Could not trigger Home navigation.")
            return
        }

        isMonitoringActive = true

        // 2. Verify outcome 600ms later once Home transition settles and PiP starts
        mainHandler.postDelayed({
            if (sessionGeneration.get() != myGen) return@postDelayed
            verifyHomeTransitionOutcome(context, preferences, myGen)
        }, 600L)
    }

    private fun verifyHomeTransitionOutcome(
        context: Context,
        preferences: QuickCirclePreferences,
        generation: Int
    ) {
        val controllers = QuickCircleNotificationListenerService.getActiveControllers(context)
        val ytController = controllers.firstOrNull { it.packageName == YOUTUBE_PACKAGE }
            ?: controllers.firstOrNull {
                val s = it.playbackState?.state ?: PlaybackState.STATE_NONE
                s == PlaybackState.STATE_PLAYING || s == PlaybackState.STATE_BUFFERING
            }

        if (ytController == null) {
            updateState(
                BackgroundPlayState.OUTCOME_VERIFIED,
                "Entered Picture-in-Picture. Drag the video box anywhere on your screen."
            )
            isMonitoringActive = false
            return
        }

        val state = ytController.playbackState?.state ?: PlaybackState.STATE_NONE
        if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
            updateState(
                BackgroundPlayState.OUTCOME_VERIFIED,
                "YouTube running in background/PiP. Drag PiP to edge to stash."
            )
            beginPassiveMonitoring(context, preferences, generation)
        } else if (state == PlaybackState.STATE_PAUSED) {
            updateState(
                BackgroundPlayState.PAUSE_ANALYSIS,
                "YouTube paused immediately on Home transition."
            )
            handlePlaybackPause(context, ytController, preferences, generation)
        } else {
            updateState(
                BackgroundPlayState.OUTCOME_VERIFIED,
                "YouTube session active: ${playbackStateToString(state)}"
            )
            beginPassiveMonitoring(context, preferences, generation)
        }
    }

    private fun beginPassiveMonitoring(
        context: Context,
        preferences: QuickCirclePreferences,
        generation: Int
    ) {
        updateState(BackgroundPlayState.PASSIVE_MONITOR, "Monitoring playback for unexpected pauses...")

        fun scheduleNextWatchdog() {
            mainHandler.postDelayed({
                if (sessionGeneration.get() != generation || !isMonitoringActive) return@postDelayed

                val controllers = QuickCircleNotificationListenerService.getActiveControllers(context)
                val yt = controllers.firstOrNull { it.packageName == YOUTUBE_PACKAGE }

                if (yt == null) {
                    updateState(BackgroundPlayState.IDLE, "YouTube closed.")
                    isMonitoringActive = false
                    return@postDelayed
                }

                val curState = yt.playbackState?.state ?: PlaybackState.STATE_NONE
                if (curState == PlaybackState.STATE_PAUSED && _currentState.value == BackgroundPlayState.PASSIVE_MONITOR) {
                    handlePlaybackPause(context, yt, preferences, generation)
                } else {
                    scheduleNextWatchdog()
                }
            }, 1500L)
        }

        scheduleNextWatchdog()
    }

    private fun handlePlaybackPause(
        context: Context,
        target: MediaController,
        preferences: QuickCirclePreferences,
        generation: Int
    ) {
        updateState(BackgroundPlayState.PAUSE_ANALYSIS, "YouTube playback paused in background.")

        val selectedMethod = preferences.bgPlayResumeMethod.value

        if (selectedMethod == ResumeMethod.NONE) {
            updateState(BackgroundPlayState.FAILED, "No candidate proven yet. Notification fallback posted.")
            postResumeNotification(context)
            return
        }

        updateState(
            BackgroundPlayState.CANDIDATE_DISPATCH,
            "Dispatching ${selectedMethod.title}..."
        )
        dispatchSingleCandidate(context, target, selectedMethod)

        updateState(BackgroundPlayState.VERIFICATION, "Waiting 2.5s for session confirmation...")
        mainHandler.postDelayed({
            if (sessionGeneration.get() != generation) return@postDelayed
            verifyPlaybackResumed(context, target, selectedMethod, generation)
        }, 2500L)
    }

    private fun dispatchSingleCandidate(
        context: Context,
        target: MediaController,
        method: ResumeMethod
    ) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            when (method) {
                ResumeMethod.AUDIO_MANAGER_PLAY -> {
                    val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY)
                    val up = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY)
                    audioManager?.dispatchMediaKeyEvent(down)
                    audioManager?.dispatchMediaKeyEvent(up)
                }
                ResumeMethod.MEDIA_CONTROLLER_PLAY -> {
                    target.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
                    target.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
                }
                ResumeMethod.TRANSPORT_CONTROLS_PLAY -> {
                    target.transportControls?.play()
                }
                ResumeMethod.NONE -> {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "dispatchSingleCandidate error", e)
        }
    }

    private fun verifyPlaybackResumed(
        context: Context,
        target: MediaController,
        method: ResumeMethod,
        generation: Int
    ) {
        val controllers = QuickCircleNotificationListenerService.getActiveControllers(context)
        val refreshed = controllers.firstOrNull { it.packageName == YOUTUBE_PACKAGE }
        val finalState = refreshed?.playbackState?.state ?: PlaybackState.STATE_NONE

        if (finalState == PlaybackState.STATE_PLAYING) {
            updateState(BackgroundPlayState.CONFIRMED, "RESUME_CONFIRMED: Session shifted to STATE_PLAYING.")
            dismissNotification(context)
            beginPassiveMonitoring(context, QuickCirclePreferences.getInstance(context), generation)
        } else {
            val stateStr = playbackStateToString(finalState)
            updateState(
                BackgroundPlayState.FAILED,
                "Resume failed: YouTube remained in $stateStr. Posting notification fallback."
            )
            postResumeNotification(context)
        }
    }

    fun onUserRequestedResume(context: Context) {
        val preferences = QuickCirclePreferences.getInstance(context)
        val controllers = QuickCircleNotificationListenerService.getActiveControllers(context)
        val ytController = controllers.firstOrNull { it.packageName == YOUTUBE_PACKAGE }

        if (ytController == null) {
            updateState(BackgroundPlayState.UNAVAILABLE, "Cannot resume: YouTube session not found.")
            return
        }

        val myGen = sessionGeneration.incrementAndGet()
        val methodToUse = if (preferences.bgPlayResumeMethod.value != ResumeMethod.NONE) {
            preferences.bgPlayResumeMethod.value
        } else {
            ResumeMethod.AUDIO_MANAGER_PLAY
        }

        updateState(BackgroundPlayState.CANDIDATE_DISPATCH, "User tapped Resume. Dispatching ${methodToUse.title}...")
        dispatchSingleCandidate(context, ytController, methodToUse)

        updateState(BackgroundPlayState.VERIFICATION, "Verifying playback after user resume...")
        mainHandler.postDelayed({
            if (sessionGeneration.get() != myGen) return@postDelayed
            val postControllers = QuickCircleNotificationListenerService.getActiveControllers(context)
            val postTarget = postControllers.firstOrNull { it.packageName == YOUTUBE_PACKAGE }
            val state = postTarget?.playbackState?.state ?: PlaybackState.STATE_NONE
            val stateStr = playbackStateToString(state)

            if (state == PlaybackState.STATE_PLAYING) {
                updateState(BackgroundPlayState.CONFIRMED, "RESUME_CONFIRMED via Notification Action.")
                dismissNotification(context)
                beginPassiveMonitoring(context, preferences, myGen)
            } else {
                updateState(
                    BackgroundPlayState.FAILED,
                    "Session did not report playing (State: $stateStr)."
                )
            }
        }, 2500L)
    }

    private fun postResumeNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Savior Background Play",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls when YouTube background playback pauses"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val resumeIntent = Intent(context, BackgroundPlayBroadcastReceiver::class.java).apply {
            action = BackgroundPlayBroadcastReceiver.ACTION_RESUME_YOUTUBE
        }
        val resumePendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_action_background_play)
            .setContentTitle("Savior Background Play")
            .setContentText("YouTube playback paused")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_action_media_play_pause, "Resume", resumePendingIntent)

        try {
            nm.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing: ${e.message}")
        }
    }

    fun dismissNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIFICATION_ID)
    }

    fun resetState() {
        sessionGeneration.incrementAndGet()
        isMonitoringActive = false
        updateState(BackgroundPlayState.IDLE, "Ready")
    }
}
