package com.example.momolabfe.firebase

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.momolabfe.BuildConfig
import com.example.momolabfe.R
import com.example.momolabfe.remote.fcm.repository.FcmRepository
import com.example.momolabfe.ui.main.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class AidiFirebaseService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmRepository: FcmRepository

    private val notificationIdCounter = AtomicInteger(0)

    companion object {
        private const val FCM_PREFS_NAME = "fcm_prefs"
        private const val FCM_TOKEN_KEY = "fcm_token"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (BuildConfig.DEBUG) {
            Log.d("FCM", "새 FCM 토큰(앞 10자리): ${token.take(10)}...")
        }

        // 로컬에 저장
        saveFcmTokenLocal(token)

        // 서버에 등록 (비동기)
        CoroutineScope(Dispatchers.IO).launch {
            fcmRepository.registerFcmToken(token).onSuccess {
                Log.d("FCM", "서버에 토큰 등록 성공")
            }.onFailure { e ->
                Log.e("FCM", "서버에 토큰 등록 실패", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "푸시 수신 from=${remoteMessage.from}")

        remoteMessage.notification?.let {
            val title = it.title ?: "MOMOLAB"
            val body = it.body ?: ""
            Log.d("FCM", "알림 수신 title=$title, body=$body")

            // 알림 표시
            showNotification(title, body)
        }
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 앱을 여는 PendingIntent 생성
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "momolab_morning_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // 고유한 알림 ID 사용
        notificationManager.notify(notificationIdCounter.incrementAndGet(), notification)
    }

    private fun saveFcmTokenLocal(token: String) {
        val prefs = getSharedPreferences(FCM_PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putString(FCM_TOKEN_KEY, token).apply()
    }
}