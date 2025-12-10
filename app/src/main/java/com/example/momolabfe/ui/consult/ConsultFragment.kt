package com.example.momolabfe.ui.consult

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentConsultBinding
import com.example.momolabfe.remote.consult.model.ChatRequest
import com.example.momolabfe.remote.consult.model.SessionEndRequest
import com.example.momolabfe.ui.consult.adapter.ChatAdapter
import com.example.momolabfe.ui.consult.viewModel.ConsultViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class ConsultFragment : Fragment() {

    private var _binding: FragmentConsultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConsultViewModel by activityViewModels()

    // 발급받은 세션 ID 관리
    private var currentSessionId: String? = null
    private val chatAdapter by lazy {
        ChatAdapter(
            onAgentSpeakerToggle = { text, turnOn ->
                if (turnOn) {
                    speak(text)
                } else {
                    stopSpeak()
                }
            },
            onUserSpeakToggle = { text, turnOn ->
                if (turnOn) {
                    speak(text)
                } else {
                    stopSpeak()
                }
            }
        )
    }

    private var lastItemCount: Int = 0 // 마지막 아이템 개수 기억
    private var navigateToHistoryOnEnd: Boolean = false // 이번 세션 종료 후 히스토리 화면으로 이동할지 여부

    // 요약 완료를 기다리는 세션 ID + 요약 중 다이얼로그
    private var waitingSummaryForSessionId: String? = null
    private var summaryDialog: Dialog? = null

    private var summaryTimeoutJob: Job? = null

    private var tts: TextToSpeech? = null
    private var currentTtsUtteranceId: String? = null // 현재 재생 중인 TTS ID

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConsultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.main_bnv)
        bottomNav?.visibility = View.VISIBLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.endIv.visibility = View.GONE
        binding.plusIv.visibility = View.GONE

        currentSessionId = null // 이전 세션 ID 무효화
        viewModel.resetMessages() // 말풍선 / 에이전트 버퍼 모두 초기화
        viewModel.clearError()

        setupRecyclerView()
        setupObservers()
        showQuickQuestions()
        setupSendButtonState()

        viewModel.startConsult()

        binding.historyIv.setOnClickListener {
            val sessionId = currentSessionId

            // 진행 중인 상담 여부 판단
            val hasConversation = viewModel.messages.value
                ?.any { msg -> msg.isUser && msg.text.isNotBlank() } == true

            if (!sessionId.isNullOrBlank() && hasConversation) {
                // 상담이 진행 중인데 히스토리 버튼을 클릭한 경우
                AlertDialog.Builder(requireContext())
                    .setTitle("기록 조회")
                    .setMessage("현재 상담이 종료되지 않았습니다.\n기록을 조회하러 가시겠습니까?")
                    .setNegativeButton("계속 상담") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton("기록 조회") { dialog, _ ->
                        dialog.dismiss()
                        endSession()

                        parentFragmentManager.beginTransaction()
                            .replace(R.id.main_frm, ConsultHistoryFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                    .show()
            } else {
                // 진행 중인 상담이 없으면 바로 기록 화면으로 이동
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_frm, ConsultHistoryFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.endIv.setOnClickListener {
            val sessionId = currentSessionId
            if (sessionId.isNullOrBlank()) {
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("상담 종료")
                .setMessage(
                    "현재 상담을 종료하시겠습니까?\n\n" +
                    "대화 내용은 상담 기록에서 확인할 수 있고,\n" +
                    "이 세션에서는 더 이상 대화를 이어갈 수 없습니다."
                )
                .setNegativeButton("취소") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("종료") { dialog, _ ->
                    dialog.dismiss()
                    navigateToHistoryOnEnd = true
                    endSession()
                }
                .show()
        }

        binding.plusIv.setOnClickListener {
            if (viewModel.isStreamingNow()) {
                Toast.makeText(requireContext(), "이전 답변을 생성 중입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            restartConsultSession()
        }

        binding.sendBtn.setOnClickListener {

            val message = binding.messageInput.text.toString().trim()

            // 스트리밍 중이면 막기
            if (viewModel.isStreamingNow()) {
                Toast.makeText(requireContext(), "이전 답변을 생성 중입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sessionId = currentSessionId
            if (sessionId.isNullOrBlank()) {
                Toast.makeText(requireContext(), "상담 세션이 없습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideQuickQuestions()

            // 환자가 보낸 메시지를 오른쪽 말풍선으로 먼저 추가
            viewModel.appendUserMessage(message)

            val request = ChatRequest(
                sessionId = sessionId,
                message = message
            )

            binding.sendBtn.isEnabled = false // 전송 버튼 잠깐 비활성화
            viewModel.chatStream(request) // 채팅 스트리밍 시작
            binding.messageInput.text?.clear() // 입력창 비우기
        }

        // Chip 빠른 질문
        binding.chip1.setOnClickListener {
            setQuickQuestion("제수량이 잘 안 나와요")
        }
        binding.chip2.setOnClickListener {
            setQuickQuestion("식단 관리하는 방법")
        }
        binding.chip3.setOnClickListener {
            setQuickQuestion("복통이 있어요")
        }
        binding.chip4.setOnClickListener {
            setQuickQuestion("혈압이 높아요")
        }

        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale.KOREAN)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        // 현재 관리하는 utterance에 대해서만 처리
                        if (utteranceId == currentTtsUtteranceId) {
                            currentTtsUtteranceId = null
                            view.post {
                                if (_binding != null) {
                                    chatAdapter.onTtsFinished()
                                }
                            }
                        }
                    }

                    @Deprecated("DEPRECATION")
                    override fun onError(utteranceId: String?) {
                        if (utteranceId == currentTtsUtteranceId) {
                            currentTtsUtteranceId = null
                            view.post {
                                if (_binding != null) {
                                    chatAdapter.onTtsFinished()
                                }
                            }
                        }
                    }
                })
            }
        }
    }

    private fun setupRecyclerView() {
        binding.chatRv.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true   // 항상 아래 기준
            }
            itemAnimator = null
        }
    }

    private fun setupSendButtonState() {
        fun updateState(text: CharSequence?) {
            val hasText = !text.isNullOrBlank()

            binding.sendBtn.isEnabled = hasText
            binding.sendBtn.alpha = if (hasText) 1f else 0.5f
        }

        // 초기 상태 세팅
        updateState(binding.messageInput.text)

        // 텍스트 변경 감지
        binding.messageInput.addTextChangedListener { text ->
            updateState(text)
        }
    }

    private fun setupObservers() {
        viewModel.startConsult.observe(viewLifecycleOwner) { response ->
            currentSessionId = response.sessionId
            binding.endIv.visibility = View.GONE
            binding.plusIv.visibility = View.GONE
        }

        viewModel.messages.observe(viewLifecycleOwner) { list ->
            val binding = _binding ?: return@observe // 뷰가 사라졌으면 그냥 리턴
            val recyclerView = binding.chatRv

            val newSize = list.size

            chatAdapter.submitList(list) {
                // 새 말풍선이 추가된 경우에만 스크롤
                if (newSize > lastItemCount && newSize > 0) {
                    recyclerView.scrollToPosition(newSize - 1)
                }
                lastItemCount = newSize
            }

            // 첫 번째 사용자 메시지의 인덱스 찾기
            val firstUserIndex = list.indexOfFirst { msg ->
                msg.isUser && msg.text.isNotBlank()
            }

            // 그 이후에 에이전트 답변이 있는지 확인
            val hasAgentReplyAfterUser = if (firstUserIndex == -1) {
                false
            } else {
                list
                    .drop(firstUserIndex + 1)
                    .any { msg ->
                        !msg.isUser &&
                                msg.text.isNotBlank() &&
                                !msg.isTyping // 스트리밍 중인 말풍선은 제외
                    }
            }

            val hasActiveSession = !currentSessionId.isNullOrBlank()
            val shouldShowEndButton =
                hasActiveSession && hasAgentReplyAfterUser && !viewModel.isStreamingNow()

            binding.endIv.visibility = if (shouldShowEndButton) View.VISIBLE else View.GONE

            // 스트리밍 완료 시 버튼 상태 복원
            if (!viewModel.isStreamingNow()) {
                setupSendButtonState()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.endConsultSuccess.collect { endedSessionId ->
                    waitingSummaryForSessionId = endedSessionId
                    viewModel.startSummaryFromConsultEnd(endedSessionId)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.summaryUiState.collect { state ->
                    if (state.isSummarizing &&
                        state.targetSessionId != null &&
                        state.navigateToHistoryOnEnd
                    ) {
                        showSummaryDialog()
                    } else {
                        hideSummaryDialog()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.autoEndSession.collect {
                    // 토큰 초과로 서버에서 이미 세션이 종료된 상태
                    currentSessionId = null
                    binding.endIv.visibility = View.GONE
                    binding.plusIv.visibility = View.VISIBLE
                }
            }
        }

        viewModel.summaryResult.observe(viewLifecycleOwner) { summaryRow ->
            if (summaryRow == null) return@observe

            Log.d("ConsultFragment", "요약 생성 완료 (sessionId=${summaryRow.sessionId}, length=${summaryRow.summary.length})")

            // 우리가 기다리던 세션이면 waitingSummaryForSessionId 비워주기
            if (summaryRow.sessionId == waitingSummaryForSessionId) {
                waitingSummaryForSessionId = null
            }

            hideSummaryDialog()

            // 종료 버튼으로 끝낸 경우에만 히스토리로 이동
            if (navigateToHistoryOnEnd) {
                currentSessionId = null
                viewModel.resetMessages()
                binding.endIv.visibility = View.GONE

                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_frm, ConsultHistoryFragment())
                    .addToBackStack(null)
                    .commit()

                navigateToHistoryOnEnd = false
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                hideSummaryDialog()

                val msg = if (waitingSummaryForSessionId != null && navigateToHistoryOnEnd) {
                    waitingSummaryForSessionId = null
                    navigateToHistoryOnEnd = false
                    getString(R.string.summary_error_message)
                } else {
                    errorMsg
                }

                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun setQuickQuestion(text: String) {
        binding.messageInput.setText(text)
        binding.messageInput.setSelection(text.length)
    }

    // 빠른 질문 영역 숨기기
    private fun hideQuickQuestions() {
        binding.quickQuestionLabel.visibility = View.GONE
        binding.quickQuestionGroup.visibility = View.GONE
    }

    // 빠른 질문 영역 보이기 (새 상담 등에서 사용)
    private fun showQuickQuestions() {
        binding.quickQuestionLabel.visibility = View.VISIBLE
        binding.quickQuestionGroup.visibility = View.VISIBLE
    }

    private fun endSession() {
        val sessionId = currentSessionId ?: run {
            Toast.makeText(requireContext(), "종료할 상담 세션이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 세션 종료
        currentSessionId = null
        binding.endIv.visibility = View.GONE

        val request = SessionEndRequest(sessionId = sessionId)
        viewModel.endConsult(request)
    }

    private fun showSummaryDialog() {
        if (summaryDialog?.isShowing == true) return

        summaryDialog = Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_summary_loading)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.attributes = window?.attributes?.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
            }
            show()
        }

        // 30초 타임아웃 설정 (기존 Job 정리 후 새로 생성)
        summaryTimeoutJob?.cancel()
        summaryTimeoutJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(30_000)

            if (!isAdded || _binding == null) return@launch // 뷰가 이미 파괴된 경우 안전하게 종료

            summaryDialog?.dismiss()
            summaryDialog = null
            summaryTimeoutJob = null

            // 지금 이 종료가 종료 버튼에서 온 것인지 플래그 복사
            val shouldNavigateToHistory = navigateToHistoryOnEnd
            navigateToHistoryOnEnd = false
            waitingSummaryForSessionId = null

            if (shouldNavigateToHistory) {
                // 세션은 끝났다고 보고 정리
                currentSessionId = null
                viewModel.resetMessages()
                binding.endIv.visibility = View.GONE

                Toast.makeText(
                    requireContext(),
                    getString(R.string.summary_timeout_message),
                    Toast.LENGTH_LONG
                ).show()

                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_frm, ConsultHistoryFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun hideSummaryDialog() {
        summaryTimeoutJob?.cancel()
        summaryTimeoutJob = null
        summaryDialog?.dismiss()
        summaryDialog = null
    }

    private fun restartConsultSession() {
        currentSessionId = null

        viewModel.resetMessages()
        showQuickQuestions()
        binding.endIv.visibility = View.GONE
        binding.plusIv.visibility = View.GONE

        viewModel.startConsult() // 새 상담 시작
    }

    private fun cleanTextForTts(text: String): String {
        var result = text

        // 1. 혈압: "120/80 mmHg" → "120 에 80 밀리미터 에이치지"
        result = result.replace(
            Regex("(\\d+)\\s*/\\s*(\\d+)\\s*mmHg", RegexOption.IGNORE_CASE)
        ) { match ->
            val sys = match.groupValues[1]
            val dia = match.groupValues[2]
            "$sys 에 $dia 밀리미터 에이치지"
        }

        // 2. 수축기/이완기 혈압: "90 mmHg" → "90 밀리미터 에이치지"
        result = result.replace(
            Regex("(\\d+)\\s*mmHg", RegexOption.IGNORE_CASE)
        ) { match ->
            "${match.groupValues[1]} 밀리미터 에이치지"
        }

        // 3. 혈당: "100 mg/dL" → "100 밀리그램 퍼 데시리터"
        result = result.replace(
            Regex("(\\d+)\\s*mg/dL", RegexOption.IGNORE_CASE)
        ) { match ->
            "${match.groupValues[1]} 밀리그램 퍼 데시리터"
        }

        // 4. 체중: "70 kg" → "70 킬로그램"
        result = result.replace(
            Regex("(\\d+)\\s*kg", RegexOption.IGNORE_CASE)
        ) { match ->
            "${match.groupValues[1]} 킬로그램"
        }

        // 5. 제수량 등: "100 g" → "100 그램"
        result = result.replace(
            Regex("(\\d+)\\s*g\\b", RegexOption.IGNORE_CASE)
        ) { match ->
            "${match.groupValues[1]} 그램"
        }

        // 6. 농도: "2.5 %" → "2.5 퍼센트"
        result = result.replace(
            Regex("(\\d+(?:\\.\\d+)?)\\s*%", RegexOption.IGNORE_CASE)
        ) { match ->
            "${match.groupValues[1]} 퍼센트"
        }

        // ".2" → ". 2",  "1.혈압" → "1. 혈압"
        result = result.replace(Regex("\\.(\\d)")) {
            ". ${it.groupValues[1]}"
        }
        result = result.replace(Regex("(\\d)\\.(?!\\d)")) {
            "${it.groupValues[1]}. "
        }

        return result.trim()
    }

    private fun speak(text: String) {
        val clean = cleanTextForTts(text).trim()
        if (clean.isEmpty()) return

        val utteranceId = "CONSULT_TTS_${System.currentTimeMillis()}"
        currentTtsUtteranceId = utteranceId

        tts?.speak(
            clean,
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        )
    }

    private fun stopSpeak() {
        currentTtsUtteranceId = null
        tts?.stop()
        chatAdapter.onTtsFinished()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideSummaryDialog()
        tts?.stop()
        chatAdapter.onTtsFinished()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        tts = null
    }
}