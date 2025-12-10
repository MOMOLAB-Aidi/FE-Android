package com.example.momolabfe.ui.stats.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.remote.stats.model.Last7DaysAverageResponse
import com.example.momolabfe.remote.stats.model.Last7DaysStats
import com.example.momolabfe.remote.stats.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _errorEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorEvent = _errorEvent.asSharedFlow()

    private val _last7DaysStats = MutableLiveData<Last7DaysStats>()
    val last7DaysStats: LiveData<Last7DaysStats> get() = _last7DaysStats

    private val _last7DaysAverage = MutableLiveData<Last7DaysAverageResponse>()
    val last7DaysAverage: LiveData<Last7DaysAverageResponse> get() = _last7DaysAverage

    fun getLast7Days() {
        viewModelScope.launch {
            val result = statsRepository.getLast7Days()
            result.onSuccess { list ->
                _last7DaysStats.value = list
            }.onFailure { e ->
                val message = e.localizedMessage ?: "최근 7일간의 통계를 불러오지 못했습니다."
                _errorEvent.tryEmit(message)
            }
        }
    }

    fun getLast7DaysAverage() {
        viewModelScope.launch {
            val result = statsRepository.getLast7DaysAverage()
            result.onSuccess { avgResponse ->
                _last7DaysAverage.value = avgResponse
            }.onFailure { e ->
                val message = e.localizedMessage ?: "최근 7일간의 평균 통계를 불러오지 못했습니다."
                _errorEvent.tryEmit(message)
            }
        }
    }
}