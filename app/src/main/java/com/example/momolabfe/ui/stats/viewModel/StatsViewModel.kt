package com.example.momolabfe.ui.stats.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.remote.stats.model.Last7DaysStats
import com.example.momolabfe.remote.stats.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _getStatsResult = MutableLiveData<Last7DaysStats>()
    val getStatsResult: LiveData<Last7DaysStats> get() = _getStatsResult

    fun getLast7Days() {
        viewModelScope.launch {
            val result = statsRepository.getLast7Days()
            result.onSuccess { list ->
                _getStatsResult.value = list
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "최근 7일간의 통계를 불러오지 못했습니다."
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}