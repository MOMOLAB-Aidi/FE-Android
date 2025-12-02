package com.example.momolabfe.remote.education.model

import com.google.gson.annotations.SerializedName

data class EducationResponse (
    @SerializedName("tipId") val tipId: Long,
    @SerializedName("body") val body: String
)