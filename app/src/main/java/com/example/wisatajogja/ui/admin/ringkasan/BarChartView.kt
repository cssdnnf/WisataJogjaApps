package com.example.wisatajogja.ui.admin.ringkasan

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.ChartPoint
import kotlin.math.max

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.green) }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.gold) }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.line)
        strokeWidth = dp(0.7f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.muted)
        textSize = sp(8f)
        textAlign = Paint.Align.CENTER
    }

    private var points: List<ChartPoint> = emptyList()
    private var progress = 1f
    private var animator: ValueAnimator? = null

    fun setData(data: List<ChartPoint>) {
        points = data
        animator?.cancel()
        if (data.isEmpty()) {
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 620
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val left = dp(2f)
        val right = width - dp(2f)
        val top = dp(8f)
        val bottom = height - dp(24f)
        val chartHeight = bottom - top
        val maxValue = max(1, points.maxOf { it.visits })

        for (i in 0..2) {
            val y = top + chartHeight * i / 2f
            canvas.drawLine(left, y, right, y, gridPaint)
        }

        val slot = (right - left) / points.size
        val barWidth = slot * 0.52f
        val radius = dp(4f)
        points.forEachIndexed { index, point ->
            val ratio = point.visits.toFloat() / maxValue
            val barHeight = chartHeight * ratio * progress
            val x = left + slot * index + (slot - barWidth) / 2f
            val y = bottom - barHeight
            val paint = if (index == points.lastIndex) highlightPaint else barPaint
            canvas.drawRoundRect(RectF(x, y, x + barWidth, bottom), radius, radius, paint)
            val showLabel = index == points.lastIndex || (index % 2 == 0 && index < points.lastIndex - 1)
            if (showLabel) {
                canvas.drawText(point.label, x + barWidth / 2f, height - dp(6f), labelPaint)
            }
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
