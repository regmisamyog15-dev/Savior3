package com.example.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.QuickCirclePreferences
import com.example.livess.LiveSsItem
import com.example.livess.LiveSsMode
import com.example.livess.LiveSsRepository
import com.example.overlay.OverlayStateManager
import com.example.service.FloatingBubbleHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecorderService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var currentOutputFile: File? = null
    private var startTimeMillis = 0L
    private var isRecording = false
    private var isPaused = false

    private val tickerHandler = Handler(Looper.getMainLooper())
    private val tickerRunnable = object : Runnable {
        override fun run() {
            if (isRecording && !isPaused) {
                updateNotification()
                tickerHandler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_START -> {
                val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
                val data = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (resultCode != 0 && data != null && !isRecording) {
                    startRecordingSession(resultCode, data)
                } else if (!isRecording) {
                    stopSelf()
                }
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecordingSession()
        }
        return START_NOT_STICKY
    }

    private fun startRecordingSession(resultCode: Int, data: Intent) {
        serviceScope.launch {
            val prefs = QuickCirclePreferences.getInstance(this@ScreenRecorderService)
            val config = ScreenRecorderConfig(
                quality = prefs.recorderQuality.value,
                audioSource = prefs.recorderAudioSource.value,
                cleanRecording = prefs.recorderCleanCapture.value,
                controlsMode = prefs.recorderControlsMode.value
            )

            // Savior guarantees that its own overlays are removed before capture begins
            OverlayStateManager.onCaptureStarted(isRecording = true)
            if (config.cleanRecording) {
                OverlayStateManager.hideAllOverlays(this@ScreenRecorderService)
                delay(150)
            }

            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val screen = Point()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.currentWindowMetrics.bounds
                screen.x = bounds.width()
                screen.y = bounds.height()
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealSize(screen)
            }
            val dpi = resources.displayMetrics.densityDpi

            // Determine target recording resolution
            var videoWidth = screen.x
            var videoHeight = screen.y

            if (config.quality.targetHeight > 0 && screen.y > config.quality.targetHeight) {
                val aspectRatio = screen.x.toFloat() / screen.y.toFloat()
                videoHeight = config.quality.targetHeight
                videoWidth = (videoHeight * aspectRatio).toInt()
            }
            // Dimensions must be even for H.264
            if (videoWidth % 2 != 0) videoWidth -= 1
            if (videoHeight % 2 != 0) videoHeight -= 1

            val fps = config.quality.targetFps
            val bitrate = config.quality.targetBitrate

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Savior_Rec_$timestamp.mp4")
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this@ScreenRecorderService)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder = recorder

            val hasMicAudio = config.audioSource == AudioRecordingSource.MIC ||
                    config.audioSource == AudioRecordingSource.DEVICE_AND_MIC

            try {
                if (hasMicAudio) {
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (hasMicAudio) {
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    recorder.setAudioEncodingBitRate(192000)
                    recorder.setAudioSamplingRate(44100)
                }

                recorder.setVideoSize(videoWidth, videoHeight)
                recorder.setVideoFrameRate(fps)
                recorder.setVideoEncodingBitRate(bitrate)
                recorder.setOutputFile(file.absolutePath)
                recorder.prepare()
            } catch (e: Exception) {
                Log.e(TAG, "Failed preparing MediaRecorder", e)
                Toast.makeText(this@ScreenRecorderService, "Failed initializing screen recorder: ${e.message}", Toast.LENGTH_SHORT).show()
                cleanupAndStop()
                return@launch
            }

            // Start MediaProjection
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            try {
                val proj = projectionManager.getMediaProjection(resultCode, data)
                if (proj == null) {
                    cleanupAndStop()
                    return@launch
                }
                mediaProjection = proj

                proj.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        Log.i(TAG, "MediaProjection stopped externally")
                        stopRecordingSession()
                    }
                }, null)

                virtualDisplay = proj.createVirtualDisplay(
                    "savior-recording",
                    videoWidth,
                    videoHeight,
                    dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    recorder.surface,
                    null,
                    null
                )

                recorder.start()
                isRecording = true
                isPaused = false
                startTimeMillis = SystemClock.elapsedRealtime()

                // Foreground Notification
                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (hasMicAudio && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    }
                } else 0

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, buildNotification("00:00"), serviceType)
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification("00:00"))
                }

                tickerHandler.post(tickerRunnable)

                // Optional floating pill
                if (config.controlsMode == RecordingControlsMode.FLOATING_AND_NOTIFICATION) {
                    FloatingRecordingPill.show(applicationContext) {
                        stopRecordingSession()
                    }
                }

                Toast.makeText(this@ScreenRecorderService, "🔴 Recording started", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording capture", e)
                Toast.makeText(this@ScreenRecorderService, "Could not start recording session", Toast.LENGTH_SHORT).show()
                cleanupAndStop()
            }
        }
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording && !isPaused) {
            try {
                mediaRecorder?.pause()
                isPaused = true
                updateNotification()
                Toast.makeText(this, "Recording paused", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.w(TAG, "pause failed", e)
            }
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording && !isPaused) {
            try {
                mediaRecorder?.resume()
                isPaused = false
                updateNotification()
                tickerHandler.post(tickerRunnable)
                Toast.makeText(this, "Recording resumed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.w(TAG, "resume failed", e)
            }
        }
    }

    private fun stopRecordingSession() {
        if (!isRecording) {
            cleanupAndStop()
            return
        }

        tickerHandler.removeCallbacks(tickerRunnable)
        FloatingRecordingPill.dismiss()

        val elapsed = SystemClock.elapsedRealtime() - startTimeMillis
        if (elapsed < 800) {
            try {
                Thread.sleep(800 - elapsed)
            } catch (_: Exception) {}
        }
        val durationSec = ((SystemClock.elapsedRealtime() - startTimeMillis) / 1000f).coerceAtLeast(1f)

        var success = false
        try {
            mediaRecorder?.stop()
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        } finally {
            try {
                mediaRecorder?.reset()
                mediaRecorder?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error resetting MediaRecorder", e)
            }
            mediaRecorder = null
        }

        try {
            virtualDisplay?.release()
            virtualDisplay = null
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing projection", e)
        }

        isRecording = false
        isPaused = false

        // Restore Savior overlays and mark capture ended
        OverlayStateManager.restoreAllOverlays(this)
        OverlayStateManager.onCaptureEnded()

        stopForeground(true)

        val recordedFile = currentOutputFile
        if (success && recordedFile != null && recordedFile.exists() && recordedFile.length() > 0) {
            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                recordedFile
            )

            // Register with repository
            val item = LiveSsItem(
                id = "rec_${System.currentTimeMillis()}",
                title = "Screen Recording (${formatDuration(durationSec.toInt())})",
                stillUri = uri,
                videoUri = uri,
                createdAt = System.currentTimeMillis(),
                durationSeconds = durationSec,
                mode = LiveSsMode.SCREEN_RECORDING,
                width = 1080,
                height = 1920
            )
            LiveSsRepository.getInstance(this).addItem(item)

            Toast.makeText(this, "🎬 Recording saved! Opening Editor...", Toast.LENGTH_LONG).show()

            // Open Video Editor Activity
            VideoEditorActivity.launch(this, uri)
        } else {
            Toast.makeText(this, "Recording failed or was too short", Toast.LENGTH_SHORT).show()
        }

        stopSelf()
    }

    private fun cleanupAndStop() {
        tickerHandler.removeCallbacks(tickerRunnable)
        FloatingRecordingPill.dismiss()
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        try {
            virtualDisplay?.release()
            mediaProjection?.stop()
        } catch (_: Exception) {}
        virtualDisplay = null
        mediaProjection = null
        isRecording = false
        OverlayStateManager.restoreAllOverlays(this)
        OverlayStateManager.onCaptureEnded()
        stopForeground(true)
        stopSelf()
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        val elapsedSec = ((SystemClock.elapsedRealtime() - startTimeMillis) / 1000).toInt()
        val timeStr = formatDuration(elapsedSec)
        nm.notify(NOTIFICATION_ID, buildNotification(timeStr))
        FloatingRecordingPill.updateTime(timeStr)
    }

    private fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    private fun buildNotification(timeStr: String): Notification {
        val stopIntent = Intent(this, ScreenRecorderService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, ScreenRecorderService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePending = PendingIntent.getService(
            this,
            2,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPaused) "⏸ Savior Recording Paused" else "🔴 Savior — Recording $timeStr")
            .setContentText("Clean Capture Active • Tap Stop when done")
            .setSmallIcon(R.drawable.ic_action_screen_recorder)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(stopPending)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.addAction(
                if (isPaused) R.drawable.ic_action_media_play_pause else R.drawable.ic_action_pause,
                if (isPaused) "Resume" else "Pause",
                pauseResumePending
            )
        }

        builder.addAction(
            R.drawable.ic_action_stop,
            "Stop",
            stopPending
        )

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Savior Screen Recording",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Persistent notification for ongoing screen recordings"
                enableVibration(false)
                setSound(null, null)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (instance === this) {
            instance = null
        }
    }

    companion object {
        private const val TAG = "ScreenRecorder"
        private const val CHANNEL_ID = "savior_screen_recorder_channel"
        private const val NOTIFICATION_ID = 3003

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_RESUME = "action_resume"
        const val ACTION_STOP = "action_stop"

        @Volatile
        var instance: ScreenRecorderService? = null
            private set

        val isRecordingActive: Boolean get() = instance?.isRecording == true
        val isRunning: Boolean get() = isRecordingActive

        fun stop(context: Context) {
            val intent = Intent(context, ScreenRecorderService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
