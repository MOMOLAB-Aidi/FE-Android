package com.example.momolabfe.remote.record.data

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class GetCalendarResponse(
    @SerializedName("date") val date: LocalDate,
    @SerializedName("hasSchedule") val hasSchedule: Boolean
)