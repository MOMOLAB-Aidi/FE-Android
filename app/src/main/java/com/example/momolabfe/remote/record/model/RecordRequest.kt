package com.example.momolabfe.remote.record.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable
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
    @SerializedName("gcs_path") val gcsPath: String? = null,
    @SerializedName("exchanges") val exchanges: List<RecordExchangeCreateRequest>
) : Serializable

data class RecordExchangeCreateRequest (
    @SerializedName("exchange_no") val exchangeNo: Int? = null,
    @SerializedName("exchange_time") val exchangeTime: String,
    @SerializedName("drain_volume") val drainVolume: Int,
    @SerializedName("fill_volume") val fillVolume: Int,
    @SerializedName("fill_concentration") val fillConcentration: Double,
    @SerializedName("uf") val uf: Int
)

data class RecordUpdateRequest (
    @SerializedName("record_date") val recordDate: LocalDate? = null,
    @SerializedName("record_dw") val recordDw: DayWeek? = null,
    @SerializedName("weight") val weight: Double? = null,
    @SerializedName("systolic") val systolic: Int? = null,
    @SerializedName("diastolic") val diastolic: Int? = null,
    @SerializedName("fasting_glucose") val fastingGlucose: Int? = null,
    @SerializedName("urine_count") val urineCount: Int? = null,
    @SerializedName("turbidity") val turbidity: Turbidity? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("total_uf") val totalUf: Int? = null,
    @SerializedName("exchanges") val exchanges: List<RecordExchangeUpdateRequest>? = null,
)

data class RecordExchangeUpdateRequest (
    @SerializedName("id") val id: Long? = null,
    @SerializedName("exchange_time") val exchangeTime: String? = null,
    @SerializedName("drain_volume") val drainVolume: Int? = null,
    @SerializedName("fill_volume") val fillVolume: Int? = null,
    @SerializedName("fill_concentration") val fillConcentration: Double? = null,
    @SerializedName("uf") val uf: Int? = null
)
