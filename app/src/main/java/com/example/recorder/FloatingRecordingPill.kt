package com.example.recorder

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.R

/**
 * Optional draggable floating pill displaying elapsed recording time and a Stop button.
 * Default is OFF (Notification Only) per user settings.
 */
@SuppressLint("ViewConstructor")
class FloatingRecordingPill(
    context: Context,
    private val onStopRequested: () -> Unit
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val timeTextView: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padH = (12 * density).toInt()
        val padV = (6 * density).toInt()
        setPadding(padH, padV, padH, padV)

        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20 * density
            setColor(Color.argb(235, 20, 22, 26))
            setStroke((1.2f * density).toInt(), Color.argb(180, 224, 108, 117)) // Red tint
        }
        elevation = 16 * density

        // 1. Red blinking dot icon
        val dot = ImageView(context).apply {
            val s = (12 * density).toInt()
            layoutParams = LayoutParams(s, s).apply {
                marginEnd = (8 * density).toInt()
            }
            val d = ContextCompat.getDrawable(context, R.drawable.ic_action_record_dot)?.mutate()
            d?.setTint(Color.rgb(224, 108, 117))
            setImageDrawable(d)
        }
        addView(dot)

        // 2. Timer Text
        timeTextView = TextView(context).apply {
            text = "00:00"
            setTextColor(Color.rgb(238, 240, 242))
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (12 * density).toInt()
            }
        }
        addView(timeTextView)

        // 3. Stop Button
        val btnStop = ImageView(context).apply {
            val s = (28 * density).toInt()
            val pad = (5 * density).toInt()
            layoutParams = LayoutParams(s, s)
            setPadding(pad, pad, pad, pad)
            val d = ContextCompat.getDrawable(context, R.drawable.ic_action_stop)?.mutate()
            d?.setTint(Color.rgb(224, 108, 117))
            setImageDrawable(d)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(50, 255, 255, 255))
            }
            contentDescription = "Stop Recording"
            setOnClickListener { onStopRequested() }
        }
        addView(btnStop)
    }

    fun setTime(text: String) {
        timeTextView.text = text
    }

    companion object {
        private const val TAG = "RecordingPill"
        private var currentPill: FloatingRecordingPill? = null

        fun show(context: Context, onStop: () -> Unit) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            if (!Settings.canDrawOverlays(context)) return

            dismiss(wm)

            val density = context.resources.displayMetrics.density
            val pill = FloatingRecordingPill(context, onStop)
            currentPill = pill

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = (16 * density).toInt()
                y = (48 * density).toInt()
            }

            try {
                wm.addView(pill, params)
            } catch (e: Exception) {
                Log.e(TAG, "Failed adding floating recording pill", e)
                currentPill = null
            }
        }

        fun updateTime(timeStr: String) {
            currentPill?.setTime(timeStr)
        }

        fun dismiss(wm: WindowManager? = null) {
            currentPill?.let { pill ->
                try {
                    val manager = wm ?: pill.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    manager.removeView(pill)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove recording pill", e)
                }
                currentPill = null
            }
        }
    }
}
