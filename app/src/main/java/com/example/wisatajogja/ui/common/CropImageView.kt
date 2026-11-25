package com.example.wisatajogja.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import com.example.wisatajogja.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Kanvas crop sederhana: gambar selalu menutupi frame crop, pengguna dapat
 * menggeser dan mencubit untuk memilih bagian yang akan diunggah.
 */
class CropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private var aspectRatio = 1f
    private val imageMatrix = Matrix()
    private val cropRect = RectF()
    private var minScale = 1f
    private var maxScale = 5f
    private var lastX = 0f
    private var lastY = 0f

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x990C2E26.toInt() }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        color = ContextCompat.getColor(context, R.color.cream_light)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(0.7f)
        color = 0x55FBF7EE
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.gold)
    }

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = true

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor.coerceIn(0.7f, 1.4f)
                imageMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
                constrainTransform()
                invalidate()
                return true
            }
        },
    )

    init {
        isClickable = true
        isFocusable = true
        contentDescription = "Area atur gambar. Geser dan cubit untuk memperbesar."
        setBackgroundColor(ContextCompat.getColor(context, R.color.cream_deep))
    }

    fun setAspectRatio(ratio: Float) {
        aspectRatio = ratio.coerceAtLeast(0.1f)
        if (width > 0 && height > 0) {
            setupCropRect()
            fitImageToCrop()
        }
    }

    fun setImage(bitmap: Bitmap) {
        this.bitmap = bitmap
        if (width > 0 && height > 0) {
            setupCropRect()
            fitImageToCrop()
        }
        invalidate()
    }

    fun zoomBy(factor: Float) {
        if (bitmap == null || cropRect.isEmpty) return
        imageMatrix.postScale(factor, factor, cropRect.centerX(), cropRect.centerY())
        constrainTransform()
        invalidate()
    }

    fun hasImage(): Boolean = bitmap != null

    fun release() {
        bitmap?.recycle()
        bitmap = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupCropRect()
        if (bitmap != null) fitImageToCrop()
    }

    private fun setupCropRect() {
        val margin = dp(18f)
        val availableWidth = (width - margin * 2).coerceAtLeast(1f)
        val availableHeight = (height - margin * 2).coerceAtLeast(1f)
        var cropWidth = availableWidth
        var cropHeight = cropWidth / aspectRatio
        if (cropHeight > availableHeight) {
            cropHeight = availableHeight
            cropWidth = cropHeight * aspectRatio
        }
        val left = (width - cropWidth) / 2f
        val top = (height - cropHeight) / 2f
        cropRect.set(left, top, left + cropWidth, top + cropHeight)
    }

    private fun fitImageToCrop() {
        val source = bitmap ?: return
        if (cropRect.isEmpty) return
        val baseScale = max(cropRect.width() / source.width, cropRect.height() / source.height)
        minScale = baseScale
        maxScale = baseScale * 5f
        imageMatrix.reset()
        imageMatrix.postScale(baseScale, baseScale)
        imageMatrix.postTranslate(
            cropRect.centerX() - source.width * baseScale / 2f,
            cropRect.centerY() - source.height * baseScale / 2f,
        )
        constrainTransform()
        invalidate()
    }

    private fun constrainTransform() {
        val source = bitmap ?: return
        if (cropRect.isEmpty) return
        val values = FloatArray(9)
        imageMatrix.getValues(values)
        val currentScale = values[Matrix.MSCALE_X]
        when {
            currentScale < minScale -> {
                val factor = minScale / currentScale
                imageMatrix.postScale(factor, factor, cropRect.centerX(), cropRect.centerY())
            }
            currentScale > maxScale -> {
                val factor = maxScale / currentScale
                imageMatrix.postScale(factor, factor, cropRect.centerX(), cropRect.centerY())
            }
        }
        val mapped = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        imageMatrix.mapRect(mapped)
        var dx = 0f
        var dy = 0f
        if (mapped.left > cropRect.left) dx = cropRect.left - mapped.left
        if (mapped.right < cropRect.right) dx = cropRect.right - mapped.right
        if (mapped.top > cropRect.top) dy = cropRect.top - mapped.top
        if (mapped.bottom < cropRect.bottom) dy = cropRect.bottom - mapped.bottom
        if (abs(dx) > 0.01f || abs(dy) > 0.01f) imageMatrix.postTranslate(dx, dy)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bitmap == null) return false
        parent?.requestDisallowInterceptTouchEvent(true)
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    imageMatrix.postTranslate(dx, dy)
                    constrainTransform()
                    invalidate()
                }
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val source = bitmap ?: return
        canvas.drawColor(ContextCompat.getColor(context, R.color.cream_deep))
        canvas.drawBitmap(source, imageMatrix, null)
        if (cropRect.isEmpty) return

        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, dimPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), dimPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, dimPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, dimPaint)

        canvas.drawRect(cropRect, borderPaint)
        val thirdW = cropRect.width() / 3f
        val thirdH = cropRect.height() / 3f
        for (i in 1..2) {
            val x = cropRect.left + thirdW * i
            val y = cropRect.top + thirdH * i
            canvas.drawLine(x, cropRect.top, x, cropRect.bottom, gridPaint)
            canvas.drawLine(cropRect.left, y, cropRect.right, y, gridPaint)
        }

        val handle = dp(20f)
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left + handle, cropRect.top, handlePaint)
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left, cropRect.top + handle, handlePaint)
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right - handle, cropRect.top, handlePaint)
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right, cropRect.top + handle, handlePaint)
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left + handle, cropRect.bottom, handlePaint)
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left, cropRect.bottom - handle, handlePaint)
        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right - handle, cropRect.bottom, handlePaint)
        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right, cropRect.bottom - handle, handlePaint)
    }

    fun createCroppedFile(context: Context): File? {
        val source = bitmap ?: return null
        if (cropRect.isEmpty || width <= 0 || height <= 0) return null
        return runCatching {
            val viewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(viewBitmap)
            canvas.drawColor(ContextCompat.getColor(context, R.color.cream_deep))
            canvas.drawBitmap(source, imageMatrix, null)

            val left = cropRect.left.roundToInt().coerceIn(0, width - 1)
            val top = cropRect.top.roundToInt().coerceIn(0, height - 1)
            val cropWidth = cropRect.width().roundToInt().coerceAtMost(width - left)
            val cropHeight = cropRect.height().roundToInt().coerceAtMost(height - top)
            val cropped = Bitmap.createBitmap(viewBitmap, left, top, cropWidth, cropHeight)
            val outputWidth = if (aspectRatio > 1.3f) 1280 else 1080
            val outputHeight = (outputWidth / aspectRatio).roundToInt()
            val scaled = Bitmap.createScaledBitmap(cropped, outputWidth, outputHeight, true)
            val output = File.createTempFile("crop_", ".jpg", context.cacheDir)
            FileOutputStream(output).use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            viewBitmap.recycle()
            if (cropped !== viewBitmap) cropped.recycle()
            if (scaled !== cropped) scaled.recycle()
            output
        }.getOrNull()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
