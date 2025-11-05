package com.example.momolabfe.data.remote.record.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate


data class RecordCreateRequest (
    @SerializedName("record_date") val recordDate: LocalDate,
    @SerializedName("record_dw") val recordDw: DayWeek,
    @SerializedName("weight") val weight: Double,
    @SerializedName("systolic") val systolic: Int,
    @SerializedName("diastolic") val diastolic: Int,
    @SerializedName("fasting_glucose") val fastingGlucose: Int,
    @SerializedName("urine_count") val urineCount: Int,
    @SerializedName("turbidity") val turbidity: Turbidity,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("total_uf") val totalUf: Int,
)