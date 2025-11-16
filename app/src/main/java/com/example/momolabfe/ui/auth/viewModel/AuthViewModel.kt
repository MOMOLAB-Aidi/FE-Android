package com.example.momolabfe.ui.auth.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.data.remote.auth.data.LoginRequest
import com.example.momolabfe.data.remote.auth.data.LoginResponse
import com.example.momolabfe.data.remote.auth.data.TokenResponse
import com.example.momolabfe.data.remote.auth.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _loginSuccess = MutableSharedFlow<LoginResponse>(extraBufferCapacity = 1)
    val loginSuccess: SharedFlow<LoginResponse> = _loginSuccess.asSharedFlow()

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            val result = authRepository.login(request)
            result.onSuccess { tokenResponse ->
                _loginSuccess.tryEmit(tokenResponse)
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "로그인에 실패했습니다."
                _errorMessage.value = null
            }
        }
    }
}