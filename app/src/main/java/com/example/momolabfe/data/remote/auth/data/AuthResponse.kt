package com.example.momolabfe.data.remote.auth.data

import com.google.gson.annotations.SerializedName

data class AuthResponse (
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
)