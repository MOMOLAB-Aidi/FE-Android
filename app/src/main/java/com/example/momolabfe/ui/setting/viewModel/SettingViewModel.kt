package com.example.momolabfe.ui.setting.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.remote.user.model.MyPageResponse
import com.example.momolabfe.remote.user.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _getPageResult = MutableLiveData<MyPageResponse>()
    val getPageResult: LiveData<MyPageResponse> get() = _getPageResult

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
}