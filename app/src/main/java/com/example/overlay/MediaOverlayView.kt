package com.example.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.browser.BrowserMediaState
import com.example.browser.SaviorBrowserActivity
import com.example.browser.SaviorMediaManager

@SuppressLint("ViewConstructor")
class MediaOverlayView(
    context: Context,
    private var isPlaying: Boolean,
    private var trackTitle: String,
    private var trackSource: String,
    private val onPlayPauseRequested: () -> Unit,
    private val onNextRequested: () -> Unit,
    private val onPreviousRequested: () -> Unit,
    private val onOpenBrowserRequested: () -> Unit,
    private val onDismissRequested: () -> Unit
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable = Runnable { dismissWithAnimation() }

    private val titleText: TextView
    private val subText: TextView
    private val statusDot: ImageView
    private val playPauseButton: ImageView

    init {
        orientation = VERTICAL
        val padH = (16 * density).toInt()
        val padV = (14 * density).toInt()
        setPadding(padH, padV, padH, padV)

        // Card background: Dark panel with Brass border
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18 * density
            setColor(Color.argb(245, 20, 22, 26))
            setStroke((1.4f * density).toInt(), Color.argb(140, 210, 188, 148))
        }
        background = bgDrawable
        elevation = 16 * density

        // 1. Header Row: Status dot, Title/Artist column, Close button
        val headerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        statusDot = ImageView(context).apply {
            val s = (10 * density).toInt()
            layoutParams = LayoutParams(s, s).apply {
                marginEnd = (10 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (isPlaying) Color.rgb(142, 192, 124) else Color.rgb(210, 188, 148))
            }
        }

        val textCol = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = (8 * density).toInt()
            }
        }

        titleText = TextView(context).apply {
            text = if (trackTitle.isNotBlank()) trackTitle else "Savior Background Play"
            setTextColor(Color.rgb(238, 240, 242))
            textSize = 13.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        subText = TextView(context).apply {
            text = if (trackSource.isNotBlank()) trackSource else "Active • Tap to control"
            setTextColor(Color.rgb(168, 174, 182))
            textSize = 11f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        textCol.addView(titleText)
        textCol.addView(subText)

        val btnClose = ImageView(context).apply {
            val s = (30 * density).toInt()
            val pad = (6 * density).toInt()
            layoutParams = LayoutParams(s, s)
            setPadding(pad, pad, pad, pad)
            val d = ContextCompat.getDrawable(context, R.drawable.ic_action_close)?.mutate()
            d?.setTint(Color.rgb(142, 148, 156))
            setImageDrawable(d)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(40, 255, 255, 255))
            }
            setOnClickListener { dismissWithAnimation() }
        }

        headerRow.addView(statusDot)
        headerRow.addView(textCol)
        headerRow.addView(btnClose)
        addView(headerRow)

        // 2. Control Buttons Row: [⏮ Prev] [⏯ Play/Pause] [⏭ Next] [🌐 Open Browser]
        val controlsRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = (12 * density).toInt()
            }
        }

        // Prev button
        val btnPrev = ImageView(context).apply {
            val s = (38 * density).toInt()
            val pad = (8 * density).toInt()
            layoutParams = LayoutParams(s, s).apply {
                marginEnd = (10 * density).toInt()
            }
            setPadding(pad, pad, pad, pad)
            val d = ContextCompat.getDrawable(context, R.drawable.ic_action_recents)?.mutate()
            d?.setTint(Color.rgb(238, 240, 242))
            setImageDrawable(d)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(45, 255, 255, 255))
            }
            contentDescription = "Previous Track"
            setOnClickListener {
                resetAutoDismiss()
                onPreviousRequested()
            }
        }

        // Play / Pause button
        playPauseButton = ImageView(context).apply {
            val s = (48 * density).toInt()
            val pad = (10 * density).toInt()
            layoutParams = LayoutParams(s, s).apply {
                marginEnd = (10 * density).toInt()
            }
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(210, 188, 148)) // Brass button
            }
            contentDescription = "Play or Pause"
            setOnClickListener {
                resetAutoDismiss()
                isPlaying = !isPlaying
                updatePlayPauseIcon(isPlaying)
                onPlayPauseRequested()
            }
        }
        updatePlayPauseIcon(isPlaying)

        // Next button
        val btnNext = ImageView(context).apply {
            val s = (38 * density).toInt()
            val pad = (8 * density).toInt()
            layoutParams = LayoutParams(s, s).apply {
                marginEnd = (12 * density).toInt()
            }
            setPadding(pad, pad, pad, pad)
            val d = ContextCompat.getDrawable(context, R.drawable.ic_action_recents)?.mutate()
            d?.setTint(Color.rgb(238, 240, 242))
            setImageDrawable(d)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(45, 255, 255, 255))
            }
            contentDescription = "Next Track"
            setOnClickListener {
                resetAutoDismiss()
                onNextRequested()
            }
        }

        // Open Browser pill button
        val btnOpenBrowser = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            val pH = (12 * density).toInt()
            val pV = (8 * density).toInt()
            setPadding(pH, pV, pH, pV)
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * density
                setColor(Color.argb(55, 210, 188, 148))
                setStroke((1 * density).toInt(), Color.argb(120, 210, 188, 148))
            }
            setOnClickListener {
                dismissWithAnimation()
                onOpenBrowserRequested()
            }

            val icon = ImageView(context).apply {
                val s = (16 * density).toInt()
                layoutParams = LayoutParams(s, s).apply { marginEnd = (6 * density).toInt() }
                val d = ContextCompat.getDrawable(context, R.drawable.ic_action_background_play)?.mutate()
                d?.setTint(Color.rgb(210, 188, 148))
                setImageDrawable(d)
            }
            val label = TextView(context).apply {
                text = "Browser"
                setTextColor(Color.rgb(210, 188, 148))
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            addView(icon)
            addView(label)
        }

        controlsRow.addView(btnPrev)
        controlsRow.addView(playPauseButton)
        controlsRow.addView(btnNext)
        controlsRow.addView(btnOpenBrowser)
        addView(controlsRow)

        startEntranceAnimation()
        mainHandler.postDelayed(autoDismissRunnable, 8000L)
    }

    private fun updatePlayPauseIcon(playing: Boolean) {
        val iconRes = if (playing) R.drawable.ic_action_media_play_pause else R.drawable.ic_action_background_play
        val d = ContextCompat.getDrawable(context, iconRes)?.mutate()
        d?.setTint(Color.rgb(20, 22, 26)) // Dark icon on Brass button
        playPauseButton.setImageDrawable(d)

        statusDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (playing) Color.rgb(142, 192, 124) else Color.rgb(210, 188, 148))
        }
    }

    fun updatePlaybackState(playing: Boolean, title: String, source: String) {
        this.isPlaying = playing
        this.trackTitle = title
        this.trackSource = source

        titleText.text = if (title.isNotBlank()) title else "Savior Background Play"
        subText.text = if (source.isNotBlank()) source else if (playing) "Playing" else "Paused"
        updatePlayPauseIcon(playing)
        resetAutoDismiss()
    }

    private fun resetAutoDismiss() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        mainHandler.postDelayed(autoDismissRunnable, 8000L)
    }

    private fun startEntranceAnimation() {
        alpha = 0f
        translationY = -40f * density
        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private var isDismissing = false

    fun dismissWithAnimation() {
        if (isDismissing) return
        isDismissing = true
        mainHandler.removeCallbacks(autoDismissRunnable)
        animate()
            .alpha(0f)
            .translationY(-30f * density)
            .setDuration(160L)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onDismissRequested()
                }
            })
            .start()
    }
}
