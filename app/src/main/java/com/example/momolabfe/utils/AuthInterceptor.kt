package com.example.momolabfe.utils

import android.util.Log
import com.example.momolabfe.remote.auth.LogoutManager
import com.example.momolabfe.remote.auth.data.TokenRequest
import com.example.momolabfe.remote.auth.service.AuthService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

// 요청 단위 1회 재시도 가드용 마커
private object RetryOnceTag

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val logoutManager: LogoutManager,
    @NoAuthRetrofit private val noAuthService: AuthService,
) : Interceptor {

    companion object {
        private const val AUTH_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val JWT_EXPIRED_CODE = "AUTH_401_02"
        private const val HTTP_UNAUTHORIZED = 401

        // 재발급 대상 제외, 즉시 로그아웃할 코드들
        private val FORCE_LOGOUT_CODES = setOf(
            "AUTH_401_03", // TOKEN_INVALID
            "AUTH_401_04", // TOKEN_GENERAL_ERROR
            "AUTH_401_06", // REFRESH_TOKEN_NOT_FOUND
            "USER_403_01"  // USER_STATUS_INACTIVE
        )

        private val NO_AUTH_PATHS = listOf(
            "/api/v1/auth/login",
            "/api/v1/auth/reissue"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 이미 재발급 재시도를 했는지 확인
        val hasRetried = originalRequest.tag(RetryOnceTag::class.java) != null
        if (isNoAuthRequired(originalRequest.url.encodedPath)) {
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenManager.getAccessToken()
        val refreshToken = tokenManager.getRefreshToken()

        val requestWithToken = originalRequest.newBuilder().apply {
            if (!accessToken.isNullOrEmpty()) {
                addHeader(AUTH_HEADER, "$BEARER_PREFIX$accessToken")
            }
        }.build()

        val response = chain.proceed(requestWithToken)

        // 이미 재시도한 요청은 더 이상 재발급을 시도하지 않음
        if (!hasRetried && isTokenExpired(response) && !refreshToken.isNullOrEmpty()) {

            val newAccessToken = refreshAccessToken(refreshToken)
            if (!newAccessToken.isNullOrEmpty()) {
                val newRequest = originalRequest.newBuilder()
                    .header(AUTH_HEADER, "$BEARER_PREFIX$newAccessToken")
                    .tag(RetryOnceTag::class.java, RetryOnceTag)
                    .build()
                response.close()
                return chain.proceed(newRequest)
            } else {
                // 재발급 실패 시에는 기존 응답을 그대로 반환
                return response
            }
        }

        // 401이면 RT 만료/무효로 간주하고 즉시 로그아웃
        if (response.code == HTTP_UNAUTHORIZED) {
            val code = extractErrorCode(response)
            if (code != null && code != JWT_EXPIRED_CODE && FORCE_LOGOUT_CODES.contains(code)) {
                logoutManager.forceLogout()
            }
        }

        return response
    }

    private fun isNoAuthRequired(path: String): Boolean =
        NO_AUTH_PATHS.any { path == it }

    // 토큰 만료 처리
    private fun isTokenExpired(response: Response): Boolean {
        if (response.code != HTTP_UNAUTHORIZED) return false
        return try {
            val bodyString = readBodyString(response)
            if (bodyString.isNotEmpty()) {
                val api = parseApiErrorBody(bodyString)
                api?.code == JWT_EXPIRED_CODE
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e("AuthInterceptor", "응답 본문 파싱 실패", e)
            true
        }
    }

    // 에러코드만 추출 (재발급 로직과 분리)
    private fun extractErrorCode(response: Response): String? = try {
        val bodyString = readBodyString(response)
        if (bodyString.isNotEmpty()) parseApiErrorBody(bodyString)?.code else null
    } catch (e: Exception) {
        Log.e("AuthInterceptor", "코드 파싱 실패", e)
        null
    }

    // peekBody로 본문을 소비하지 않고 미리보기 문자열 획득
    private fun readBodyString(response: Response): String {
        return try {
            response.peekBody(1024 * 1024).string() // 1MB
        } catch (e: Exception) {
            Log.e("AuthInterceptor", "본문 미리보기 실패", e)
            ""
        }
    }

    // ApiResponse<Any?>로 에러 바디 파싱
    private fun parseApiErrorBody(bodyString: String): ApiResponse<Any?>? = try {
        val type = object : TypeToken<ApiResponse<Any?>>() {}.type
        Gson().fromJson<ApiResponse<Any?>>(bodyString, type)
    } catch (_: Exception) {
        null
    }

    private fun refreshAccessToken(refreshToken: String): String? {
        // 동시 재발급을 1회로 합쳐줌
        return runBlocking {
            RefreshSingleFlight.refresh {
                reissueOnce(refreshToken)
            }
        }
    }

    // 네트워크 호출만 담당 (싱글플라이트 블록 안에서 호출)
    private suspend fun reissueOnce(refreshToken: String): String? {
        return try {

            val reissueResponse = noAuthService.reissue(
                TokenRequest(refreshToken)
            )

            if (reissueResponse.isSuccessful) {
                val newToken = reissueResponse.body()?.result
                fun String.mask() = if (length > 10) take(4) + "..." + takeLast(6) else "***"

                Log.d("AuthInterceptor", "토큰이 성공적으로 재발급되었습니다.")
                Log.d("AuthInterceptor", "새로운 accessToken: ${newToken?.accessToken?.mask()}")
                Log.d("AuthInterceptor", "새로운 refreshToken: ${newToken?.refreshToken?.mask()}")

                tokenManager.saveTokens(
                    accessToken = newToken?.accessToken ?: "",
                    refreshToken = newToken?.refreshToken ?: refreshToken
                )
                newToken?.accessToken
            } else {
                // 401이면 RT 만료/무효로 간주하고 즉시 로그아웃
                if (reissueResponse.code() == HTTP_UNAUTHORIZED) {
                    val errBody = reissueResponse.errorBody()?.string()
                    val errCode = errBody?.let { parseApiErrorBody(it)?.code }

                    Log.e(
                        "AuthInterceptor",
                        "토큰 재발급 실패: 401${if (errCode != null) " (code=$errCode)" else ""} 강제 로그아웃"
                    )
                    logoutManager.forceLogout()
                } else {
                    Log.e("AuthInterceptor", "토큰 재발급 실패: ${reissueResponse.code()}")
                }
                null
            }
        } catch (e: Exception) {
            Log.e("AuthInterceptor", "토큰 재발급 실패", e)
            null
        }
    }
}