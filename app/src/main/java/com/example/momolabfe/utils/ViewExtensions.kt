package com.example.momolabfe.utils

import android.util.TypedValue
import androidx.fragment.app.Fragment

fun Fragment.dpToPx(dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()
}