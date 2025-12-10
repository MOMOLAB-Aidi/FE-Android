package com.example.momolabfe.ui.auth.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.BuildConfig
import com.example.momolabfe.remote.auth.model.LoginRequest
import com.example.momolabfe.remote.auth.model.TokenResponse
import com.example.momolabfe.remote.auth.repository.AuthRepository
import com.example.momolabfe.remote.auth.repository.PreferenceRepository
import com.example.momolabfe.remote.fcm.repository.FcmRepository
import com.example.momolabfe.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val prefRepository: PreferenceRepository,
    private val fcmRepository: FcmRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _errorEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorEvent = _errorEvent.asSharedFlow()

    private val _loginSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loginSuccess: SharedFlow<Unit> = _loginSuccess.asSharedFlow()

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            val result = authRepository.login(request)
            result.onSuccess { loginResponse ->
                // 1) 토큰 저장 (→ AuthInterceptor에서 Authorization 헤더 붙일 수 있게)
                saveTokens(loginResponse.tokens)

                // 2) 로그인 성공 후 FCM 토큰 서버에 등록
                registerFcmTokenAfterLogin()

                _loginSuccess.tryEmit(Unit)
            }.onFailure { e ->
                val message = e.localizedMessage ?: "로그인에 실패했습니다."
                _errorEvent.emit(message)
            }
        }
    }

    private fun saveTokens(tokens: TokenResponse) {
        tokenManager.saveTokens(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken
        )
    }

    // 로그인 성공 직후, 로컬에 저장된 FCM 토큰을 서버에 등록
    private suspend fun registerFcmTokenAfterLogin() {
        val prefs = appContext.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        val fcmToken = prefs.getString("fcm_token", null)

        if (fcmToken.isNullOrBlank()) {
            Log.d("FCM", "저장된 FCM 토큰이 없어 서버에 보낼 수 없음")
            return
        }

        if (BuildConfig.DEBUG) {
            Log.d("FCM", "로그인 후 서버에 보낼 FCM 토큰: ${fcmToken.take(10)}...")
        }

        val result = fcmRepository.registerFcmToken(fcmToken)

        result.onSuccess {
            Log.d("FCM", "로그인 후 FCM 토큰 서버 등록 성공")
        }.onFailure { e ->
            Log.e("FCM", "로그인 후 FCM 토큰 서버 등록 실패", e)
        }
    }

    // ID 저장
    fun savePatientId(id: String) {
        viewModelScope.launch {
            prefRepository.savePatientId(id)
        }
    }

    // ID 제거
    fun clearSavedPatientId() {
        viewModelScope.launch {
            prefRepository.clearPatientId()
        }
    }

    // ID 불러오기
    fun getSavedPatientId(): LiveData<String> = prefRepository.getPatientId()
}