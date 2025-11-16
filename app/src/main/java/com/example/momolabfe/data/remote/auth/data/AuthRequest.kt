package com.example.momolabfe.data.remote.auth.data

import com.google.gson.annotations.SerializedName

data class AuthRequest (
    @SerializedName("loginId") val loginId: String,
    @SerializedName("password") val password: String
)