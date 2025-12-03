package com.example.momolabfe.remote.consult.model

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
    @SerializedName("ended_at") val endedAt: String,
    @SerializedName("first_user_question") val firstUserQuestion: String?,
    @SerializedName("summary") val summary: String?
)

data class ConsultDetailResponse (
    @SerializedName("role") val role: MessageRole,
    @SerializedName("content") val content: String,
    @SerializedName("created_at") val createdAt: String
)

data class ConsultSessionSummaryRow (
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("summary") val summary: String
)