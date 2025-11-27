package com.example.momolabfe.ui.record.data

import android.os.Parcelable
import com.example.momolabfe.remote.record.data.DayWeek
import com.example.momolabfe.remote.record.data.Turbidity
import kotlinx.parcelize.Parcelize

// 화면 간 전달용
@Parcelize
data class RecordCommonDraft(
    val recordDate: String,
    val recordDw: DayWeek,
    val weight: Double,
    val systolic: Int,
    val diastolic: Int,
    val fastingGlucose: Int,
    val urineCount: Int,
    val turbidity: Turbidity,
    val notes: String? = null,
    val gcsPath: String? = null
) : Parcelable