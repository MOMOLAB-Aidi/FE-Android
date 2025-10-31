package com.example.momolabfe.ui.record.viewModel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.data.remote.record.model.RecordResponse
import com.example.momolabfe.data.remote.record.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _ocrResult = MutableLiveData<RecordResponse?>()
    val ocrResult: LiveData<RecordResponse?> get() = _ocrResult

    // ocr 텍스트 추출
    fun getRecordByOcr(imageUri: Uri) {
        viewModelScope.launch {
            _ocrResult.value = null

            val result = recordRepository.recordByOcr(imageUri)
            result.onSuccess {
                _ocrResult.value = it
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "OCR 텍스트 추출에 실패했습니다."
            }
        }
    }

    fun clearOcr() {
        _ocrResult.value = null
        _errorMessage.value = null
    }
}