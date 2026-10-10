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
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.StreamType

@SuppressLint("ViewConstructor")
class VolumeOverlayView(
    context: Context,
    private val streamType: StreamType,
    private var currentVolume: Int,
    private val maxVolume: Int,
    private var isMuted: Boolean,
    private val onVolumeChangeRequested: (volume: Int, playSound: Boolean) -> Unit,
    private val onMuteToggleRequested: () -> Unit,
    private val onDismissRequested: () -> Unit
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable = Runnable { dismissWithAnimation() }

    private val titleText: TextView
    private val valueText: TextView
    private val seekBar: SeekBar
    private val muteButton: ImageView

    init {
        orientation = VERTICAL
        val padH = (16 * density).toInt()
        val padV = (14 * density).toInt()
        setPadding(padH, padV, padH, padV)

        // Card background
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16 * density
            setColor(Color.argb(240, 24, 27, 30))
            setStroke((1.2f * density).toInt(), Color.argb(60, 238, 240, 242))
        }
        background = bgDrawable

        // Header Row: Title, Value, Mute Button
        val headerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        titleText = TextView(context).apply {
            text = streamType.title
            setTextColor(Color.rgb(238, 240, 242))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        valueText = TextView(context).apply {
            text = formatVolumeText(currentVolume, maxVolume, isMuted)
            setTextColor(Color.rgb(168, 174, 182))
            textSize = 12f
            setPadding(0, 0, (12 * density).toInt(), 0)
        }

        val mb = ImageView(context).apply {
            layoutParams = LayoutParams((32 * density).toInt(), (32 * density).toInt())
            setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
            setOnClickListener {
                resetAutoDismiss()
                onMuteToggleRequested()
            }
        }
        muteButton = mb
        updateMuteIcon(isMuted)

        headerRow.addView(titleText)
        headerRow.addView(valueText)
        headerRow.addView(muteButton)
        addView(headerRow)

        // Volume Controls Row: [- Button] [SeekBar] [+ Button]
        val controlsRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = (10 * density).toInt()
            }
        }

        val btnDown = ImageView(context).apply {
            val btnSize = (38 * density).toInt()
            layoutParams = LayoutParams(btnSize, btnSize)
            val pad = (6 * density).toInt()
            setPadding(pad, pad, pad, pad)
            val d = ContextCompat.getDrawable(context, R.drawable.ic_action_volume_down)?.mutate()
            d?.setTint(Color.rgb(238, 240, 242))
            setImageDrawable(d)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(70, 255, 255, 255))
            }
            contentDescription = "Volume Down"
            setOnClickListener {
                resetAutoDismiss()
                val targetVol = (currentVolume - 1).coerceAtLeast(0)
                updateVolume(targetVol, maxVolume, targetVol == 0)
                onVolumeChangeRequested(targetVol, true)
            }
        }

        // SeekBar
        seekBar = SeekBar(context).apply {
            max = maxVolume
            progress = currentVolume
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (6 * density).toInt()
                marginEnd = (6 * density).toInt()
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        resetAutoDismiss()
                        currentVolume = progress
                        isMuted = progress == 0
                        valueText.text = formatVolumeText(progress, maxVolume, isMuted)
                        updateMuteIcon(isMuted)
                        onVolumeChangeRequested(progress, false)
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {
                    resetAutoDismiss()
                }

                override fun onStopTrackingTouch(sb: SeekBar?) {
                    resetAutoDismiss()
                    val finalProgress = sb?.progress ?: currentVolume
                    onVolumeChangeRequested(finalProgress, true)
                }
            })
        }

        val btnUp = ImageView(context).apply {
            val btnSize = (38 * density).toInt()
            layoutParams = LayoutParams(btnSize, btnSize)
            val pad = (6 * density).toInt()
            setPadding(pad, pad, pad, pad)
            val d = ContextCompat.getDrawable(context, R.drawable.ic_action_volume_up)?.mutate()
            d?.setTint(Color.rgb(238, 240, 242))
            setImageDrawable(d)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(70, 255, 255, 255))
            }
            contentDescription = "Volume Up"
            setOnClickListener {
                resetAutoDismiss()
                val targetVol = (currentVolume + 1).coerceAtMost(maxVolume)
                updateVolume(targetVol, maxVolume, false)
                onVolumeChangeRequested(targetVol, true)
            }
        }

        controlsRow.addView(btnDown)
        controlsRow.addView(seekBar)
        controlsRow.addView(btnUp)
        addView(controlsRow)

        startEntranceAnimation()
        resetAutoDismiss()
    }

    private fun formatVolumeText(volume: Int, max: Int, muted: Boolean): String {
        return if (muted || volume == 0) {
            "Muted"
        } else {
            val percent = if (max > 0) ((volume.toFloat() / max) * 100).toInt() else 0
            "$percent%"
        }
    }

    private fun updateMuteIcon(muted: Boolean) {
        val iconRes = if (muted) R.drawable.ic_action_volume_mute else R.drawable.ic_action_volume_up
        val d = ContextCompat.getDrawable(context, iconRes)?.mutate()
        if (d != null) {
            val tintColor = if (muted) Color.rgb(224, 108, 117) else Color.rgb(210, 188, 148)
            d.setTint(tintColor)
            muteButton?.setImageDrawable(d)
        }
    }

    fun updateVolume(volume: Int, max: Int, muted: Boolean) {
        currentVolume = volume
        isMuted = muted
        seekBar.max = max
        seekBar.progress = volume
        valueText.text = formatVolumeText(volume, max, muted)
        updateMuteIcon(muted)
        resetAutoDismiss()
    }

    private fun resetAutoDismiss() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        mainHandler.postDelayed(autoDismissRunnable, 3500L)
    }

    private fun startEntranceAnimation() {
        alpha = 0f
        translationY = -24 * density
        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun dismissWithAnimation() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        animate()
            .alpha(0f)
            .translationY(-24 * density)
            .setDuration(180L)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onDismissRequested()
                }
            })
            .start()
    }
}
