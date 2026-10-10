package com.example.livess

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.recorder.ScreenRecorderService

/**
 * Android system permission mediator for on-demand screen capture & screen recording.
 * Privacy-first: Screen capture is authorized only for explicit user actions and never runs in the background.
 */
class LiveSsPermissionActivity : ComponentActivity() {

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SCREENSHOT
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            if (mode == MODE_RECORDING) {
                val serviceIntent = Intent(this, ScreenRecorderService::class.java).apply {
                    action = ScreenRecorderService.ACTION_START
                    putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(ScreenRecorderService.EXTRA_RESULT_DATA, result.data)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } else {
                val serviceIntent = Intent(this, LiveSsCaptureService::class.java).apply {
                    putExtra(LiveSsCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(LiveSsCaptureService.EXTRA_RESULT_DATA, result.data)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        } else {
            Toast.makeText(this, "Screen capture permission was not granted", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val MODE_SCREENSHOT = "mode_screenshot"
        const val MODE_RECORDING = "mode_recording"

        fun launchForScreenshot(context: Context) {
            val intent = Intent(context, LiveSsPermissionActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_SCREENSHOT)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        fun launchForRecording(context: Context) {
            val intent = Intent(context, LiveSsPermissionActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_RECORDING)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        fun launch(context: Context) {
            launchForScreenshot(context)
        }
    }
}
