package com.example.livess

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.overlay.FloatingBubbleManager
import com.example.overlay.OverlayStateManager
import com.example.service.FloatingBubbleHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Privacy-first single-shot screen capture service.
 * Operates purely on-demand:
 * 1. Hides all Savior overlays so Savior UI NEVER appears in the capture.
 * 2. Acquires single frame via MediaProjection.
 * 3. Immediately stops MediaProjection and VirtualDisplay to release resources.
 * 4. Restores Savior overlays.
 * 5. Presents floating preview pill with Edit and Share options.
 */
class LiveSsCaptureService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        handlerThread = HandlerThread("savior-capture").apply {
            start()
            this@LiveSsCaptureService.handler = Handler(looper)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

        if (resultCode != 0 && data != null) {
            executeSingleShotCapture(resultCode, data)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun executeSingleShotCapture(resultCode: Int, data: Intent) {
        serviceScope.launch {
            // 1. Mark capture started and hide all Savior overlays
            OverlayStateManager.onCaptureStarted(isRecording = false)
            OverlayStateManager.hideAllOverlays(this@LiveSsCaptureService)
            FloatingScreenshotPill.dismissCurrent()

            // Short pause to guarantee WindowManager has completely cleared the overlay surfaces
            delay(120)

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

            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            var projection: MediaProjection? = null
            var virtualDisplay: VirtualDisplay? = null
            var imageReader: ImageReader? = null
            var capturedBitmap: Bitmap? = null

            try {
                projection = projectionManager.getMediaProjection(resultCode, data)
                if (projection != null) {
                    val reader = ImageReader.newInstance(screen.x, screen.y, PixelFormat.RGBA_8888, 2)
                    imageReader = reader

                    projection.registerCallback(object : MediaProjection.Callback() {
                        override fun onStop() {
                            Log.i(TAG, "MediaProjection onStop")
                        }
                    }, handler)

                    virtualDisplay = projection.createVirtualDisplay(
                        "savior-screenshot",
                        screen.x,
                        screen.y,
                        dpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        reader.surface,
                        null,
                        handler
                    )

                    // Poll briefly for the single clean screen frame
                    withContext(Dispatchers.Default) {
                        for (i in 0 until 18) {
                            val img = reader.acquireLatestImage()
                            if (img != null) {
                                img.use { image ->
                                    val plane = image.planes[0]
                                    val rowStride = plane.rowStride
                                    val pixelStride = plane.pixelStride
                                    val rowPixels = rowStride / pixelStride

                                    val tmp = Bitmap.createBitmap(rowPixels, screen.y, Bitmap.Config.ARGB_8888)
                                    tmp.copyPixelsFromBuffer(plane.buffer)
                                    capturedBitmap = Bitmap.createBitmap(tmp, 0, 0, screen.x, screen.y)
                                    tmp.recycle()
                                }
                                break
                            }
                            delay(35)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during screen capture", e)
            } finally {
                // 2. IMMEDIATELY stop and release all MediaProjection resources
                try {
                    virtualDisplay?.release()
                    imageReader?.close()
                    projection?.stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Error cleaning up projection", e)
                }

                // 3. Restore Savior overlays and mark capture ended
                OverlayStateManager.restoreAllOverlays(this@LiveSsCaptureService)
                OverlayStateManager.onCaptureEnded()
            }

            // 4. Handle captured image
            if (capturedBitmap != null) {
                val bmp = capturedBitmap!!
                val file = File(filesDir, "savior_ss_${System.currentTimeMillis()}.png")
                try {
                    FileOutputStream(file).use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    val uri = FileProvider.getUriForFile(
                        this@LiveSsCaptureService,
                        "$packageName.fileprovider",
                        file
                    )

                    val item = LiveSsItem(
                        id = "ss_${System.currentTimeMillis()}",
                        title = "Live Screenshot",
                        stillUri = uri,
                        videoUri = null,
                        createdAt = System.currentTimeMillis(),
                        durationSeconds = 0f,
                        mode = LiveSsMode.LIVE_SCREENSHOT,
                        width = screen.x,
                        height = screen.y
                    )
                    LiveSsRepository.getInstance(this@LiveSsCaptureService).addItem(item)

                    // 5. Present Floating Preview Pill with Edit and Share options
                    FloatingScreenshotPill.show(applicationContext, bmp, uri)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed saving captured screenshot", e)
                    Toast.makeText(this@LiveSsCaptureService, "Failed to save screenshot", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@LiveSsCaptureService, "Could not capture screen", Toast.LENGTH_SHORT).show()
            }

            stopForeground(true)
            stopSelf()
        }
    }

    fun attachBubbleManager(bm: FloatingBubbleManager) {
        // Retained for compatibility
    }

    fun onBubbleTapped() {
        // No-op in privacy-first mode: Savior never records or buffers screen beforehand!
    }

    fun onMenuDismissed() {
        // No-op
    }

    fun triggerLiveSs1() {
        LiveSsPermissionActivity.launch(this)
    }

    fun triggerLiveSs2() {
        LiveSsPermissionActivity.launch(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Savior Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Temporary notification during single-shot screen capture"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Savior")
            .setContentText("Capturing screenshot...")
            .setSmallIcon(R.drawable.ic_action_screenshot)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        handlerThread?.quitSafely()
        if (instance === this) {
            instance = null
        }
    }

    companion object {
        private const val TAG = "LiveSsService"
        private const val CHANNEL_ID = "savior_screenshot_channel"
        private const val NOTIFICATION_ID = 2002

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        @Volatile
        var instance: LiveSsCaptureService? = null
            private set

        val isRunning: Boolean get() = instance != null
    }
}
