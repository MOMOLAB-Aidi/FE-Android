package com.example.momolabfe.remote.stats.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class Last7DaysStats (
    @SerializedName("points") val points: List<WeightUfPoint>,
    @SerializedName("bp_summary") val bpSummary: BloodPressureSummary
)

data class WeightUfPoint (
    @SerializedName("record_date") val recordDate: LocalDate,
    @SerializedName("weight") val weight: Float,
    @SerializedName("total_uf") val totalUf: Int
)

data class BloodPressureSummary (
    @SerializedName("avg_systolic") val avgSystolic: Float,
    @SerializedName("avg_diastolic") val avgDiastolic: Float,
    @SerializedName("max_systolic") val maxSystolic: Int,
    @SerializedName("max_diastolic") val maxDiastolic: Int,
    @SerializedName("min_systolic") val minSystolic: Int,
    @SerializedName("min_diastolic") val minDiastolic: Int
)