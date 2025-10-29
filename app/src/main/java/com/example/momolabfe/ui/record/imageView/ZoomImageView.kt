package com.example.momolabfe.ui.record.imageView

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min

class ZoomImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrixValues = FloatArray(9)
    private val drawMatrix = Matrix()
    private val startPoint = PointF()

    private var minScale = 1f
    private var maxScale = 4f

    private var currentScale = 1f
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val target = (currentScale * scaleFactor).coerceIn(minScale, maxScale)
            val delta = target / currentScale
            drawMatrix.postScale(delta, delta, detector.focusX, detector.focusY)
            imageMatrix = drawMatrix
            currentScale = target
            constrainToBounds()
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
        imageMatrix = drawMatrix
        isClickable = true
    }

    // 이미지/뷰 크기에 맞춰 화면에 꽉 차게 기본 배치 + 스케일 1로 초기화
    fun resetZoom() {

        if (drawable == null) return
        val dWidth = drawable.intrinsicWidth.toFloat()
        val dHeight = drawable.intrinsicHeight.toFloat()
        val vWidth = width.toFloat()
        val vHeight = height.toFloat()
        if (dWidth <= 0f || dHeight <= 0f || vWidth <= 0f || vHeight <= 0f) return

        drawMatrix.reset()

        val scale = min(vWidth / dWidth, vHeight / dHeight)
        val dx = (vWidth - dWidth * scale) / 2f
        val dy = (vHeight - dHeight * scale) / 2f

        drawMatrix.postScale(scale, scale)
        drawMatrix.postTranslate(dx, dy)
        imageMatrix = drawMatrix

        minScale = 1f
        // 초기 스케일을 1로 보고, 실제로는 현재 배치 스케일을 기준으로 상대 확대
        currentScale = 1f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post { resetZoom() }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (drawable == null) return false

        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startPoint.set(event.x, event.y)
                isDragging = true
                parent?.requestDisallowInterceptTouchEvent(true) // 터치 가로채기 금지
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && !scaleDetector.isInProgress) {
                    val dx = event.x - startPoint.x
                    val dy = event.y - startPoint.y
                    startPoint.set(event.x, event.y)

                    // 현재가 최소 스케일일 땐 과도한 드래그 제한
                    if (currentScale > minScale || canDragAtMinScale(dx, dy)) {
                        drawMatrix.postTranslate(dx, dy)
                        imageMatrix = drawMatrix
                        constrainToBounds()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private fun canDragAtMinScale(dx: Float, dy: Float): Boolean {
        // 최소 스케일일 때 좌우/상하로 조금은 움직일 수 있게 하고 싶다면 조정
        return (dx * dx + dy * dy) > 4f
    }

    private fun constrainToBounds() {
        // 이미지가 화면 밖으로 사라지지 않도록 경계 보정
        val rect = drawable?.bounds ?: return

        getImageMatrixValues()
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]
        val scaleX = matrixValues[Matrix.MSCALE_X]
        val scaleY = matrixValues[Matrix.MSCALE_Y]

        val imgW = rect.width() * scaleX
        val imgH = rect.height() * scaleY

        val viewW = width.toFloat()
        val viewH = height.toFloat()

        var dx = 0f
        var dy = 0f

        if (imgW <= viewW) {
            dx = (viewW - imgW) / 2f - transX
        } else {
            if (transX > 0) dx = -transX
            if (transX + imgW < viewW) dx = viewW - (transX + imgW)
        }

        if (imgH <= viewH) {
            dy = (viewH - imgH) / 2f - transY
        } else {
            if (transY > 0) dy = -transY
            if (transY + imgH < viewH) dy = viewH - (transY + imgH)
        }

        if (dx != 0f || dy != 0f) {
            drawMatrix.postTranslate(dx, dy)
            imageMatrix = drawMatrix
        }
    }

    private fun getImageMatrixValues() {
        imageMatrix.getValues(matrixValues)
    }

    fun applyFitAndCenter(drawableWidth: Int, drawableHeight: Int) {
        if (drawableWidth <= 0 || drawableHeight <= 0 || width == 0 || height == 0) return
        // 완전 초기화
        drawMatrix.reset()
        imageMatrix = drawMatrix
        currentScale = 1f

        val vW = width.toFloat()
        val vH = height.toFloat()
        val dW = drawableWidth.toFloat()
        val dH = drawableHeight.toFloat()

        val scale = minOf(vW / dW, vH / dH)
        val dx = (vW - dW * scale) / 2f
        val dy = (vH - dH * scale) / 2f

        drawMatrix.postScale(scale, scale)
        drawMatrix.postTranslate(dx, dy)
        imageMatrix = drawMatrix
    }
}
