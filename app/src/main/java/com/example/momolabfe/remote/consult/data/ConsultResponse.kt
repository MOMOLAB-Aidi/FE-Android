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

data class GetConsultResponse (
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("message_count") val messageCount: Int
)

data class ConsultDetailResponse (
    @SerializedName("role") val role: MessageRole,
    @SerializedName("content") val content: String,
    @SerializedName("created_at") val createdAt: String
)