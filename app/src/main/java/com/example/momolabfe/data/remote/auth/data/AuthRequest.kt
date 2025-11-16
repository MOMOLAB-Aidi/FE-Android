package com.example.momolabfe.data.remote.auth.data

import com.google.gson.annotations.SerializedName

data class LoginRequest (
    @SerializedName("loginId") val loginId: String,
    @SerializedName("password") val password: String
)

data class TokenRequest (
    @SerializedName("refreshToken") val refreshToken: String
)