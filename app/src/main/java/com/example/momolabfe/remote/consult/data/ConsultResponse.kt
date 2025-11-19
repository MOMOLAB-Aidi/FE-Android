package com.example.momolabfe.remote.consult.data

import com.google.gson.annotations.SerializedName

data class SessionStartResponse (
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("message") val message: String
)

data class ChatResponse (
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("response") val response: String
)

data class SessionEndResponse (
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("status") val status: String
)