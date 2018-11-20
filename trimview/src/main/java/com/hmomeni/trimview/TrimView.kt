package com.hmomeni.trimview

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class TrimView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#99ffffff")
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    private val whitePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }
    private val greenPaint = Paint().apply {
        color = Color.GREEN
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    private val bracketPaint = Paint().apply {
        color = Color.BLACK
        textSize = dpToPx(14).toFloat()
        isAntiAlias = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
    }
    private val bgLine = RectF()
    private val mainLine = RectF()
    private val progressLine = RectF()
    private val leftAnchor = RectF()
    private val rightAnchor = RectF()
    private val mainLineHeight = dpToPx(8)
    private val anchorWidth = dpToPx(24).toFloat()
    private val radius = dpToPx(5).toFloat()

    var onTrimChangeListener: TrimChangeListener? = null

    var max: Int = 100

    var progress = 0
        set(value) {
            field = value
            calculateProgress()
            invalidate()
        }

    var trim: Int = max / 3
        set(value) {
            field = value
            calculateLeftandRight()
        }
    var trimStart: Int = 0
    var minTrim: Int = 0
    var maxTrim: Int = max

    private var maxPx = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, anchorWidth.toInt())
        if (measuredWidth > 0 && measuredHeight > 0) {
            maxPx = (measuredWidth - (2 * anchorWidth)).toInt()
            bgLine.set(
                anchorWidth,
                (measuredHeight / 2f) - (mainLineHeight / 2),
                anchorWidth + maxPx,
                (measuredHeight / 2f) + (mainLineHeight / 2)
            )
            mainLine.set(
                anchorWidth,
                (measuredHeight / 2f) - (mainLineHeight / 2),
                anchorWidth + (trim * maxPx / max).toFloat(),
                (measuredHeight / 2f) + (mainLineHeight / 2)
            )
            progressLine.set(
                anchorWidth,
                (measuredHeight / 2f) - (mainLineHeight / 2),
                anchorWidth + (trim * progress / 100f),
                (measuredHeight / 2f) + (mainLineHeight / 2)
            )
            leftAnchor.set(0f, 0f, anchorWidth, measuredHeight.toFloat())
            rightAnchor.set(
                mainLine.right,
                0f,
                mainLine.right + anchorWidth,
                measuredHeight.toFloat()
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(bgLine, radius, radius, bgPaint)
        canvas.drawRect(mainLine, whitePaint)
        canvas.drawRoundRect(leftAnchor, radius, radius, whitePaint)
        canvas.drawRoundRect(rightAnchor, radius, radius, whitePaint)
        canvas.drawRect(progressLine, greenPaint)
        canvas.drawText("[", leftAnchor.left + anchorWidth * 2 / 5, leftAnchor.bottom - anchorWidth / 3, bracketPaint)
        canvas.drawText("]", rightAnchor.left + anchorWidth * 2 / 5, rightAnchor.bottom - anchorWidth / 3, bracketPaint)
    }

    private var captured: Captured = Captured.WHOLE

    private var initTrim = 0
    private var initTrimStart = 0
    private var initx = 0f
    private var initrx = 0f
    private var initlx = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initTrim = trim
                initTrimStart = trimStart
                initx = event.x
                captured = when {
                    rightAnchor.contains(event.x, event.y) -> {
                        initrx = rightAnchor.left
                        Captured.RIGHT
                    }
                    leftAnchor.contains(event.x, event.y) -> {
                        initlx = leftAnchor.left
                        Captured.LEFT
                    }
                    else -> {
                        initrx = rightAnchor.left
                        initlx = leftAnchor.left
                        Captured.WHOLE
                    }
                }
                onTrimChangeListener?.onDragStarted(trimStart, trim)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - initx
                when (captured) {
                    Captured.LEFT -> {
                        val newx = initlx + dx
                        val newTrimStart = initTrimStart + (dx * max / (maxPx)).toInt()
                        val newTrim = initTrim - newTrimStart + initTrimStart
                        if (
                            newTrim in minTrim..maxTrim
                            && newx >= 0
                        ) {
                            trimStart = newTrimStart
                            trim = newTrim
                            calculateLeftandRight()
                            onTrimChangeListener?.onLeftEdgeChanged(trimStart, trim)
                        }
                    }
                    Captured.RIGHT -> {
                        val newx = initrx + dx
                        val newTrim = initTrim + (dx * max / (maxPx)).toInt()
                        if (
                            newTrim in minTrim..maxTrim
                            && newx + anchorWidth <= measuredWidth
                        ) {
                            trim = newTrim
                            if (progress > trim) {
                                progress = trim
                            }
                            calculateLeftandRight()
                            onTrimChangeListener?.onRightEdgeChanged(trimStart, trim)
                        }
                    }
                    Captured.WHOLE -> {
                        if (initrx + dx + anchorWidth <= measuredWidth && initlx + dx >= 0) {
                            trimStart = initTrimStart + (dx * max / (measuredWidth - 2 * anchorWidth)).toInt()
                            calculateLeftandRight()
                            onTrimChangeListener?.onRangeChanged(trimStart, trim)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                onTrimChangeListener?.onDragStopped(trimStart, trim)
            }
        }

        return true
    }

    private fun calculateLeftandRight(invalidate: Boolean = true) {
        val trimStartPx = trimStart * maxPx / max
        val trimPx = trim * maxPx / max

        leftAnchor.left = trimStartPx.toFloat()
        leftAnchor.right = leftAnchor.left + anchorWidth
        mainLine.left = leftAnchor.right

        rightAnchor.left = (trimStartPx + trimPx).toFloat() + anchorWidth
        rightAnchor.right = rightAnchor.left + anchorWidth
        mainLine.right = rightAnchor.left

        calculateProgress(false)

        if (invalidate)
            invalidate()
    }

    private fun calculateProgress(invalidate: Boolean = true) {
        val progressPx = progress * maxPx / max
        progressLine.left = mainLine.left
        progressLine.right = progressLine.left + progressPx

        if (invalidate)
            invalidate()
    }

    enum class Captured {
        LEFT, RIGHT, WHOLE
    }

    abstract class TrimChangeListener {
        open fun onDragStarted(trimStart: Int, trim: Int) {}
        open fun onLeftEdgeChanged(trimStart: Int, trim: Int) {}
        open fun onRightEdgeChanged(trimStart: Int, trim: Int) {}
        open fun onRangeChanged(trimStart: Int, trim: Int) {}
        open fun onDragStopped(trimStart: Int, trim: Int) {}
    }

    private fun dpToPx(dp: Int) = (dp * Resources.getSystem().displayMetrics.density).toInt()

}