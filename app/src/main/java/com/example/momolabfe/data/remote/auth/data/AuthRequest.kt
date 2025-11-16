package com.example.momolabfe.data.remote.login.data

import com.google.gson.annotations.SerializedName

data class LoginRequest (
    @SerializedName("loginId") val loginId: String,
    @SerializedName("password") val password: String
)