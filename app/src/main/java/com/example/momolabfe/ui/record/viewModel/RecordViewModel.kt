package com.example.momolabfe.ui.record.viewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.data.remote.record.model.RecordRequest
import com.example.momolabfe.data.remote.record.model.RecordResponse
import com.example.momolabfe.data.remote.record.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _recordSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val recordSuccess: SharedFlow<Unit> = _recordSuccess.asSharedFlow()

    private val _recordResult = MutableLiveData<RecordResponse?>()
    val recordResult: LiveData<RecordResponse?> get() = _recordResult

    // 기록 수기 작성
    fun recordByWriting(request: RecordRequest) {
        viewModelScope.launch {
            val result = recordRepository.recordByWriting(request)
            result.onSuccess {
                _recordResult.value = it
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "OCR 텍스트 추출에 실패했습니다."
            }
        }
    }

    // ocr 텍스트 추출
    fun recordByOcr(imageUri: Uri) {
        viewModelScope.launch {
            _recordResult.value = null

            val result = recordRepository.recordByOcr(imageUri)
            result.onSuccess {
                _recordResult.value = it
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "OCR 텍스트 추출에 실패했습니다."
            }
        }
    }

    fun clearOcr() {
        _recordResult.value = null
        _errorMessage.value = null
    }
}