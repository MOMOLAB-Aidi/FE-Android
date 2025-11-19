package com.example.momolabfe.ui.record.viewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.data.remote.record.model.GetCalendarResponse
import com.example.momolabfe.remote.record.model.RecordCreateRequest
import com.example.momolabfe.remote.record.model.RecordExchangeCreateRequest
import com.example.momolabfe.remote.record.model.RecordOcrResponse
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

    // 교환 시간 (24시간 형식, 서버 전송용)
    private val _exchangeTime = MutableLiveData<String>()
    val exchangeTime: LiveData<String> = _exchangeTime

    private val _recordSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val recordSuccess: SharedFlow<Unit> = _recordSuccess.asSharedFlow()

    private val _recordResult = MutableLiveData<Unit?>()
    val recordResult: LiveData<Unit?> get() = _recordResult

    private val _ocrRecordResult = MutableLiveData<com.example.momolabfe.remote.record.model.RecordOcrResponse?>()
    val ocrRecordResult: LiveData<com.example.momolabfe.remote.record.model.RecordOcrResponse?> get() = _ocrRecordResult

    private val _recordCreated = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val recordCreated: SharedFlow<Long> = _recordCreated

    private val _calendarData = MutableLiveData<List<GetCalendarResponse>>()
    val calendarData: LiveData<List<GetCalendarResponse>> = _calendarData

    // 캘린더 일정 조회
    fun getCalendar(year: Int, month: Int) {
        viewModelScope.launch {
            val result = recordRepository.getCalendar(year, month)

            result.onSuccess { calendarList ->
                _calendarData.value = calendarList
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "캘린더 조회에 실패했습니다."
            }
        }
    }



    // 수기 작성 - 공통 정보
    fun recordCommonByWriting(request: com.example.momolabfe.remote.record.model.RecordCreateRequest) {
        viewModelScope.launch {
            val result = recordRepository.recordCommonByWriting(request)
            result.onSuccess { id ->
                _recordCreated.emit(id)
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "공통 정보 저장에 실패했습니다."
            }
        }
    }

    // 수기 작성 - 회차별 정보
    fun recordExchangeByWriting(recId: Long, request: com.example.momolabfe.remote.record.model.RecordExchangeCreateRequest) {
        viewModelScope.launch {
            val result = recordRepository.recordExchangeByWriting(recId, request)
            result.onSuccess {
                _recordSuccess.tryEmit(Unit)
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "회차별 정보 저장에 실패했습니다."
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

    fun setExchangeTime(time: String) {
        _exchangeTime.value = time
    }

    fun getExchangeTime(): String? {
        return _exchangeTime.value
    }

    fun clearOcr() {
        _ocrRecordResult.value = null
        _errorMessage.value = null
    }
}