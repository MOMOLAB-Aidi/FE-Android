package com.example.momolabfe.ui.record.viewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.remote.record.model.GetCalendarResponse
import com.example.momolabfe.remote.record.model.RecordCreateRequest
import com.example.momolabfe.remote.record.model.RecordGetResponse
import com.example.momolabfe.remote.record.model.RecordOcrResponse
import com.example.momolabfe.remote.record.model.RecordUpdateRequest
import com.example.momolabfe.remote.record.model.TodayExchangeSummary
import com.example.momolabfe.remote.record.model.WeeklyAverageResponse
import com.example.momolabfe.remote.record.repository.RecordRepository
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

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    // 교환 시간 (24시간 형식, 서버 전송용)
    private val _exchangeTime = MutableLiveData<String>()
    val exchangeTime: LiveData<String> = _exchangeTime

    private val _recordSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val recordSuccess: SharedFlow<Unit> = _recordSuccess.asSharedFlow()

    private val _recordResult = MutableLiveData<Unit?>()
    val recordResult: LiveData<Unit?> get() = _recordResult

    private val _record = MutableLiveData<RecordGetResponse>()
    val record: LiveData<RecordGetResponse> = _record

    private val _recordlistItems = MutableLiveData<List<RecordGetResponse>>()
    val recordlistItems: LiveData<List<RecordGetResponse>> get() = _recordlistItems

    private val _weeklyAverageData = MutableLiveData<WeeklyAverageResponse?>()
    val weeklyAverageData: LiveData<WeeklyAverageResponse?> = _weeklyAverageData

    private val _ocrRecordResult = MutableLiveData<RecordOcrResponse?>()
    val ocrRecordResult: LiveData<RecordOcrResponse?> get() = _ocrRecordResult

    private val _editSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val editSuccess: SharedFlow<Unit> = _editSuccess.asSharedFlow()

    private val _deleteSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleteSuccess: SharedFlow<Unit> = _deleteSuccess.asSharedFlow()

    private val _calendarData = MutableLiveData<List<GetCalendarResponse>>()
    val calendarData: LiveData<List<GetCalendarResponse>> = _calendarData

    private val _todayExchangeSummary = MutableLiveData<TodayExchangeSummary?>()
    val todayExchangeSummary: LiveData<TodayExchangeSummary?> get() = _todayExchangeSummary

    // OCR로부터 마지막으로 받은 gcsPath를 보관
    var lastOcrGcsPath: String? = null

    // 캘린더 일정 조회
    fun getCalendar(year: Int, month: Int) {
        viewModelScope.launch {
            val result = recordRepository.getCalendar(year, month)

            result.onSuccess { calendarList ->
                _calendarData.value = calendarList
            }.onFailure { e ->
                val message = e.localizedMessage ?: "캘린더 조회에 실패했습니다."
                _errorEvent.emit(message)
            }
        }
    }

    // 기록 작성
    fun createRecord(request: RecordCreateRequest) {
        viewModelScope.launch {
            val result = recordRepository.createRecord(request)
            result.onSuccess {
                _recordSuccess.tryEmit(Unit)
            }.onFailure { e ->
                val message = e.localizedMessage ?: "기록 작성에 실패했습니다."
                _errorEvent.emit(message)
            }
        }
    }

    // 기록 수정
    fun updateRecord(recId: Long, request: RecordUpdateRequest) {
        viewModelScope.launch {
            val result = recordRepository.updateRecord(recId, request)
            result.onSuccess {
                _recordSuccess.tryEmit(Unit)
            }.onFailure { e ->
                val message = e.localizedMessage ?: "기록 수정에 실패했습니다."
                _errorEvent.emit(message)
            }
        }
    }

    // 전체 기록 조회
    fun getRecordList(year: Int, month: Int) {
        viewModelScope.launch {
            val result = recordRepository.getRecordList(year, month)
            result.onSuccess { list ->
                _recordlistItems.value = list
            }.onFailure { e ->
                val message = e.localizedMessage ?: "전체 기록 조회에 실패했습니다."
                _errorEvent.emit(message)
            }
        }
    }

    // 특정 기록 조회
    fun getRecord(recId: Long) {
        viewModelScope.launch {
            val result = recordRepository.getRecord(recId)
            result.onSuccess {
                _record.value = it
            }.onFailure { e ->
                val message = e.localizedMessage ?: "특정 기록 조회에 실패했습니다."
                _errorEvent.emit(message)
            }
        }
    }

    // 최근 3개 기록 조회
    fun getRecentRecords() {
        viewModelScope.launch {
            val result = recordRepository.getRecentRecords()
            result.onSuccess { list ->
                _recordlistItems.value = list
            }.onFailure { e ->
                val message = e.localizedMessage ?: "최근 3개 기록 조회에 실패했습니다."
                _errorEvent.emit(message)
            }
        }
    }

    // 주간 평균 기록 조회
    fun getWeeklyAvgRecords() {
        viewModelScope.launch {
            val result = recordRepository.getWeeklyAvgRecords(null)
            result.onSuccess { weeklyAvgData ->
                _weeklyAverageData.value = weeklyAvgData
            }.onFailure { e ->
                val message = e.localizedMessage ?: "주간 평균 기록 조회에 실패했습니다."
                _errorEvent.emit(message)
            }
        }
    }

    // 오늘의 요약 조회
    fun getTodayExchangeSummary() {
        viewModelScope.launch {
            val result = recordRepository.getTodayExchangeSummary()
            result.onSuccess { summary ->
                _todayExchangeSummary.value = summary
            }.onFailure { e ->
                val message = e.localizedMessage ?: "오늘의 요약 조회에 실패했습니다."
                _errorEvent.emit(message)
            }
        }
    }

    // 특정 기록 삭제
    fun deleteRecord(recId: Long) {
        viewModelScope.launch {
            val result = recordRepository.deleteRecord(recId)
            result.onSuccess {
                _deleteSuccess.tryEmit(Unit)
            }.onFailure { e ->
                val message = e.localizedMessage ?: "특정 기록 삭제에 실패했습니다."
                _errorEvent.emit(message)
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
                val message = e.localizedMessage ?: "OCR 텍스트 추출에 실패했습니다."
                _errorEvent.emit(message)
            }
        }
    }

    fun setExchangeTime(time: String) {
        _exchangeTime.value = time
    }

    fun clearOcr() {
        _ocrRecordResult.value = null
    }
}