package com.example.momolabfe.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AidiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // 오레오 이상에서만 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "momolab_morning_channel"
            val channelName = "아침 투석 알림"
            val descriptionText = "MOMOLAB AIDI의 아침 투석 계획 알림 채널입니다."

            val importance = NotificationManager.IMPORTANCE_HIGH // 헤드업 알림용
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = descriptionText
                enableVibration(true) // 진동
                enableLights(true) // LED (지원 단말)
                setShowBadge(true)
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}