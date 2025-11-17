package com.example.momolabfe.data.remote.user.data

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class MyPageResponse (
    @SerializedName("loginId") val loginId: String,
    @SerializedName("recordStartDate") val recordStartDate: LocalDate,
    @SerializedName("recordPeriod") val recordPeriod: String
)