package com.example.momolabfe.data.remote.record.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class GetCalendarResponse(
    @SerializedName("date") val date: LocalDate,
    @SerializedName("hasSchedule") val hasSchedule: Boolean
)