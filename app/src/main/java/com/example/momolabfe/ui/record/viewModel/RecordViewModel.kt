package com.example.momolabfe.ui.record.viewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.data.remote.record.model.RecordCreateRequest
import com.example.momolabfe.data.remote.record.model.RecordOcrResponse
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

    private val _recordResult = MutableLiveData<Unit?>()
    val recordResult: LiveData<Unit?> get() = _recordResult

    private val _ocrRecordResult = MutableLiveData<RecordOcrResponse?>()
    val ocrRecordResult: LiveData<RecordOcrResponse?> get() = _ocrRecordResult

    // 수기 작성 - 공통 정보
    fun recordCommonByWriting(request: RecordCreateRequest) {
        viewModelScope.launch {
            val result = recordRepository.recordCommonByWriting(request)
            result.onSuccess {
                _recordSuccess.tryEmit(Unit)
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "OCR 텍스트 추출에 실패했습니다."
            }
        }
    }

    // ocr 텍스트 추출
    fun recordByOcr(imageUri: Uri) {
        viewModelScope.launch {
            _ocrRecordResult.value = null

            val result = recordRepository.recordByOcr(imageUri)
            result.onSuccess {
                _ocrRecordResult.value = it
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