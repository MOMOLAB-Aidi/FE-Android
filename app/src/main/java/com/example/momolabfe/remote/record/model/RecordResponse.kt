package com.example.momolabfe.data.remote.record.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.LocalTime

data class RecordIdResponse (
    @SerializedName("id") val id: Long
)

data class RecordGetResponse (
    @SerializedName("id") val id: Int,
    @SerializedName("record_date") val recordDate: LocalDate,
    @SerializedName("record_dw") val recordDw: DayWeek,
    @SerializedName("weight") val weight: Double,
    @SerializedName("systolic") val systolic: Int,
    @SerializedName("diastolic") val diastolic: Int,
    @SerializedName("fasting_glucose") val fastingGlucose: Int,
    @SerializedName("urine_count") val urineCount: Int,
    @SerializedName("turbidity") val turbidity: Turbidity,
    @SerializedName("notes") val notes: String?,
    @SerializedName("total_uf") val totalUf: Int,
    @SerializedName("gcs_path") val gcsPath: String?,
    @SerializedName("exchanges") val exchanges: List<RecordExchangeGetResponse> = emptyList()
)

data class RecordExchangeGetResponse (
    @SerializedName("id") val id: Int,
    @SerializedName("exchange_no") val exchangeNo: Int,
    @SerializedName("exchange_time") val exchangeTime: LocalTime,
    @SerializedName("drain_volume") val drainVolume: Int,
    @SerializedName("fill_volume") val fillVolume: Int,
    @SerializedName("fill_concentration") val fillConcentration: Double,
    @SerializedName("uf") val uf: Int
)

data class RecordOcrResponse (
    @SerializedName("gcs_path") val gcsPath: String,
    @SerializedName("ocr_data") val ocrData: OcrRecordData
)

data class OcrRecordData (
    @SerializedName("record_date") val recordDate: LocalDate,
    @SerializedName("record_dw") val recordDw: DayWeek,
    @SerializedName("weight") val weight: Double,
    @SerializedName("systolic") val systolic: Int,
    @SerializedName("diastolic") val diastolic: Int,
    @SerializedName("fasting_glucose") val fastingGlucose: Int,
    @SerializedName("urine_count") val urineCount: Int,
    @SerializedName("turbidity") val turbidity: Turbidity,
    @SerializedName("notes") val notes: String?,
    @SerializedName("total_uf") val totalUf: Int,
    @SerializedName("gcs_path") val gcsPath: String,
    @SerializedName("exchanges") val exchanges: List<OcrRecordExchangeData> = emptyList()
)

data class OcrRecordExchangeData (
    @SerializedName("id") val id: Int,
    @SerializedName("exchange_no") val exchangeNo: Int,
    @SerializedName("exchange_time") val exchangeTime: LocalTime,
    @SerializedName("drain_volume") val drainVolume: Int,
    @SerializedName("fill_volume") val fillVolume: Int,
    @SerializedName("fill_concentration") val fillConcentration: Double,
    @SerializedName("uf") val uf: Int
)
