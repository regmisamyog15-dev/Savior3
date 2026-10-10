package com.example.browser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BrowserMediaState(val label: String) {
    NONE("No Media"),
    PLAYING("Playing"),
    PAUSED("Paused"),
    BUFFERING("Buffering"),
    STOPPED("Stopped")
}

object SaviorMediaManager {
    private const val TAG = "SaviorMediaManager"
    private const val CHANNEL_ID = "savior_browser_playback"
    private const val NOTIFICATION_ID = 5001

    // Action Intents for media buttons
    const val ACTION_PLAY = "com.example.action.MEDIA_PLAY"
    const val ACTION_PAUSE = "com.example.action.MEDIA_PAUSE"
    const val ACTION_NEXT = "com.example.action.MEDIA_NEXT"
    const val ACTION_PREV = "com.example.action.MEDIA_PREV"

    private var appContext: Context? = null
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    // Callbacks to the active WebView controller
    var onPlayRequested: (() -> Unit)? = null
    var onPauseRequested: (() -> Unit)? = null
    var onNextRequested: (() -> Unit)? = null
    var onPreviousRequested: (() -> Unit)? = null
    var onSeekRequested: ((Long) -> Unit)? = null

    // Observable states for UI & Diagnostics
    private val _mediaState = MutableStateFlow(BrowserMediaState.NONE)
    val mediaState: StateFlow<BrowserMediaState> = _mediaState.asStateFlow()

    private val _mediaTitle = MutableStateFlow("No active media")
    val mediaTitle: StateFlow<String> = _mediaTitle.asStateFlow()

    private val _mediaSource = MutableStateFlow("Savior Browser")
    val mediaSource: StateFlow<String> = _mediaSource.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isPiPActive = MutableStateFlow(false)
    val isPiPActive: StateFlow<Boolean> = _isPiPActive.asStateFlow()

    private val _audioFocusState = MutableStateFlow("NONE")
    val audioFocusState: StateFlow<String> = _audioFocusState.asStateFlow()

    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive.asStateFlow()

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        createNotificationChannel(context)
        setupMediaSession(context)
    }

    private fun setupMediaSession(context: Context) {
        try {
            val session = MediaSession(context, "SaviorMediaSession").apply {
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        Log.d(TAG, "MediaSession.onPlay()")
                        onPlayRequested?.invoke()
                    }

                    override fun onPause() {
                        Log.d(TAG, "MediaSession.onPause()")
                        onPauseRequested?.invoke()
                    }

                    override fun onSkipToNext() {
                        Log.d(TAG, "MediaSession.onSkipToNext()")
                        onNextRequested?.invoke()
                    }

                    override fun onSkipToPrevious() {
                        Log.d(TAG, "MediaSession.onSkipToPrevious()")
                        onPreviousRequested?.invoke()
                    }

                    override fun onSeekTo(pos: Long) {
                        Log.d(TAG, "MediaSession.onSeekTo($pos)")
                        onSeekRequested?.invoke(pos)
                    }
                })
                isActive = false
            }
            mediaSession = session
            _sessionActive.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaSession", e)
        }
    }

    /**
     * Called by the HTML5 media observer inside SaviorBrowserActivity when
     * real video/audio elements in the WebView change playback state.
     */
    fun updatePlaybackState(
        context: Context,
        isPlaying: Boolean,
        isBuffering: Boolean,
        title: String,
        source: String,
        positionMs: Long,
        durationMs: Long
    ) {
        val state = when {
            isBuffering -> BrowserMediaState.BUFFERING
            isPlaying -> BrowserMediaState.PLAYING
            positionMs > 0 || durationMs > 0 -> BrowserMediaState.PAUSED
            else -> BrowserMediaState.NONE
        }

        _mediaState.value = state
        _mediaTitle.value = if (title.isNotBlank()) title else "Web Video"
        _mediaSource.value = if (source.isNotBlank()) source else "Savior Browser"
        _currentPositionMs.value = positionMs
        _durationMs.value = durationMs

        val session = mediaSession ?: return

        if (state == BrowserMediaState.NONE || state == BrowserMediaState.STOPPED) {
            session.isActive = false
            _sessionActive.value = false
            abandonAudioFocus()
            SaviorMediaPlaybackService.stop(context)
            dismissNotification(context)
            return
        }

        // Activate session
        session.isActive = true
        _sessionActive.value = true

        val playbackStateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO
            )

        val stateCode = when (state) {
            BrowserMediaState.PLAYING -> PlaybackState.STATE_PLAYING
            BrowserMediaState.PAUSED -> PlaybackState.STATE_PAUSED
            BrowserMediaState.BUFFERING -> PlaybackState.STATE_BUFFERING
            else -> PlaybackState.STATE_NONE
        }

        playbackStateBuilder.setState(stateCode, positionMs, if (isPlaying) 1.0f else 0.0f)
        session.setPlaybackState(playbackStateBuilder.build())

        // Update Metadata
        val metaBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, _mediaTitle.value)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, _mediaSource.value)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "Savior Browser")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)

        session.setMetadata(metaBuilder.build())

        if (isPlaying) {
            _audioFocusState.value = "PLAYING_WEBVIEW"
            SaviorMediaPlaybackService.start(context, true)
        } else {
            SaviorMediaPlaybackService.update(context, false)
        }
    }

    fun setPiPActive(active: Boolean) {
        _isPiPActive.value = active
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener { focusChange ->
                        handleAudioFocusChange(focusChange)
                    }
                    .build()
                focusRequest = req
                val res = am.requestAudioFocus(req)
                val granted = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                _audioFocusState.value = if (granted) "GRANTED" else "DENIED"
                granted
            } else {
                @Suppress("DEPRECATION")
                val res = am.requestAudioFocus(
                    { focusChange -> handleAudioFocusChange(focusChange) },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
                val granted = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                _audioFocusState.value = if (granted) "GRANTED" else "DENIED"
                granted
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio focus request failed", e)
            false
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
            _audioFocusState.value = "ABANDONED"
        } catch (e: Exception) {
            Log.w(TAG, "abandonAudioFocus error", e)
        }
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                _audioFocusState.value = "LOSS"
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                _audioFocusState.value = "GAINED"
            }
            else -> {
                _audioFocusState.value = "CHANGED($focusChange)"
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Savior Background Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows media controls for active browser audio and video"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun showMediaNotification(context: Context, isPlaying: Boolean) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val session = mediaSession ?: return

        val launchIntent = Intent(context, SaviorBrowserActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous Action
        val prevIntent = Intent(context, SaviorMediaActionReceiver::class.java).apply { action = ACTION_PREV }
        val prevPending = PendingIntent.getBroadcast(context, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Play/Pause Action
        val playPauseAction = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val playPauseIntent = Intent(context, SaviorMediaActionReceiver::class.java).apply { action = playPauseAction }
        val playPausePending = PendingIntent.getBroadcast(context, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Next Action
        val nextIntent = Intent(context, SaviorMediaActionReceiver::class.java).apply { action = ACTION_NEXT }
        val nextPending = PendingIntent.getBroadcast(context, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIcon = if (isPlaying) R.drawable.ic_action_media_play_pause else R.drawable.ic_action_background_play

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_action_background_play)
            .setContentTitle(_mediaTitle.value)
            .setContentText(_mediaSource.value)
            .setSubText("Savior Background Play")
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(R.drawable.ic_action_recents, "Previous", prevPending)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPausePending)
            .addAction(R.drawable.ic_action_recents, "Next", nextPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)


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

    fun stopAndReset(context: Context) {
        _mediaState.value = BrowserMediaState.NONE
        _mediaTitle.value = "No active media"
        _mediaSource.value = "Savior Browser"
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        _isPiPActive.value = false
        mediaSession?.isActive = false
        _sessionActive.value = false
        abandonAudioFocus()
        SaviorMediaPlaybackService.stop(context)
        dismissNotification(context)
    }
}
