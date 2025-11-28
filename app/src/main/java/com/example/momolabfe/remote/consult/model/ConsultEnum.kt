package com.example.momolabfe.remote.consult.data

import com.google.gson.annotations.SerializedName

enum class MessageRole {
    @SerializedName("USER") USER,
    @SerializedName("AGENT") AGENT
}