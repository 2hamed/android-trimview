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
    val anchorWidth = dpToPx(24).toFloat()
    private val radius = dpToPx(5).toFloat()

    var progress = 0
        set(value) {
            if (value > 100) {
                throw RuntimeException("progress must not exceed 100")
            }
            field = value
            progressLine.right = anchorWidth + (trimWidth * progress / 100f)
            invalidate()
        }

    var trimWidth: Int = dpToPx(150)
        set(value) {
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, anchorWidth.toInt())
        if (measuredWidth > 0 && measuredHeight > 0) {
            bgLine.set(
                0f,
                (measuredHeight / 2f) - (mainLineHeight / 2),
                measuredWidth.toFloat(),
                (measuredHeight / 2f) + (mainLineHeight / 2)
            )
            mainLine.set(
                anchorWidth,
                (measuredHeight / 2f) - (mainLineHeight / 2),
                trimWidth - anchorWidth,
                (measuredHeight / 2f) + (mainLineHeight / 2)
            )
            progressLine.set(
                anchorWidth,
                (measuredHeight / 2f) - (mainLineHeight / 2),
                anchorWidth + (trimWidth * progress / 100f),
                (measuredHeight / 2f) + (mainLineHeight / 2)
            )
            leftAnchor.set(0f, 0f, measuredHeight.toFloat(), measuredHeight.toFloat())
            rightAnchor.set(
                trimWidth - anchorWidth,
                0f,
                trimWidth.toFloat(),
                measuredHeight.toFloat()
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(bgLine, bgPaint)
        canvas.drawRect(mainLine, whitePaint)
        canvas.drawRoundRect(leftAnchor, radius, radius, whitePaint)
        canvas.drawRoundRect(rightAnchor, radius, radius, whitePaint)
        canvas.drawRect(progressLine, greenPaint)
        canvas.drawText("[", leftAnchor.centerX() - anchorWidth / 2, leftAnchor.centerY() + anchorWidth, bracketPaint)
        canvas.drawText("]", rightAnchor.centerX() - anchorWidth / 2, rightAnchor.centerY() + anchorWidth, bracketPaint)
    }

    var captured: Captured = Captured.WHOLE

    var initx = 0f
    var initrx = 0f
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
                        initrx = leftAnchor.left
                        Captured.LEFT
                    }
                    else -> Captured.WHOLE
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - initx
                val newx = initrx + dx
                when (captured) {
                    Captured.LEFT -> {
                        leftAnchor.left = newx
                        leftAnchor.right = leftAnchor.left + anchorWidth
                        invalidate()
                    }
                    Captured.RIGHT -> {
                        rightAnchor.left = newx
                        rightAnchor.right = rightAnchor.left + anchorWidth
                        invalidate()
                    }
                    Captured.WHOLE -> {

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