package com.example.momolabfe.remote.record.model

import com.google.gson.annotations.SerializedName

enum class DayWeek {
    @SerializedName("월") MON,
    @SerializedName("화") TUE,
    @SerializedName("수") WED,
    @SerializedName("목") THU,
    @SerializedName("금") FRI,
    @SerializedName("토") SAT,
    @SerializedName("일") SUN
}

enum class Turbidity {
    @SerializedName("없음") NONE,
    @SerializedName("있음") PRESENT
}