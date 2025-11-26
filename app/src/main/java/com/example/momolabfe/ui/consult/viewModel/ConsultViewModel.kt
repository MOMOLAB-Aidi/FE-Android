package com.example.momolabfe.ui.consult.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.remote.consult.data.ChatRequest
import com.example.momolabfe.remote.consult.data.SessionEndRequest
import com.example.momolabfe.remote.consult.data.SessionEndResponse
import com.example.momolabfe.remote.consult.data.StartConsultResponse
import com.example.momolabfe.remote.consult.repository.ConsultRepository
import com.example.momolabfe.utils.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConsultViewModel @Inject constructor(
    private val consultRepository: ConsultRepository
) : ViewModel() {

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _startConsult = MutableLiveData<StartConsultResponse>()
    val startConsult: LiveData<StartConsultResponse> get() = _startConsult

    private val _endConsult = MutableLiveData<SessionEndResponse>()
    val endConsult: LiveData<SessionEndResponse> get() = _endConsult

    // 에이전트 답변(스트리밍으로 누적)
    private val _agentMessage = MutableLiveData<String>()
    val agentMessage: LiveData<String> get() = _agentMessage

    // 상담 시작
    fun startConsult() {
        viewModelScope.launch {
            val result = consultRepository.startConsult()
            result.onSuccess { response ->
                _startConsult.value = response
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "상담 시작 세션 아이디 발급에 실패했습니다."
            }
        }
    }

    // 에이전트 대화
    fun chatStream(request: ChatRequest) {
        viewModelScope.launch {
            try {
                // Flow<String> 을 collect 해서 chunk 단위로 받기
                consultRepository.chatStream(request)
                    .collect { chunk ->
                        // 기존에 누적된 내용 + 새 chunk
                        val current = _agentMessage.value ?: ""
                        _agentMessage.value = current + chunk
                    }
            } catch (e: ApiException) {
                _errorMessage.value = e.localizedMessage ?: "에이전트 대화 중 오류가 발생했습니다."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "알 수 없는 오류가 발생했습니다."
            }
        }
    }

    // 상담 종료
    fun endConsult(request: SessionEndRequest) {
        viewModelScope.launch {
            val result = consultRepository.endConsult(request)
            result.onSuccess { response ->
                _endConsult.value = response
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "상담 종료에 실패했습니다."
            }
        }
    }
}