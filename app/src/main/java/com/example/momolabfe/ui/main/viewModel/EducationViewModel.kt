package com.example.momolabfe.ui.main.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.remote.education.model.EducationResponse
import com.example.momolabfe.remote.education.repository.EducationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EducationViewModel @Inject constructor(
    private val educationRepository: EducationRepository
) : ViewModel() {

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    private val _getTipResult = MutableLiveData<EducationResponse>()
    val getTipResult: LiveData<EducationResponse> get() = _getTipResult

    // 오늘의 복막투석 관리 TIP 조회
    fun getTodayTip() {
        viewModelScope.launch {
            val result = educationRepository.getTodayTip()
            result.onSuccess {
                _getTipResult.value = it
            }.onFailure { e ->
                val message = e.localizedMessage ?: "오늘의 복막투석 관리 TIP 조회에 실패했습니다."
                _errorEvent.emit(message)
            }
        }
    }
}