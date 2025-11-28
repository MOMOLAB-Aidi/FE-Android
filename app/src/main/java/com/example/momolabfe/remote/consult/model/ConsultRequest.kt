package com.example.momolabfe.remote.consult.model

import com.google.gson.annotations.SerializedName

data class ChatRequest (
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("message") val message: String
)

data class SessionEndRequest (
    @SerializedName("session_id") val sessionId: String
)