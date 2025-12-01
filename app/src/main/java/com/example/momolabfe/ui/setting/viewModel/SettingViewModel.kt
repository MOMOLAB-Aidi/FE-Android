package com.example.momolabfe.ui.setting.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.remote.user.model.MyPageResponse
import com.example.momolabfe.remote.user.model.UpdatePassword
import com.example.momolabfe.remote.user.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _passwordSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val passwordSuccess: SharedFlow<Unit> = _passwordSuccess.asSharedFlow()

    private val _getPageResult = MutableLiveData<MyPageResponse>()
    val getPageResult: LiveData<MyPageResponse> get() = _getPageResult

    private val _passwordResult = MutableLiveData<Unit?>()
    val passwordResult: LiveData<Unit?> get() = _passwordResult

    // 마이페이지 조회
    fun getMyPage() {
        viewModelScope.launch {
            val result = userRepository.getMyPage()
            result.onSuccess {
                _getPageResult.value = it
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "마이페이지 조회에 실패했습니다."
            }
        }
    }

    // 비밀번호 재설정
    fun updatePassword(request: UpdatePassword) {
        viewModelScope.launch {
            val result = userRepository.updatePassword(request)
            result.onSuccess {
                _passwordSuccess.tryEmit(Unit)
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "비밀번호 재설정에 실패했습니다."
            }
        }
    }
}