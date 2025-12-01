package com.example.momolabfe.remote.user.model

import com.google.gson.annotations.SerializedName

data class UpdatePassword (
    @SerializedName("currentPassword") val currentPassword: String,
    @SerializedName("newPassword") val newPassword: String,
    @SerializedName("newPasswordCheck") val newPasswordCheck: String
)