package com.example.momolabfe.data.remote.record.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.LocalTime


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
    @SerializedName("total_uf") val totalUf: Int? = null,
)

data class RecordExchangeCreateRequest (
    @SerializedName("exchange_time") val exchangeTime: LocalTime,
    @SerializedName("drain_volume") val drainVolume: Int,
    @SerializedName("fill_volume") val fillVolume: Int,
    @SerializedName("fill_concentration") val fillConcentration: Double,
    @SerializedName("uf") val uf: Int

)