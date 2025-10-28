package com.example.momolabfe.ui.record

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

// 가로=세로 정사각형으로 강제
class SquareImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}
