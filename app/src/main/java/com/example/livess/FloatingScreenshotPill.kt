package com.example.livess

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
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
 * Floating preview pill presented after an explicit Live Screenshot is captured.
 * Displays a thumbnail with immediate Edit, Share, and Dismiss affordances.
 * Automatically dismisses after 6 seconds if not interacted with.
 */
@SuppressLint("ViewConstructor")
class FloatingScreenshotPill(
    context: Context,
    private val screenshotBitmap: Bitmap,
    private val screenshotUri: Uri,
    private val onDismiss: () -> Unit
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable = Runnable { dismiss() }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padH = (12 * density).toInt()
        val padV = (8 * density).toInt()
        setPadding(padH, padV, padH, padV)

        // Pill background: Dark glass panel with refined Brass border
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24 * density
            setColor(Color.argb(235, 20, 22, 26))
            setStroke((1.2f * density).toInt(), Color.argb(160, 210, 188, 148))
        }
        elevation = 16 * density

        // 1. Thumbnail Image View
        val thumbnailView = ImageView(context).apply {
            val thumbW = (40 * density).toInt()
            val thumbH = (48 * density).toInt()
            layoutParams = LayoutParams(thumbW, thumbH).apply {
                marginEnd = (10 * density).toInt()
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(screenshotBitmap)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8 * density
                setStroke((1 * density).toInt(), Color.argb(70, 238, 240, 242))
            }
            clipToOutline = true
            contentDescription = "Screenshot Preview Thumbnail"
            setOnClickListener {
                resetAutoDismiss()
                ScreenshotEditorActivity.launch(context, screenshotUri)
                dismiss()
            }
        }
        addView(thumbnailView)

        // 2. Info Text Column
        val textCol = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = (8 * density).toInt()
            }
        }
        val titleText = TextView(context).apply {
            text = "Screenshot Captured"
            setTextColor(Color.rgb(238, 240, 242))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val subText = TextView(context).apply {
            text = "Clean 100% • Tap to edit or share"
            setTextColor(Color.rgb(210, 188, 148))
            textSize = 11f
        }
        textCol.addView(titleText)
        textCol.addView(subText)
        addView(textCol)

        // 3. Edit Action Button
        val btnEdit = createActionButton(
            iconRes = R.drawable.ic_action_edit,
            contentDesc = "Edit Screenshot",
            tintColor = Color.rgb(238, 240, 242)
        ) {
            ScreenshotEditorActivity.launch(context, screenshotUri)
            dismiss()
        }
        addView(btnEdit)

        // 4. Share Action Button
        val btnShare = createActionButton(
            iconRes = R.drawable.ic_action_share,
            contentDesc = "Share Screenshot",
            tintColor = Color.rgb(210, 188, 148)
        ) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, screenshotUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Screenshot via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            dismiss()
        }
        addView(btnShare)

        // 5. Close / Dismiss Button
        val btnClose = createActionButton(
            iconRes = R.drawable.ic_action_close,
            contentDesc = "Close Preview",
            tintColor = Color.rgb(142, 148, 156)
        ) {
            dismiss()
        }
        addView(btnClose)

        mainHandler.postDelayed(autoDismissRunnable, 6000L)
    }

    private fun createActionButton(
        iconRes: Int,
        contentDesc: String,
        tintColor: Int,
        onClick: () -> Unit
    ): ImageView {
        val size = (34 * density).toInt()
        val pad = (6 * density).toInt()
        return ImageView(context).apply {
            layoutParams = LayoutParams(size, size).apply {
                marginStart = (4 * density).toInt()
            }
            setPadding(pad, pad, pad, pad)
            val d = ContextCompat.getDrawable(context, iconRes)?.mutate()
            d?.setTint(tintColor)
            setImageDrawable(d)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(40, 255, 255, 255))
            }
            contentDescription = contentDesc
            setOnClickListener {
                resetAutoDismiss()
                onClick()
            }
        }
    }

    private fun resetAutoDismiss() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        mainHandler.postDelayed(autoDismissRunnable, 6000L)
    }

    fun dismiss() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        onDismiss()
    }

    companion object {
        private const val TAG = "ScreenshotPill"
        private var currentPill: FloatingScreenshotPill? = null

        fun show(context: Context, bitmap: Bitmap, uri: Uri) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            if (!Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Cannot show screenshot pill: overlay permission missing")
                return
            }

            dismissCurrent(wm)

            val density = context.resources.displayMetrics.density
            val pill = FloatingScreenshotPill(context, bitmap, uri) {
                dismissCurrent(wm)
            }
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
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = (48 * density).toInt()
            }

            try {
                wm.addView(pill, params)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add floating screenshot pill to WindowManager", e)
                currentPill = null
            }
        }

        fun dismissCurrent(wm: WindowManager? = null) {
            currentPill?.let { pill ->
                try {
                    val manager = wm ?: pill.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    manager.removeView(pill)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove screenshot pill", e)
                }
                currentPill = null
            }
        }
    }
}
