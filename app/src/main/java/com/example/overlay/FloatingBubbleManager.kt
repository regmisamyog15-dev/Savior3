package com.example.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowMetrics
import com.example.data.QuickCirclePreferences
import com.example.model.QuickActionType
import com.example.service.SystemActionController
import kotlin.math.min
import kotlin.math.roundToInt

class FloatingBubbleManager(
    private val context: Context,
    private val actionController: SystemActionController
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val preferences = QuickCirclePreferences.getInstance(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var bubbleView: FloatingBubbleView? = null
    private var bubbleLayoutParams: WindowManager.LayoutParams? = null
    private var radialOverlayView: RadialOverlayView? = null
    private var volumeOverlayView: VolumeOverlayView? = null

    private var isBubbleAttached = false
    private var isTemporaryMode = false

    private val autoHideRunnable = Runnable {
        if (!preferences.isPermanent.value || isTemporaryMode) {
            hideBubble()
            isTemporaryMode = false
        }
    }

    private fun getOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun getPreferredOverlayType(): Int {
        // If running inside accessibility service, TYPE_ACCESSIBILITY_OVERLAY (2032) can be used
        return try {
            if (context is android.accessibilityservice.AccessibilityService) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                getOverlayType()
            }
        } catch (e: Exception) {
            getOverlayType()
        }
    }

    fun showBubble(temporaryDurationMs: Long? = null) {
        if (!preferences.isEnabled.value) return

        mainHandler.removeCallbacks(autoHideRunnable)
        if (temporaryDurationMs != null) {
            isTemporaryMode = true
            mainHandler.postDelayed(autoHideRunnable, temporaryDurationMs)
        } else if (!preferences.isPermanent.value) {
            mainHandler.postDelayed(autoHideRunnable, 10000L)
        }

        if (isBubbleAttached && bubbleView != null) {
            bubbleView?.visibility = android.view.View.VISIBLE
            return
        }

        val density = context.resources.displayMetrics.density
        val sizeDp = preferences.bubbleSizeDp.value
        val opacity = preferences.bubbleOpacity.value
        val sizePx = (sizeDp * density).toInt()

        val safeBounds = getSafeBounds(sizePx)
        val posX = preferences.positionX.value
        val posY = preferences.positionY.value

        val (initialX, initialY) = if (posX < 0 || posY < 0) {
            safeBounds.right to ((safeBounds.top + safeBounds.bottom) * 0.45f).roundToInt()
        } else {
            posX.coerceIn(safeBounds.left, safeBounds.right) to posY.coerceIn(safeBounds.top, safeBounds.bottom)
        }

        val layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            getPreferredOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }
        bubbleLayoutParams = layoutParams

        val view = FloatingBubbleView(
            context = context,
            sizeDp = sizeDp,
            baseOpacity = opacity,
            onBubbleClicked = { handleBubbleTap() },
            onPositionUpdated = { newX, newY -> updateBubblePosition(newX, newY) },
            onDragFinished = { finalX, finalY ->
                val bounds = getSafeBounds(sizePx)
                val clampedX = finalX.coerceIn(bounds.left, bounds.right)
                val clampedY = finalY.coerceIn(bounds.top, bounds.bottom)
                preferences.savePosition(clampedX, clampedY)
                resetIdleTimeout()
            }
        )

        @SuppressLint("ClickableViewAccessibility")
        view.setOnTouchListener { _, event ->
            val currX = bubbleLayoutParams?.x ?: 0
            val currY = bubbleLayoutParams?.y ?: 0
            val screenW = getScreenDimensions().x
            val edge = preferences.dockEdge.value
            view.handleTouch(event, currX, currY, screenW, edge)
        }

        bubbleView = view

        try {
            windowManager.addView(bubbleView, layoutParams)
            isBubbleAttached = true
        } catch (e: WindowManager.BadTokenException) {
            Log.w(TAG, "showBubble: token not valid yet; checking application overlay fallback", e)
            isBubbleAttached = false
            if (Settings.canDrawOverlays(context)) {
                try {
                    layoutParams.type = getOverlayType()
                    windowManager.addView(bubbleView, layoutParams)
                    isBubbleAttached = true
                    Log.i(TAG, "showBubble: fallback to TYPE_APPLICATION_OVERLAY succeeded")
                } catch (fallbackEx: Exception) {
                    Log.w(TAG, "showBubble: fallback failed", fallbackEx)
                    isBubbleAttached = false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "showBubble: addView failed", e)
            isBubbleAttached = false
        }
    }

    fun hideBubble() {
        mainHandler.removeCallbacks(autoHideRunnable)
        hideRadialMenu()
        hideVolumePanel()
        if (isBubbleAttached && bubbleView != null) {
            try {
                windowManager.removeView(bubbleView)
            } catch (e: Exception) {
                Log.w(TAG, "hideBubble: removeView failed", e)
            }
            bubbleView = null
            isBubbleAttached = false
        }
    }

    fun triggerTemporaryShow(durationMs: Long = 10000L) {
        if (!isBubbleAttached) {
            showBubble(durationMs)
            return
        }
        bubbleView?.visibility = android.view.View.VISIBLE
        mainHandler.removeCallbacks(autoHideRunnable)
        mainHandler.postDelayed(autoHideRunnable, durationMs)
    }

    fun updateAppearance() {
        val sizeDp = preferences.bubbleSizeDp.value
        val opacity = preferences.bubbleOpacity.value
        bubbleView?.updateAppearance(sizeDp, opacity)

        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        bubbleLayoutParams?.let { lp ->
            lp.width = sizePx
            lp.height = sizePx
            val bounds = getSafeBounds(sizePx)
            lp.x = lp.x.coerceIn(bounds.left, bounds.right)
            lp.y = lp.y.coerceIn(bounds.top, bounds.bottom)
            preferences.savePosition(lp.x, lp.y)
            if (isBubbleAttached && bubbleView != null) {
                try {
                    windowManager.updateViewLayout(bubbleView, lp)
                } catch (e: Exception) {
                    Log.w(TAG, "updateAppearance failed", e)
                }
            }
        }
    }

    private fun updateBubblePosition(x: Int, y: Int) {
        bubbleLayoutParams?.let { lp ->
            lp.x = x
            lp.y = y
            if (isBubbleAttached && bubbleView != null) {
                try {
                    windowManager.updateViewLayout(bubbleView, lp)
                } catch (e: Exception) {
                    Log.w(TAG, "updateBubblePosition failed", e)
                }
            }
        }
    }

    private fun handleBubbleTap() {
        resetIdleTimeout()
        // Notify Live SS service to freeze pre-buffer and grab clean key frame
        com.example.livess.LiveSsCaptureService.instance?.onBubbleTapped()

        if (radialOverlayView != null) {
            hideRadialMenu()
        } else {
            showRadialMenu()
        }
    }

    private fun showRadialMenu() {
        if (radialOverlayView != null) return

        val screen = getScreenDimensions()
        val density = context.resources.displayMetrics.density
        val baseRadius = density * 108f
        val baseButtonRadius = density * 24f
        val basePadding = density * 8f
        val baseRequiredMargin = baseRadius + baseButtonRadius + basePadding

        val smallestDimension = min(screen.x, screen.y)
        val maxFittingMargin = (smallestDimension / 2f) - (density * 4f)
        val radialScale = if (baseRequiredMargin > maxFittingMargin) {
            (maxFittingMargin / baseRequiredMargin).coerceIn(0.45f, 1.0f)
        } else {
            1.0f
        }
        val requiredMargin = baseRequiredMargin * radialScale

        val bubbleX = bubbleLayoutParams?.x?.toFloat() ?: (screen.x / 2f)
        val bubbleY = bubbleLayoutParams?.y?.toFloat() ?: (screen.y / 2f)
        val bubbleSize = preferences.bubbleSizeDp.value * density

        val isNearRightEdge = (bubbleX + (bubbleSize / 2f)) >= (screen.x / 2f)
        val menuCenterX = if (isNearRightEdge) {
            (bubbleX + (bubbleSize / 2f)).coerceIn(requiredMargin, screen.x - (bubbleSize / 4f))
        } else {
            (bubbleX + (bubbleSize / 2f)).coerceIn(bubbleSize / 4f, screen.x - requiredMargin)
        }
        val menuCenterY = (bubbleY + (bubbleSize / 2f)).coerceIn(density * 72f, screen.y - (72f * density))

        val actions = preferences.activeActions.value
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            getPreferredOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        radialOverlayView = RadialOverlayView(
            context = context,
            actions = actions,
            centerX = menuCenterX,
            centerY = menuCenterY,
            onActionSelected = { action ->
                hideRadialMenu()
                when (action) {
                    QuickActionType.VOLUME -> {
                        val volumeInfo = actionController.getVolumeInfo()
                        showVolumePanel(volumeInfo.first, volumeInfo.second, volumeInfo.third)
                    }
                    QuickActionType.BACKGROUND_PLAY -> {
                        showMediaPanel()
                    }
                    QuickActionType.VOLUME_UP,
                    QuickActionType.VOLUME_DOWN,
                    QuickActionType.VOLUME_MUTE -> {
                        actionController.performAction(action) { current, max, isMuted ->
                            showVolumePanel(current, max, isMuted)
                        }
                    }
                    else -> {
                        actionController.performAction(action)
                    }
                }
                resetIdleTimeout()
            },
            onDismissRequested = {
                hideRadialMenu()
                resetIdleTimeout()
            },
            scale = radialScale
        )

        try {
            windowManager.addView(radialOverlayView, params)
        } catch (e: WindowManager.BadTokenException) {
            Log.w(TAG, "showRadialMenu: token not ready; checking fallback", e)
            if (Settings.canDrawOverlays(context)) {
                try {
                    params.type = getOverlayType()
                    windowManager.addView(radialOverlayView, params)
                } catch (fallbackEx: Exception) {
                    Log.w(TAG, "showRadialMenu: fallback failed", fallbackEx)
                    radialOverlayView = null
                }
            } else {
                radialOverlayView = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "showRadialMenu: addView failed", e)
            radialOverlayView = null
        }
    }

    private fun hideRadialMenu() {
        radialOverlayView?.let { rov ->
            try {
                windowManager.removeView(rov)
            } catch (e: Exception) {
                Log.w(TAG, "hideRadialMenu failed", e)
            }
            radialOverlayView = null
        }
        com.example.livess.LiveSsCaptureService.instance?.onMenuDismissed()
    }

    /**
     * Returns screen-coordinate bounds of the bubble and radial menu to patch them out of Live SS frames.
     */
    fun getOverlayScreenRects(): List<Rect> {
        val list = mutableListOf<Rect>()
        bubbleLayoutParams?.let { lp ->
            if (isBubbleAttached && bubbleView?.visibility == android.view.View.VISIBLE) {
                list.add(Rect(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height))
            }
        }
        radialOverlayView?.let { rov ->
            if (rov.visibility == android.view.View.VISIBLE) {
                val screen = getScreenDimensions()
                list.add(Rect(0, 0, screen.x, screen.y))
            }
        }
        return list
    }

    /**
     * Hides all overlays cleanly so Savior's UI never contaminates captures.
     */
    fun hideAllOverlays() {
        bubbleView?.visibility = android.view.View.GONE
        hideRadialMenu()
        hideVolumePanel()
        hideMediaPanel()
    }

    /**
     * Restores overlays after capture completes.
     */
    fun restoreAllOverlays() {
        if (preferences.isEnabled.value && isBubbleAttached) {
            bubbleView?.visibility = android.view.View.VISIBLE
        }
    }

    fun hideAllForLiveSs() = hideAllOverlays()
    fun restoreAllForLiveSs() = restoreAllOverlays()

    fun showVolumePanel(currentVolume: Int, maxVolume: Int, isMuted: Boolean) {
        if (volumeOverlayView == null) {
            val density = context.resources.displayMetrics.density
            val panelWidth = (280f * density).toInt()

            val params = WindowManager.LayoutParams(
                panelWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getPreferredOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (72f * density).toInt()
            }

            volumeOverlayView = VolumeOverlayView(
                context = context,
                streamType = preferences.defaultStreamType.value,
                currentVolume = currentVolume,
                maxVolume = maxVolume,
                isMuted = isMuted,
                onVolumeChangeRequested = { newVol, playSound ->
                    actionController.setVolume(newVol, playSound) { updatedVol, max, muted ->
                        volumeOverlayView?.updateVolume(updatedVol, max, muted)
                    }
                    resetIdleTimeout()
                },
                onMuteToggleRequested = {
                    actionController.toggleMute { updatedVol, max, muted ->
                        volumeOverlayView?.updateVolume(updatedVol, max, muted)
                    }
                    resetIdleTimeout()
                },
                onDismissRequested = {
                    hideVolumePanel()
                }
            )

            try {
                windowManager.addView(volumeOverlayView, params)
            } catch (e: WindowManager.BadTokenException) {
                Log.w(TAG, "showVolumePanel: token not ready; checking fallback", e)
                if (Settings.canDrawOverlays(context)) {
                    try {
                        params.type = getOverlayType()
                        windowManager.addView(volumeOverlayView, params)
                    } catch (fallbackEx: Exception) {
                        Log.w(TAG, "showVolumePanel fallback failed", fallbackEx)
                        volumeOverlayView = null
                    }
                } else {
                    volumeOverlayView = null
                }
            } catch (e: Exception) {
                Log.w(TAG, "showVolumePanel addView failed", e)
                volumeOverlayView = null
            }
        } else {
            volumeOverlayView?.updateVolume(currentVolume, maxVolume, isMuted)
        }
    }

    private fun hideVolumePanel() {
        volumeOverlayView?.let { vov ->
            try {
                windowManager.removeView(vov)
            } catch (e: Exception) {
                Log.w(TAG, "hideVolumePanel failed", e)
            }
            volumeOverlayView = null
        }
    }

    private var mediaOverlayView: MediaOverlayView? = null

    fun showMediaPanel() {
        if (mediaOverlayView == null) {
            val density = context.resources.displayMetrics.density
            val panelWidth = (310f * density).toInt()

            val params = WindowManager.LayoutParams(
                panelWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getPreferredOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (72f * density).toInt()
            }

            val isPlaying = com.example.browser.SaviorMediaManager.mediaState.value == com.example.browser.BrowserMediaState.PLAYING
            val title = com.example.browser.SaviorMediaManager.mediaTitle.value
            val source = com.example.browser.SaviorMediaManager.mediaSource.value

            mediaOverlayView = MediaOverlayView(
                context = context,
                isPlaying = isPlaying,
                trackTitle = title,
                trackSource = source,
                onPlayPauseRequested = {
                    if (com.example.browser.SaviorMediaManager.mediaState.value == com.example.browser.BrowserMediaState.PLAYING) {
                        com.example.browser.SaviorMediaManager.onPauseRequested?.invoke()
                    } else {
                        com.example.browser.SaviorMediaManager.onPlayRequested?.invoke()
                    }
                    resetIdleTimeout()
                },
                onNextRequested = {
                    com.example.browser.SaviorMediaManager.onNextRequested?.invoke()
                    resetIdleTimeout()
                },
                onPreviousRequested = {
                    com.example.browser.SaviorMediaManager.onPreviousRequested?.invoke()
                    resetIdleTimeout()
                },
                onOpenBrowserRequested = {
                    hideMediaPanel()
                    com.example.browser.SaviorBrowserActivity.launch(context)
                },
                onDismissRequested = {
                    hideMediaPanel()
                }
            )

            try {
                windowManager.addView(mediaOverlayView, params)
            } catch (e: Exception) {
                Log.w(TAG, "showMediaPanel addView failed", e)
                if (Settings.canDrawOverlays(context)) {
                    try {
                        params.type = getOverlayType()
                        windowManager.addView(mediaOverlayView, params)
                    } catch (fallbackEx: Exception) {
                        Log.w(TAG, "showMediaPanel fallback failed", fallbackEx)
                        mediaOverlayView = null
                    }
                } else {
                    mediaOverlayView = null
                }
            }
        } else {
            val isPlaying = com.example.browser.SaviorMediaManager.mediaState.value == com.example.browser.BrowserMediaState.PLAYING
            val title = com.example.browser.SaviorMediaManager.mediaTitle.value
            val source = com.example.browser.SaviorMediaManager.mediaSource.value
            mediaOverlayView?.updatePlaybackState(isPlaying, title, source)
        }
    }

    fun hideMediaPanel() {
        mediaOverlayView?.let { mov ->
            try {
                windowManager.removeView(mov)
            } catch (e: Exception) {
                Log.w(TAG, "hideMediaPanel failed", e)
            }
            mediaOverlayView = null
        }
    }

    private fun resetIdleTimeout() {
        if (!preferences.isPermanent.value || isTemporaryMode) {
            mainHandler.removeCallbacks(autoHideRunnable)
            mainHandler.postDelayed(autoHideRunnable, 10000L)
        }
    }

    private fun getSafeBounds(sizePx: Int): Rect {
        val screen = getScreenDimensions()
        val margin = (8f * context.resources.displayMetrics.density).roundToInt()
        return Rect(
            margin,
            margin,
            (screen.x - sizePx - margin).coerceAtLeast(margin),
            (screen.y - sizePx - margin).coerceAtLeast(margin)
        )
    }

    private fun getScreenDimensions(): Point {
        val size = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics: WindowMetrics = windowManager.currentWindowMetrics
            size.x = metrics.bounds.width()
            size.y = metrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getSize(size)
        }
        return size
    }

    companion object {
        private const val TAG = "QuickCircle"
    }
}
