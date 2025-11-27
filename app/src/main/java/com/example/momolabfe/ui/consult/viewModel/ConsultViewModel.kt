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
    private fun nextId(): Long = nextMessageId++

    // 동시에 여러 번 스트리밍 되는 것 방지용 플래그
    private var isStreaming: Boolean = false
    fun isStreamingNow(): Boolean = isStreaming

    // 지금 스트리밍으로 채우고 있는 에이전트 말풍선 id
    private var streamingMessageId: Long? = null

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
        if (isStreaming) return // 이미 스트리밍 중이면 무시

        viewModelScope.launch {
            isStreaming = true
            var buffer = ""

            showAgentTyping()

            try {
                consultRepository.chatStream(request)
                    .collect { chunk ->
                        buffer += chunk
                        updateAgentStreamingMessage(buffer)
                    }
            } catch (e: ApiException) {
                _errorMessage.value = e.localizedMessage ?: "에이전트 대화 중 오류가 발생했습니다."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "알 수 없는 오류가 발생했습니다."
            } finally {
                isStreaming = false
                streamingMessageId = null
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


    // 공통 메시지 추가 함수
    private fun addMessageInternal(text: String, isUser: Boolean, isTyping: Boolean = false) {
        val current = _messages.value.orEmpty().toMutableList()
        current.add(
            ChatMessage(
                id = nextId(),
                text = text,
                isUser = isUser,
                isTyping = isTyping
            )
        )
        _messages.value = current
    }

    // 사용자 메시지 추가
    fun appendUserMessage(text: String) {
        addMessageInternal(text, isUser = true)
    }

    // 에이전트 메시지 추가
    private fun appendAgentMessage(text: String) {
        addMessageInternal(text, isUser = false)
    }

    // 에이전트 타이핑 말풍선
    private fun showAgentTyping() {
        val current = _messages.value.orEmpty().toMutableList()
        current.add(
            ChatMessage(
                id = nextId(),
                text = "입력 중...",
                isUser = false,
                isTyping = true
            )
        )
        _messages.value = current
    }

    // 응답 갱신
    private fun updateAgentStreamingMessage(fullText: String) {
        val current = _messages.value.orEmpty().toMutableList()

        if (current.isNotEmpty() && !current.last().isUser) {
            val last = current.last()
            current[current.lastIndex] = last.copy(
                text = fullText,
                isTyping = false // 여기서 애니메이션 OFF
            )
        } else {
            // 에이전트 말풍선이 없으면 새로 하나 생성
            current.add(
                ChatMessage(
                    id = nextId(),
                    text = fullText,
                    isUser = false,
                    isTyping = false
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
        streamingMessageId = null
        isStreaming = false
    }
}