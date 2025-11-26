package com.example.momolabfe.remote.consult.data

import com.google.gson.annotations.SerializedName

data class StartConsultResponse (
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("message") val message: String
)

data class SessionEndResponse (
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("status") val status: String
)