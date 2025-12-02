package com.example.momolabfe.firebase

import android.util.Log
import com.example.momolabfe.remote.fcm.repository.FcmRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MomolabFirebaseService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmRepository: FcmRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "새 FCM 토큰: $token")

        // 로컬에 저장
        saveFcmTokenLocal(token)

        // 서버에 등록
        CoroutineScope(Dispatchers.IO).launch {
            fcmRepository.registerFcmToken(token)
                .onSuccess { Log.d("FCM", "서버에 FCM 토큰 등록 성공") }
                .onFailure { e -> Log.e("FCM", "서버에 FCM 토큰 등록 실패", e) }
        }
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