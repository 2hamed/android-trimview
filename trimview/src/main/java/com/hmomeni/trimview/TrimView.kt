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
    private val glassPaint = Paint().apply {
        color = Color.parseColor("#33ffffff")
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

    var onTrimChangeListener: TrimView.TrimChangeListener? = null

    var max: Int = 100

    var progress = 0
        set(value) {
            if (value > max) {
                throw RuntimeException("progress must not exceed max($max)")
            }
            field = value
            progressLine.right = anchorWidth + (trim * progress / 100f)
            invalidate()
        }

    var trim: Int = max / 3
        set(value) {
            field = value
            mainLine.right = trim.toFloat() * measuredWidth.toFloat() / max.toFloat()
            rightAnchor.left = mainLine.right + anchorWidth
            invalidate()
        }
    var trimStart: Int = 0
    var minTrim: Int = 0
    var maxTrim: Int = max

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, anchorWidth.toInt())
        if (measuredWidth > 0 && measuredHeight > 0) {
            bgLine.set(
                anchorWidth,
                (measuredHeight / 2f) - (mainLineHeight / 2),
                measuredWidth.toFloat() - anchorWidth,
                (measuredHeight / 2f) + (mainLineHeight / 2)
            )
            mainLine.set(
                anchorWidth,
                (measuredHeight / 2f) - (mainLineHeight / 2),
                trim.toFloat() * (measuredWidth.toFloat() - anchorWidth) / max.toFloat(),
                (measuredHeight / 2f) + (mainLineHeight / 2)
            )
            progressLine.set(
                anchorWidth,
                (measuredHeight / 2f) - (mainLineHeight / 2),
                anchorWidth + (trim * progress / 100f),
                (measuredHeight / 2f) + (mainLineHeight / 2)
            )
            leftAnchor.set(0f, 0f, measuredHeight.toFloat(), measuredHeight.toFloat())
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
        canvas.drawRoundRect(leftAnchor, radius, radius, glassPaint)
        canvas.drawRoundRect(rightAnchor, radius, radius, glassPaint)
        canvas.drawRect(progressLine, greenPaint)
//        canvas.drawText("[", leftAnchor.centerX() - anchorWidth / 2, leftAnchor.centerY() + anchorWidth, bracketPaint)
//        canvas.drawText("]", rightAnchor.centerX() - anchorWidth / 2, rightAnchor.centerY() + anchorWidth, bracketPaint)
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
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - initx
                when (captured) {
                    Captured.LEFT -> {
                        val newx = initlx + dx
                        if (
                            max * (rightAnchor.left - newx - anchorWidth) / (measuredWidth - 2 * anchorWidth) >= minTrim
                            && max * (rightAnchor.left - newx - anchorWidth) / (measuredWidth - 2 * anchorWidth) <= maxTrim
                            && newx >= 0
                        ) {
                            trimStart = initTrimStart + (dx * max / (measuredWidth - 2 * anchorWidth)).toInt()
                            trim = initTrim - trimStart + initTrimStart
                            calculateLeftandRight()
                            onTrimChangeListener?.onLeftEdgeChanged(trimStart, trim)
                        }
                    }
                    Captured.RIGHT -> {
                        val newx = initrx + dx
                        if (
                            max * (newx - leftAnchor.right) / (measuredWidth - 2 * anchorWidth) >= minTrim
                            && max * (newx - leftAnchor.right) / (measuredWidth - 2 * anchorWidth) <= maxTrim
                            && newx + anchorWidth <= measuredWidth
                        ) {
                            trim = initTrim + (dx * max / (measuredWidth - 2 * anchorWidth)).toInt()
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
        }

        return true
    }

    private fun calculateLeftandRight() {
        val trimStartPx = trimStart * (measuredWidth - 2 * anchorWidth) / max
        val trimPx = trim * (measuredWidth - 2 * anchorWidth) / max
        leftAnchor.left = trimStartPx
        leftAnchor.right = leftAnchor.left + anchorWidth
        mainLine.left = leftAnchor.right

        rightAnchor.left = trimStartPx + trimPx
        rightAnchor.right = rightAnchor.left + anchorWidth
        mainLine.right = rightAnchor.left

        invalidate()

    }

    enum class Captured {
        LEFT, RIGHT, WHOLE
    }

    abstract class TrimChangeListener {
        open fun onLeftEdgeChanged(trimStart: Int, trim: Int) {}
        open fun onRightEdgeChanged(trimStart: Int, trim: Int) {}
        open fun onRangeChanged(trimStart: Int, trim: Int) {}
    }

    private fun dpToPx(dp: Int) = (dp * Resources.getSystem().displayMetrics.density).toInt()
}