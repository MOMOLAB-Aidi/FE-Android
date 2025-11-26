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
import com.example.momolabfe.ui.consult.data.ChatMessage
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

    // 전체 채팅 리스트 (말풍선 용)
    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> get() = _messages

    // ChatMessage 고유 id 생성용 시퀀스
    private var nextMessageId: Long = 0L
    private fun nextId(): Long = ++nextMessageId

    // 공통 메시지 추가 함수
    private fun addMessageInternal(text: String, isUser: Boolean) {
        val current = _messages.value.orEmpty().toMutableList()
        current.add(
            ChatMessage(
                id = nextId(),
                text = text,
                isUser = isUser
            )
        )
        _messages.value = current
    }

    // 외부에서 호출하는 사용자/에이전트 메시지 추가 API
    fun appendUserMessage(text: String) {
        addMessageInternal(text, isUser = true)
    }

    fun appendAgentMessage(text: String) {
        addMessageInternal(text, isUser = false)
    }

    // 상담 시작
    fun startConsult() {
        viewModelScope.launch {
            val result = consultRepository.startConsult()
            result.onSuccess { response ->
                _startConsult.value = response

                // 세션 시작 시 환영 메시지를 채팅 리스트에도 추가
                if (response.message.isNotBlank()) {
                    appendAgentMessage(response.message)
                    _agentMessage.value = response.message
                }
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "상담 시작 세션 아이디 발급에 실패했습니다."
            }
        }
    }

    // 에이전트 대화
    fun chatStream(request: ChatRequest) {
        viewModelScope.launch {
            var buffer = ""
            try {
                consultRepository.chatStream(request)
                    .collect { chunk ->
                        buffer += chunk
                        updateLastAgentMessage(buffer)
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

    // 마지막 에이전트 말풍선에 스트리밍 텍스트 누적
    private fun updateLastAgentMessage(fullText: String) {
        val current = _messages.value.orEmpty().toMutableList()

        if (current.isNotEmpty() && !current.last().isUser) {
            // 마지막이 에이전트 말풍선이면 내용만 갱신 (id 유지!)
            val last = current.last()
            current[current.lastIndex] = last.copy(text = fullText)
        } else {
            // 마지막이 사용자이거나 리스트가 비어 있으면 새 에이전트 말풍선 추가
            current.add(
                ChatMessage(
                    id = nextId(),
                    text = fullText,
                    isUser = false
                )
            )
        }

        _messages.value = current
        _agentMessage.value = fullText
    }

    // 새 상담 시작 시 히스토리 초기화
    fun resetMessages() {
        _messages.value = emptyList()
        _agentMessage.value = ""
        nextMessageId = 0L
    }
}