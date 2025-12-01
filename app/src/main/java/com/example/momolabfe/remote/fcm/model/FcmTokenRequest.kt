package com.example.momolabfe.remote.fcm.model

import com.google.gson.annotations.SerializedName

data class FcmTokenRequest(
    @SerializedName("fcmToken") val fcmToken: String
)
