package com.example.momolabfe.ui.consult.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.momolabfe.remote.consult.model.ChatRequest
import com.example.momolabfe.remote.consult.model.ConsultDetailResponse
import com.example.momolabfe.remote.consult.model.ConsultSessionSummaryRow
import com.example.momolabfe.remote.consult.model.GetConsultResponse
import com.example.momolabfe.remote.consult.model.MessageRole
import com.example.momolabfe.remote.consult.model.SessionEndRequest
import com.example.momolabfe.remote.consult.model.SessionEndResponse
import com.example.momolabfe.remote.consult.model.StartConsultResponse
import com.example.momolabfe.remote.consult.repository.ConsultRepository
import com.example.momolabfe.ui.consult.data.ChatMessage
import com.example.momolabfe.ui.consult.data.SummaryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _endConsultSuccess = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val endConsultSuccess: SharedFlow<String> = _endConsultSuccess.asSharedFlow()

    private val _history = MutableLiveData<List<GetConsultResponse>>()
    val history: LiveData<List<GetConsultResponse>> get() = _history

    private val _consult = MutableLiveData<List<ConsultDetailResponse>>()
    val consult: LiveData<List<ConsultDetailResponse>> get() = _consult

    private val _summaryResult = MutableLiveData<ConsultSessionSummaryRow>()
    val summaryResult: LiveData<ConsultSessionSummaryRow> get() = _summaryResult

    private val _deleteSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleteSuccess: SharedFlow<Unit> = _deleteSuccess.asSharedFlow()

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

    // 요약 UI 상태 (요약 중인지, 어떤 세션인지)
    private val _summaryUiState = MutableStateFlow(SummaryUiState())
    val summaryUiState: StateFlow<SummaryUiState> = _summaryUiState.asStateFlow()

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
                // chunk 들어올 때마다 버퍼 누적 + 그 버퍼로 마지막 에이전트 말풍선 갱신
                consultRepository.chatStream(request)
                    .collect { chunk ->
                        buffer += chunk
                        updateAgentStreamingMessage(buffer)
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "에이전트 대화 중 오류가 발생했습니다."
            } finally {
                isStreaming = false
                markAgentMessageCompleted()
            }
        }
    }

    // 상담 종료
    fun endConsult(request: SessionEndRequest) {
        viewModelScope.launch {
            val result = consultRepository.endConsult(request)
            result.onSuccess { response ->
                _endConsult.value = response
                _endConsultSuccess.tryEmit(request.sessionId)
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "상담 종료에 실패했습니다."
            }
        }
    }

    // 전체 상담 기록 목록 조회
    fun getConsultList(skip: Int = 0, limit: Int = 50) {
        viewModelScope.launch {
            val result = consultRepository.getConsultList(skip, limit)
            result.onSuccess { list ->
                _history.value = list
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "상담 기록을 불러오지 못했습니다."
            }
        }
    }

    // 특정 상담 기록 상세 조회
    fun getConsult(sessionId: String) {
        viewModelScope.launch {
            val result = consultRepository.getConsult(sessionId)
            result.onSuccess { detailList ->
                _consult.value = detailList

                resetMessages()

                // 서버에서 온 role 기준으로 ChatMessage 변환
                val mapped = detailList.map { detail ->
                    ChatMessage(
                        id = nextId(),
                        text = detail.content,
                        isUser = detail.role == MessageRole.USER
                    )
                }

                _messages.value = mapped
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "특정 상담 기록 상세 조회에 실패했습니다."
            }
        }
    }

    // 특정 세션 상담 기록 삭제
    fun deleteConsult(sessionId: String) {
        viewModelScope.launch {
            val result = consultRepository.deleteConsult(sessionId)
            result.onSuccess {
                // 리스트 갱신
                val current = _history.value.orEmpty()
                _history.value = current.filterNot { it.sessionId == sessionId }

                _deleteSuccess.tryEmit(Unit)
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "특정 세션 상담 기록 삭제에 실패했습니다."
            }
        }
    }

    // 특정 상담 세션 요약
    fun summaryConsult(sessionId: String) {
        viewModelScope.launch {

            val result = consultRepository.summaryConsult(sessionId)
            result.onSuccess { response ->
                _summaryResult.value = response

                // 요약 종료 상태로 리셋
                _summaryUiState.value = _summaryUiState.value.copy(
                    isSummarizing = false
                )
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "특정 상담 세션 요약에 실패했습니다."

                // 실패해도 상태는 종료로 리셋
                _summaryUiState.value = _summaryUiState.value.copy(
                    isSummarizing = false
                )
            }
        }
    }


    // 공통 메시지 추가 함수
    private fun addMessageInternal(text: String, isUser: Boolean) {
        val current = _messages.value.orEmpty().toMutableList()
        current.add(
            ChatMessage(
                id = nextId(),
                text = text,
                isUser = isUser,
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

    private fun showAgentTyping() {
        val current = _messages.value.orEmpty().toMutableList()
        current.add(
            ChatMessage(
                id = nextId(),
                text = "입력 중...",   // 처음에는 인디케이터 텍스트만
                isUser = false,
                isTyping = true
            )
        )
        _messages.value = current
    }

    // 스트리밍 중 실시간 텍스트 갱신
    private fun updateAgentStreamingMessage(fullText: String) {
        val current = _messages.value.orEmpty().toMutableList()

        if (current.isNotEmpty() && !current.last().isUser) {
            val last = current.last()
            current[current.lastIndex] = last.copy(
                text = fullText,
            )
        } else {
            // 에이전트 말풍선이 없으면 새로 하나 생성
            current.add(
                ChatMessage(
                    id = nextId(),
                    text = fullText,
                    isUser = false,
                    isTyping = true
                )
            )
        }
        _messages.value = current
        _agentMessage.value = fullText
    }

    fun applySummaryToHistory(row: ConsultSessionSummaryRow) {
        val current = _history.value?.toMutableList() ?: return
        val index = current.indexOfFirst { it.sessionId == row.sessionId }
        if (index == -1) return

        val old = current[index]
        current[index] = old.copy(summary = row.summary)
        _history.value = current
    }

    // 답변 완료 마킹 함수
    private fun markAgentMessageCompleted() {
        val current = _messages.value.orEmpty().toMutableList()
        if (current.isNotEmpty() && !current.last().isUser) {
            val last = current.last()
            if (last.isTyping) {
                current[current.lastIndex] = last.copy(isTyping = false)
                _messages.value = current
            }
        }
    }

    // 상담 화면에서 종료 버튼으로 끝냈을 때 호출
    fun startSummaryFromConsultEnd(sessionId: String) {
        _summaryUiState.value = SummaryUiState(
            isSummarizing = true,
            targetSessionId = sessionId,
            navigateToHistoryOnEnd = true
        )

        summaryConsult(sessionId)
    }

    // 기록 화면에서 "요약 다시 생성" 눌렀을 때 호출
    fun retrySummaryFromHistory(sessionId: String) {
        _summaryUiState.value = SummaryUiState(
            isSummarizing = true,
            targetSessionId = sessionId,
            navigateToHistoryOnEnd = false
        )

        summaryConsult(sessionId)
    }

    // 새 상담 시작 시 히스토리 초기화
    fun resetMessages() {
        _messages.value = emptyList()
        _agentMessage.value = ""
        nextMessageId = 0L
        isStreaming = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}