package com.example.momolabfe.remote.consult.model

import com.google.gson.annotations.SerializedName

enum class MessageRole {
    @SerializedName("USER") USER,
    @SerializedName("AGENT") AGENT
}