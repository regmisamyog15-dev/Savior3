package com.example.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.QuickActionType
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@SuppressLint("ViewConstructor")
class RadialOverlayView(
    context: Context,
    private val actions: List<QuickActionType>,
    private val centerX: Float,
    private val centerY: Float,
    private val onActionSelected: (QuickActionType) -> Unit,
    private val onDismissRequested: () -> Unit,
    private val scale: Float = 1.0f
) : View(context) {

    private val density = resources.displayMetrics.density
    private val buttonArcRadius = density * 98f * scale
    private val innerGuideRadius = density * 66f * scale
    private val outerGuideRadius = density * 130f * scale
    private val buttonRadius = density * 23f * scale
    private val centerOrbRadius = density * 28f * scale

    private val mainHandler = Handler(Looper.getMainLooper())
    private var animProgress = 0f
    private var activeHoverAction: QuickActionType? = null

    private val autoDismissRunnable = Runnable { dismissWithAnimation() }

    private val scrimPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(120, 16, 18, 20)
    }

    private val dialArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(135, 16, 18, 20)
    }

    private val innerGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density * 1.1f
        color = Color.argb(45, 238, 240, 242)
    }

    private val outerGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density * 1.0f
        color = Color.argb(32, 238, 240, 242)
    }

    private val buttonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(165, 24, 27, 30)
    }

    private val buttonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density * 1.2f
        color = Color.argb(55, 238, 240, 242)
    }

    private val buttonActiveBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density * 2.2f
        color = Color.argb(235, 210, 188, 148) // Brass glow
    }

    private val orbGlassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val orbRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density * 1.4f
        color = Color.argb(110, 238, 240, 242)
    }
    private val orbBrassRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density * 1.8f
        color = Color.argb(180, 210, 188, 148)
    }
    private val orbCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val captionBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(225, 16, 18, 20)
    }
    private val captionBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density * 1.0f
        color = Color.argb(45, 238, 240, 242)
    }
    private val captionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(238, 240, 242)
        textSize = density * 13f
        textAlign = Paint.Align.RIGHT
        isFakeBoldText = true
    }
    private val captionSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(142, 148, 156)
        textSize = density * 10.5f
        textAlign = Paint.Align.RIGHT
    }

    private val actionDrawables: MutableMap<QuickActionType, Drawable> = mutableMapOf()

    init {
        loadDrawables()
        startEntranceAnimation()
        mainHandler.postDelayed(autoDismissRunnable, 10000L)
    }

    private fun loadDrawables() {
        for (action in actions) {
            val resId = getActionIconRes(action)
            val d = ContextCompat.getDrawable(context, resId)?.mutate()
            if (d != null) {
                d.setTint(Color.rgb(238, 240, 242))
                actionDrawables[action] = d
            }
        }
    }

    private fun getActionIconRes(action: QuickActionType): Int {
        return when (action) {
            QuickActionType.VOLUME -> R.drawable.ic_action_volume_up
            QuickActionType.VOLUME_UP -> R.drawable.ic_action_volume_up
            QuickActionType.VOLUME_DOWN -> R.drawable.ic_action_volume_down
            QuickActionType.VOLUME_MUTE -> R.drawable.ic_action_volume_mute
            QuickActionType.SCREENSHOT -> R.drawable.ic_action_screenshot
            QuickActionType.LIVE_SCREENSHOT -> R.drawable.ic_action_screenshot
            QuickActionType.SCREEN_RECORDER -> R.drawable.ic_action_screen_recorder
            QuickActionType.BACKGROUND_PLAY -> R.drawable.ic_action_background_play
            QuickActionType.LOCK_SCREEN -> R.drawable.ic_action_lock
            QuickActionType.SPLIT_SCREEN -> R.drawable.ic_action_split_screen
            QuickActionType.RECENTS -> R.drawable.ic_action_recents
            QuickActionType.QUICK_SETTINGS -> R.drawable.ic_action_quick_settings
            QuickActionType.HOME -> R.drawable.ic_action_home
            QuickActionType.NOTIFICATIONS -> R.drawable.ic_action_notifications
            QuickActionType.MEDIA_PLAY_PAUSE -> R.drawable.ic_action_media_play_pause
            QuickActionType.LIVE_SS_1 -> R.drawable.ic_action_screenshot
            QuickActionType.LIVE_SS_2 -> R.drawable.ic_action_screen_recorder
        }
    }

    private fun isDockedLeft(): Boolean {
        val w = if (width > 0) width.toFloat() else resources.displayMetrics.widthPixels.toFloat()
        return centerX < w / 2f
    }

    private fun getButtonAngleDeg(index: Int, count: Int, isLeft: Boolean): Double {
        if (count <= 1) return if (isLeft) 0.0 else 180.0
        val sweep = 120.0
        val step = sweep / (count - 1)
        return if (isLeft) {
            -60.0 + (index * step)
        } else {
            240.0 - (index * step)
        }
    }

    private fun getButtonPosition(index: Int, count: Int, radius: Float, isLeft: Boolean): PointF {
        val deg = getButtonAngleDeg(index, count, isLeft)
        val rad = Math.toRadians(deg)
        val bx = centerX + (radius * cos(rad)).toFloat()
        val by = centerY + (radius * sin(rad)).toFloat()
        return PointF(bx, by)
    }

    private fun startEntranceAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260L
            interpolator = OvershootInterpolator(1.15f)
            addUpdateListener { va ->
                animProgress = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private var isDismissing = false

    fun dismissWithAnimation() {
        if (isDismissing) return
        isDismissing = true
        mainHandler.removeCallbacks(autoDismissRunnable)
        ValueAnimator.ofFloat(animProgress, 0f).apply {
            duration = 180L
            addUpdateListener { va ->
                animProgress = va.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onDismissRequested()
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (animProgress <= 0.01f) return

        // Scrim
        scrimPaint.alpha = (120 * animProgress).toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

        val isLeft = isDockedLeft()
        val curInnerR = innerGuideRadius * animProgress
        val curOuterR = outerGuideRadius * animProgress
        val curBtnArcR = buttonArcRadius * animProgress

        val outerRect = RectF(centerX - curOuterR, centerY - curOuterR, centerX + curOuterR, centerY + curOuterR)
        val innerRect = RectF(centerX - curInnerR, centerY - curInnerR, centerX + curInnerR, centerY + curInnerR)

        val arcStartDeg = if (isLeft) 300f else 120f
        val arcSweepDeg = 120f

        val path = Path().apply {
            arcTo(outerRect, arcStartDeg, arcSweepDeg)
            arcTo(innerRect, arcStartDeg + arcSweepDeg, -arcSweepDeg)
            close()
        }

        dialArcPaint.alpha = (135 * animProgress).toInt()
        canvas.drawPath(path, dialArcPaint)
        canvas.drawArc(outerRect, arcStartDeg, arcSweepDeg, false, outerGuidePaint)
        canvas.drawArc(innerRect, arcStartDeg, arcSweepDeg, false, innerGuidePaint)

        drawCenterOrb(canvas)

        val count = actions.size
        if (count == 0) return

        for (i in 0 until count) {
            val action = actions[i]
            val pt = getButtonPosition(i, count, curBtnArcR, isLeft)
            val bx = pt.x
            val by = pt.y
            val bR = animProgress * buttonRadius

            canvas.drawCircle(bx, by, bR, buttonBgPaint)
            if (action == activeHoverAction) {
                canvas.drawCircle(bx, by, bR + (density * 1.5f), buttonActiveBorderPaint)
            } else {
                canvas.drawCircle(bx, by, bR, buttonBorderPaint)
            }

            actionDrawables[action]?.let { d ->
                val iconSize = (density * 12.5f * animProgress).toInt()
                d.setBounds(
                    (bx - iconSize).toInt(),
                    (by - iconSize).toInt(),
                    (bx + iconSize).toInt(),
                    (by + iconSize).toInt()
                )
                d.draw(canvas)
            }
        }

        val tooltipAction = activeHoverAction
        if (animProgress > 0.75f && tooltipAction != null) {
            drawCaptionTooltip(canvas, tooltipAction)
        }
    }

    private fun drawCenterOrb(canvas: Canvas) {
        val r = centerOrbRadius * animProgress
        if (r <= 1f) return

        val glassShader = RadialGradient(
            centerX - (r * 0.35f),
            centerY - (r * 0.45f),
            r * 1.3f,
            intArrayOf(
                Color.argb(215, 255, 255, 255),
                Color.argb(90, 238, 240, 242),
                Color.argb(220, 24, 27, 30),
                Color.argb(250, 16, 18, 20)
            ),
            floatArrayOf(0f, 0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        orbGlassPaint.shader = glassShader
        canvas.drawCircle(centerX, centerY, r, orbGlassPaint)
        canvas.drawCircle(centerX, centerY, r, orbRimPaint)
        canvas.drawCircle(centerX, centerY, r - (density * 1.5f), orbBrassRingPaint)

        val coreR = 0.36f * r
        val coreShader = RadialGradient(
            centerX - (coreR * 0.25f),
            centerY - (coreR * 0.35f),
            coreR * 1.1f,
            intArrayOf(
                Color.rgb(243, 230, 203),
                Color.rgb(210, 188, 148),
                Color.rgb(168, 144, 102)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        orbCorePaint.shader = coreShader
        canvas.drawCircle(centerX, centerY, coreR, orbCorePaint)
    }

    private fun drawCaptionTooltip(canvas: Canvas, action: QuickActionType) {
        val isDockedLeft = centerX < width / 2f
        val padH = density * 14f
        val title = action.title
        val sub = action.subtitle
        val titleW = captionTitlePaint.measureText(title)
        val subW = captionSubPaint.measureText(sub)
        val boxW = maxOf(titleW, subW) + (2f * padH)
        val boxH = density * 42f

        val boxX = if (isDockedLeft) {
            centerX + (density * 48f)
        } else {
            centerX - boxW - (density * 48f)
        }
        val boxY = (centerY - density * 80f).coerceIn(density * 40f, height - density * 120f)

        val rect = RectF(boxX, boxY, boxX + boxW, boxY + boxH)
        canvas.drawRoundRect(rect, density * 13f, density * 13f, captionBgPaint)
        canvas.drawRoundRect(rect, density * 13f, density * 13f, captionBorderPaint)

        val textRight = boxX + boxW - padH
        canvas.drawText(title, textRight, boxY + density * 18f, captionTitlePaint)
        canvas.drawText(sub, textRight, boxY + density * 32f, captionSubPaint)
    }

    private var pressedAction: QuickActionType? = null

    private fun findActionAt(x: Float, y: Float): QuickActionType? {
        if (actions.isEmpty()) return null
        val count = actions.size
        val isLeft = isDockedLeft()

        val distCenter = hypot(x - centerX, y - centerY)
        if (distCenter <= centerOrbRadius * 1.35f) {
            return null
        }

        var closestAction: QuickActionType? = null
        var minDistance = Float.MAX_VALUE

        for (i in 0 until count) {
            val pt = getButtonPosition(i, count, buttonArcRadius, isLeft)
            val d = hypot(x - pt.x, y - pt.y)
            if (d < minDistance) {
                minDistance = d
                closestAction = actions[i]
            }
        }

        val maxHitDist = buttonRadius * 1.38f
        return if (minDistance <= maxHitDist) closestAction else null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val tx = event.x
        val ty = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val distCenter = hypot(tx - centerX, ty - centerY)
                if (distCenter <= centerOrbRadius * 1.35f) {
                    dismissWithAnimation()
                    return true
                }

                val target = findActionAt(tx, ty)
                pressedAction = target
                activeHoverAction = target
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val hovered = findActionAt(tx, ty)
                if (hovered != activeHoverAction) {
                    activeHoverAction = hovered
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val releaseTarget = findActionAt(tx, ty)
                val targetToExecute = if (pressedAction != null && releaseTarget == pressedAction) {
                    pressedAction
                } else {
                    releaseTarget ?: pressedAction
                }

                pressedAction = null
                activeHoverAction = null
                invalidate()

                if (targetToExecute != null) {
                    onActionSelected(targetToExecute)
                } else {
                    dismissWithAnimation()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedAction = null
                activeHoverAction = null
                invalidate()
                return true
            }
            else -> return super.onTouchEvent(event)
        }
    }
}
