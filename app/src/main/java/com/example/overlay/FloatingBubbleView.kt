package com.example.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import com.example.model.DockEdge
import kotlin.math.abs

@SuppressLint("ViewConstructor")
class FloatingBubbleView(
    context: Context,
    private var sizeDp: Int,
    private var baseOpacity: Float,
    private val onBubbleClicked: () -> Unit,
    private val onPositionUpdated: (x: Int, y: Int) -> Unit,
    private val onDragFinished: (x: Int, y: Int) -> Unit
) : View(context) {

    private val density = resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(105, 238, 240, 242)
        strokeWidth = density * 1.4f
    }

    private val brassRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(140, 210, 188, 148)
        strokeWidth = density * 1.8f
    }

    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val specularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(180, 255, 255, 255)
    }

    init {
        alpha = baseOpacity
    }

    fun updateAppearance(newSizeDp: Int, newOpacity: Float) {
        this.sizeDp = newSizeDp
        this.baseOpacity = newOpacity
        alpha = newOpacity
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val px = (sizeDp * density).toInt()
        setMeasuredDimension(px, px)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = (w / 2f) - (density * 2.5f)

        if (r <= 0f) return

        // Outer glass body
        val glassShader = RadialGradient(
            cx - (r * 0.35f),
            cy - (r * 0.45f),
            r * 1.3f,
            intArrayOf(
                Color.argb(210, 255, 255, 255),
                Color.argb(90, 238, 240, 242),
                Color.argb(220, 24, 27, 30),
                Color.argb(250, 16, 18, 20)
            ),
            floatArrayOf(0f, 0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        glassPaint.shader = glassShader
        canvas.drawCircle(cx, cy, r, glassPaint)
        canvas.drawCircle(cx, cy, r, rimPaint)
        canvas.drawCircle(cx, cy, r - (density * 1.5f), brassRingPaint)

        // Golden center core
        val coreRadius = 0.36f * r
        val coreShader = RadialGradient(
            cx - (coreRadius * 0.25f),
            cy - (coreRadius * 0.35f),
            coreRadius * 1.1f,
            intArrayOf(
                Color.rgb(243, 230, 203),
                Color.rgb(210, 188, 148),
                Color.rgb(168, 144, 102)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        corePaint.shader = coreShader
        canvas.drawCircle(cx, cy, coreRadius, corePaint)

        // Specular highlight glint
        val glintR = 0.28f * coreRadius
        canvas.drawCircle(cx - (coreRadius * 0.3f), cy - (coreRadius * 0.35f), glintR, specularPaint)
    }

    fun handleTouch(
        event: MotionEvent,
        currentX: Int,
        currentY: Int,
        screenWidth: Int,
        dockEdge: DockEdge
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = currentX
                initialY = currentY
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                alpha = 1.0f
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    isDragging = true
                }
                if (isDragging) {
                    val newX = (initialX + dx).toInt()
                    val newY = (initialY + dy).toInt()
                    onPositionUpdated(newX, newY)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                alpha = baseOpacity
                if (!isDragging) {
                    onBubbleClicked()
                } else {
                    snapToEdge(currentX, currentY, screenWidth, dockEdge)
                }
                return true
            }
            else -> return false
        }
    }

    private fun snapToEdge(
        currX: Int,
        currY: Int,
        screenWidth: Int,
        dockEdge: DockEdge
    ) {
        val bubbleW = width
        val targetX = when (dockEdge) {
            DockEdge.LEFT -> 8
            DockEdge.RIGHT -> (screenWidth - bubbleW - 8).coerceAtLeast(0)
            DockEdge.AUTO -> {
                val midpoint = screenWidth / 2
                if (currX + bubbleW / 2 >= midpoint) {
                    (screenWidth - bubbleW - 8).coerceAtLeast(0)
                } else {
                    8
                }
            }
        }

        val animator = ValueAnimator.ofInt(currX, targetX).apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val animatedX = va.animatedValue as Int
                onPositionUpdated(animatedX, currY)
            }
        }
        animator.start()
        onDragFinished(targetX, currY)
    }
}
