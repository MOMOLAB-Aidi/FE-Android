package com.example.momolabfe.data.remote.record.model

import com.google.gson.annotations.SerializedName

data class GetCalendarResponse(
    @SerializedName("date") val date: String,
    @SerializedName("hasSchedule") var hasSchedule: Boolean
)