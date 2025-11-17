package com.example.momolabfe.ui.auth.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.data.remote.auth.data.LoginRequest
import com.example.momolabfe.data.remote.auth.data.TokenResponse
import com.example.momolabfe.data.remote.auth.repository.AuthRepository
import com.example.momolabfe.data.remote.auth.repository.PreferenceRepository
import com.example.momolabfe.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val prefRepository: PreferenceRepository
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _loginSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loginSuccess: SharedFlow<Unit> = _loginSuccess.asSharedFlow()

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            val result = authRepository.login(request)
            result.onSuccess { loginResponse ->
                saveTokens(loginResponse.tokens)
                _loginSuccess.tryEmit(Unit)
                _errorMessage.value = null
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "로그인에 실패했습니다."
            }
        }
    }

    private fun saveTokens(tokens: TokenResponse) {
        tokenManager.saveTokens(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken
        )
    }


    // 1. ID 저장
    fun savePatientId(id: String) {
        viewModelScope.launch {
            prefRepository.savePatientId(id)
        }
    }

    // 2. ID 제거
    fun clearSavedPatientId() {
        viewModelScope.launch {
            prefRepository.clearPatientId()
        }
    }

    // 3. ID 불러오기
    fun getSavedPatientId(): LiveData<String> = prefRepository.getPatientId()
}