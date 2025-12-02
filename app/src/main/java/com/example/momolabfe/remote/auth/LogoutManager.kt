package com.example.momolabfe.remote.auth

import android.util.Log
import com.example.momolabfe.remote.auth.service.AuthService
import com.example.momolabfe.utils.AuthRetrofit
import com.example.momolabfe.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class LogoutManager @Inject constructor(
    private val tokenManager: TokenManager,
    @AuthRetrofit private val retrofitProvider: Provider<Retrofit>
) {
    // 로그아웃 성공 이벤트를 발행하는 SharedFlow
    private val _logoutSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutSuccess: SharedFlow<Unit> = _logoutSuccess.asSharedFlow()

    private val loggingOut = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val authService: AuthService by lazy {
        retrofitProvider.get().create(AuthService::class.java)
    }

    // Interceptor 등 비코루틴 컨텍스트에서 호출 가능
    fun forceLogout() {
        scope.launch {
            logout()
        }
    }

    /**
     * 전체 로그아웃 플로우를 실행
     * 서버 로그아웃 -> 로컬 정리 -> 이벤트 발행
     */
    suspend fun logout() {
        if (!loggingOut.compareAndSet(false, true)) return
        Log.d("LogoutManager", "로그아웃 시작")

        try {
            // 1) 서버 로그아웃 (실패해도 로컬 정리는 진행)
            runCatching {
                val resp = withContext(Dispatchers.IO) { authService.logout() }
                val body = resp.body()

                // 서버 응답이 성공이거나, 이미 로그아웃된 상태로 처리
                val ok = resp.isSuccessful && (body?.isSuccess == true)

                if (ok) {
                    Log.d("LogoutManager", "서버 로그아웃 성공")
                } else {
                    Log.e("LogoutManager", "서버 로그아웃 실패 또는 오류: ${body?.message ?: resp.code()}")
                    // 서버 로그아웃이 실패해도 로컬 정리는 진행
                }
            }.onFailure { e ->
                Log.e("LogoutManager", "서버 로그아웃 호출 중 예외 발생", e)
            }

            // 2) 로컬 정리 (토큰 삭제)
            withContext(Dispatchers.IO) {
                tokenManager.clearTokens()
                Log.d("LogoutManager", "로컬 토큰 정리 완료")
            }

            // 4) UI에 로그아웃 이벤트 발행
            _logoutSuccess.tryEmit(Unit)

        } catch (e: Exception) {
            Log.e("LogoutManager", "로그아웃 중 예외 발생", e)
        } finally {
            loggingOut.set(false)
        }
    }
}