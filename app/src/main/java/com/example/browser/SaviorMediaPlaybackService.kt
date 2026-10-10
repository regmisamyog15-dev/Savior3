package com.example.browser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R

/**
 * Foreground Service dedicated to keeping audio and media playback active in the background
 * when the user navigates away from Savior Browser to the Home Screen or other apps.
 */
class SaviorMediaPlaybackService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.d(TAG, "onStartCommand: $action")

        when (action) {
            ACTION_START, ACTION_UPDATE -> {
                val isPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, true) ?: true
                val notification = buildNotification(isPlaying)
                startWithNotification(notification)
            }
            ACTION_PLAY -> {
                SaviorMediaManager.onPlayRequested?.invoke()
                updateNotification(true)
            }
            ACTION_PAUSE -> {
                SaviorMediaManager.onPauseRequested?.invoke()
                updateNotification(false)
            }
            ACTION_NEXT -> {
                SaviorMediaManager.onNextRequested?.invoke()
            }
            ACTION_PREV -> {
                SaviorMediaManager.onPreviousRequested?.invoke()
            }
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                wakeLock = powerManager?.newWakeLock(
                    android.os.PowerManager.PARTIAL_WAKE_LOCK,
                    "Savior:MediaPlaybackWakeLock"
                )
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(3600000L) // 1 hour max safeguard
            }
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock acquire failed", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock release failed", e)
        }
    }

    private fun startWithNotification(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isRunning = true
            acquireWakeLock()
        } catch (e: Exception) {
            Log.e(TAG, "Failed startForeground: ${e.message}", e)
        }
    }

    fun updateNotification(isPlaying: Boolean) {
        if (isPlaying) {
            acquireWakeLock()
        } else {
            releaseWakeLock()
        }
        val notification = buildNotification(isPlaying)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundService() {
        isRunning = false
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        instance = null
        isRunning = false
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val launchIntent = Intent(this, SaviorBrowserActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous Action
        val prevIntent = Intent(this, SaviorMediaPlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPending = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Play/Pause Action
        val playPauseAction = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val playPauseIntent = Intent(this, SaviorMediaPlaybackService::class.java).apply { action = playPauseAction }
        val playPausePending = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Next Action
        val nextIntent = Intent(this, SaviorMediaPlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPending = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Stop Action
        val stopIntent = Intent(this, SaviorMediaPlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val title = SaviorMediaManager.mediaTitle.value
        val source = SaviorMediaManager.mediaSource.value
        val playPauseIcon = if (isPlaying) R.drawable.ic_action_media_play_pause else R.drawable.ic_action_background_play

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_action_background_play)
            .setContentTitle(if (title.isNotBlank()) title else "Savior Media Playing")
            .setContentText(if (source.isNotBlank()) source else "Savior Background Play")
            .setSubText("Background Play Active")
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(R.drawable.ic_action_skip_previous, "Prev", prevPending)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPausePending)
            .addAction(R.drawable.ic_action_skip_next, "Next", nextPending)
            .addAction(R.drawable.ic_action_close, "Stop", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Savior Background Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps audio and video streams playing when Savior is in the background"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "SaviorMediaService"
        const val CHANNEL_ID = "savior_browser_playback"
        const val NOTIFICATION_ID = 5001

        const val ACTION_START = "com.example.action.MEDIA_SERVICE_START"
        const val ACTION_UPDATE = "com.example.action.MEDIA_SERVICE_UPDATE"
        const val ACTION_PLAY = "com.example.action.MEDIA_SERVICE_PLAY"
        const val ACTION_PAUSE = "com.example.action.MEDIA_SERVICE_PAUSE"
        const val ACTION_NEXT = "com.example.action.MEDIA_SERVICE_NEXT"
        const val ACTION_PREV = "com.example.action.MEDIA_SERVICE_PREV"
        const val ACTION_STOP = "com.example.action.MEDIA_SERVICE_STOP"

        const val EXTRA_IS_PLAYING = "extra_is_playing"

        var instance: SaviorMediaPlaybackService? = null
            private set
        var isRunning: Boolean = false
            private set

        fun start(context: Context, isPlaying: Boolean) {
            val intent = Intent(context, SaviorMediaPlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to startForegroundService: ${e.message}")
            }
        }

        fun update(context: Context, isPlaying: Boolean) {
            if (!isRunning) {
                start(context, isPlaying)
                return
            }
            val intent = Intent(context, SaviorMediaPlaybackService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            if (!isRunning) return
            val intent = Intent(context, SaviorMediaPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
