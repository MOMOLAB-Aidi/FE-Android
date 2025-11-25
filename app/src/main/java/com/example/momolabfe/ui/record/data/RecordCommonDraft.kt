package com.example.momolabfe.ui.record.data

import com.example.momolabfe.remote.record.model.DayWeek
import com.example.momolabfe.remote.record.model.Turbidity
import java.io.Serializable
import java.time.LocalDate

// 화면 간 전달용
data class RecordCommonDraft(
    val recordDate: LocalDate,
    val recordDw: DayWeek,
    val weight: Double,
    val systolic: Int,
    val diastolic: Int,
    val fastingGlucose: Int,
    val urineCount: Int,
    val turbidity: Turbidity,
    val notes: String? = null,
    val gcsPath: String? = null
) : Serializable