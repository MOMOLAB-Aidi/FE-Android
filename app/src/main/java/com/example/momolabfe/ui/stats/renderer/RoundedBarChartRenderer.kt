package com.example.momolabfe.ui.stats.renderer

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler

class RoundedBarChartRenderer(
    chart: BarDataProvider,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : BarChartRenderer(chart, animator, viewPortHandler) {

    override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {

        // NPE / index 방어
        val buffers = mBarBuffers ?: return
        if (index < 0 || index >= buffers.size) return

        val barBuffer = mBarBuffers[index]

        val trans = mChart.getTransformer(dataSet.axisDependency)

        mBarBorderPaint.color = dataSet.barBorderColor
        mBarBorderPaint.strokeWidth = Utils.convertDpToPixel(dataSet.barBorderWidth)
        mBarBorderPaint.style = android.graphics.Paint.Style.STROKE

        val drawBorder = dataSet.barBorderWidth > 0f

        val phaseX = mAnimator.phaseX
        val phaseY = mAnimator.phaseY

        barBuffer.setPhases(phaseX, phaseY)
        barBuffer.setDataSet(index)
        barBuffer.setInverted(mChart.isInverted(dataSet.axisDependency))
        barBuffer.setBarWidth(mChart.barData.barWidth)

        barBuffer.feed(dataSet)
        trans.pointValuesToPixel(barBuffer.buffer)

        val radius = Utils.convertDpToPixel(6f) // 모서리 둥근 정도

        val isSingleColor = dataSet.colors.size == 1
        if (isSingleColor && dataSet is BarDataSet) {
            mRenderPaint.color = dataSet.color
        }

        var j = 0
        while (j < barBuffer.size()) {
            val left = barBuffer.buffer[j]
            val top = barBuffer.buffer[j + 1]
            val right = barBuffer.buffer[j + 2]
            val bottom = barBuffer.buffer[j + 3]

            // 화면 밖이면 스킵
            if (!mViewPortHandler.isInBoundsLeft(right)) {
                j += 4
                continue
            }
            if (!mViewPortHandler.isInBoundsRight(left)) break

            val entryIndex = j / 4
            if (entryIndex < 0 || entryIndex >= dataSet.entryCount) {
                j += 4
                continue
            }

            val entry = dataSet.getEntryForIndex(entryIndex) as BarEntry
            val isNegative = entry.y < 0f    // y값으로 음수/양수 판정

            if (!isSingleColor) {
                mRenderPaint.color = dataSet.getColor(entryIndex)
            }

            val rect = RectF(left, top, right, bottom)

            val radii = if (isNegative) {
                // 음수 막대: 아래쪽만 둥글게
                floatArrayOf(
                    0f, 0f, // top-left
                    0f, 0f, // top-right
                    radius, radius, // bottom-right
                    radius, radius // bottom-left
                )
            } else {
                // 양수 막대: 위쪽만 둥글게
                floatArrayOf(
                    radius, radius, // top-left
                    radius, radius, // top-right
                    0f, 0f, // bottom-right
                    0f, 0f // bottom-left
                )
            }

            val path = Path()
            path.addRoundRect(rect, radii, Path.Direction.CW)

            // 채우기
            c.drawPath(path, mRenderPaint)

            // 테두리
            if (drawBorder) {
                c.drawPath(path, mBarBorderPaint)
            }

            j += 4
        }
    }
}