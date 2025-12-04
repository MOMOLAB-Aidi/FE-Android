package com.example.momolabfe.ui.consult

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentConsultBinding
import com.example.momolabfe.remote.consult.model.ChatRequest
import com.example.momolabfe.remote.consult.model.SessionEndRequest
import com.example.momolabfe.ui.consult.adapter.ChatAdapter
import com.example.momolabfe.ui.consult.viewModel.ConsultViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class ConsultFragment : Fragment() {

    private var _binding: FragmentConsultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConsultViewModel by activityViewModels()

    // 발급받은 세션 ID 관리
    private var currentSessionId: String? = null
    private val chatAdapter by lazy { ChatAdapter() }

    private var lastItemCount: Int = 0 // 마지막 아이템 개수 기억
    private var historyDialog: Dialog? = null // 다이얼로그 생명주기 관리 용도

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

        setupRecyclerView()
        setupObservers()
        showQuickQuestions()
        setupSendButtonState()

        // 처음 진입 시 세션 없으면 상담 세션 생성
        if (currentSessionId == null) {
            viewModel.startConsult()
        }

        binding.historyIv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, ConsultHistoryFragment())
                .addToBackStack(null)
                .commit()
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
                    endSession()
                }
                .show()
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

        binding.plusIv.setOnClickListener {
            // 이전 세션은 서버에 종료 요청
            currentSessionId?.let { oldSessionId ->
                val request = SessionEndRequest(sessionId = oldSessionId)
                viewModel.endConsult(request)
                viewModel.summaryConsult(oldSessionId)
            }

            viewModel.resetMessages()
            viewModel.startConsult()
            showQuickQuestions()
            binding.endIv.visibility = View.GONE
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
            viewModel.resetMessages()
            binding.endIv.visibility = View.GONE
        }

        viewModel.messages.observe(viewLifecycleOwner) { list ->
            val newSize = list.size

            chatAdapter.submitList(list) {
                // 새 말풍선이 추가된 경우에만 스크롤
                if (newSize > lastItemCount && newSize > 0) {
                    binding.chatRv.scrollToPosition(newSize - 1)
                }
                lastItemCount = newSize
            }

            // 유저 메시지가 하나라도 있는지 체크
            val hasAgentReply = list.any { msg ->
                !msg.isUser && msg.text.isNotBlank()
            }

            val shouldShowEndButton = (currentSessionId != null) && hasAgentReply

            binding.endIv.visibility = if (shouldShowEndButton) View.VISIBLE else View.GONE

            // 스트리밍 완료 시 버튼 상태 복원
            if (!viewModel.isStreamingNow()) {
                setupSendButtonState()
            }
        }

        viewModel.endConsultSuccess.observe(viewLifecycleOwner) { endedSessionId ->
            if (endedSessionId.isNullOrBlank()) return@observe

            // 요약 생성 요청
            viewModel.summaryConsult(endedSessionId)

            // 클라이언트 상태 정리
            currentSessionId = null
            viewModel.resetMessages()
            binding.endIv.visibility = View.GONE

            // 상담 기록 화면으로 이동
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, ConsultHistoryFragment())
                .addToBackStack(null)
                .commit()

            // 이벤트 1회성 소비
            viewModel.clearEndConsultSuccess()
        }

        viewModel.summaryResult.observe(viewLifecycleOwner) { summary ->
            if (summary != null) {
                Log.d("ConsultFragment", "요약 생성 완료: $summary")
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
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

        val request = SessionEndRequest(sessionId = sessionId)
        viewModel.endConsult(request)
    }

    override fun onDestroyView() {
        historyDialog?.dismiss()
        historyDialog = null
        super.onDestroyView()
        _binding = null
    }
}