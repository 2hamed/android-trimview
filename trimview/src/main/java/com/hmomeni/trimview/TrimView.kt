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
    var minTrim: Int = 0

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
        canvas.drawText("[", leftAnchor.centerX() - anchorWidth / 2, leftAnchor.centerY() + anchorWidth, bracketPaint)
        canvas.drawText("]", rightAnchor.centerX() - anchorWidth / 2, rightAnchor.centerY() + anchorWidth, bracketPaint)
    }

    private var captured: Captured = Captured.WHOLE

    private var initx = 0f
    private var initrx = 0f
    private var initlx = 0f
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
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
                            && newx >= 0
                        ) {
                            leftAnchor.left = newx
                            leftAnchor.right = leftAnchor.left + anchorWidth
                            mainLine.left = leftAnchor.right
                            invalidate()
                        }
                    }
                    Captured.RIGHT -> {
                        val newx = initrx + dx
                        if (
                            max * (newx - leftAnchor.right) / (measuredWidth - 2 * anchorWidth) >= minTrim
                            && newx + anchorWidth <= measuredWidth
                        ) {
                            rightAnchor.left = newx
                            rightAnchor.right = rightAnchor.left + anchorWidth
                            mainLine.right = rightAnchor.left
                            invalidate()
                        }
                    }
                    Captured.WHOLE -> {
                        if (initrx + dx + anchorWidth <= measuredWidth && initlx + dx >= 0) {
                            leftAnchor.left = initlx + dx
                            leftAnchor.right = leftAnchor.left + anchorWidth
                            mainLine.left = leftAnchor.right

                            rightAnchor.left = initrx + dx
                            rightAnchor.right = rightAnchor.left + anchorWidth
                            mainLine.right = rightAnchor.left
                            invalidate()
                        }
                    }
                }
            }
        }

        return true
    }

    enum class Captured {
        LEFT, RIGHT, WHOLE
    }

    private fun dpToPx(dp: Int) = (dp * Resources.getSystem().displayMetrics.density).toInt()
}