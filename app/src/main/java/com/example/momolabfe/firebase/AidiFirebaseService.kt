package com.example.momolabfe.firebase

import android.util.Log
import com.example.momolabfe.remote.fcm.repository.FcmRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AidiFirebaseService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmRepository: FcmRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "새 FCM 토큰: $token")

        // 로컬에 저장
        saveFcmTokenLocal(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "푸시 수신 from=${remoteMessage.from}")

        remoteMessage.notification?.let {
            val title = it.title ?: "MOMOLAB"
            val body = it.body ?: ""
            Log.d("FCM", "알림 수신 title=$title, body=$body")
        }
    }

    private fun saveFcmTokenLocal(token: String) {
        val prefs = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }
}