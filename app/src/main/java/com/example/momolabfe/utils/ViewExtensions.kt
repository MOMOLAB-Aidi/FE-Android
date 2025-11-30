package com.example.momolabfe.utils

import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import androidx.fragment.app.Fragment

fun Fragment.dpToPx(dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()
}

// 동그라미 배경
fun circleFill(fillColor: Int): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fillColor)
    }
}