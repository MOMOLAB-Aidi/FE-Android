package com.example.momolabfe.remote.auth.data

import com.google.gson.annotations.SerializedName

data class LoginResponse (
    @SerializedName("tokens") val tokens: TokenResponse
)

data class TokenResponse (
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
)