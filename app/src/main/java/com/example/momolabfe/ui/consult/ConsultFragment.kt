package com.example.momolabfe.ui.consult

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
import com.example.momolabfe.remote.consult.data.ChatRequest
import com.example.momolabfe.remote.consult.data.SessionEndRequest
import com.example.momolabfe.ui.consult.adapter.ChatAdapter
import com.example.momolabfe.ui.consult.viewModel.ConsultViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class ConsultFragment : Fragment() {

    private var _binding: FragmentConsultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConsultViewModel by activityViewModels()

    // 발급받은 세션 ID 저장
    private var currentSessionId: String? = null

    private val chatAdapter by lazy { ChatAdapter() }

    private var lastItemCount: Int = 0 // 마지막 아이템 개수 기억

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

        setupRecyclerView()
        setupObservers()
        showQuickQuestions()
        setupSendButtonState()

        // 처음 진입 시 세션 없으면 상담 세션 생성
        if (currentSessionId == null) {
            viewModel.startConsult()
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
            endCurrentSessionIfNeeded()
            viewModel.resetMessages()
            viewModel.startConsult()
            showQuickQuestions()
        }

        // Chip 빠른 질문
        binding.chip1.setOnClickListener {
            setQuickQuestionAndSend("제수량이 잘 안 나와요")
        }
        binding.chip2.setOnClickListener {
            setQuickQuestionAndSend("식단 관리하는 방법")
        }
        binding.chip3.setOnClickListener {
            setQuickQuestionAndSend("복통이 있어요")
        }
        binding.chip4.setOnClickListener {
            setQuickQuestionAndSend("혈압이 높아요")
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
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Log.e("CONSULT_FRAGMENT", errorMsg)
            }
        }
    }

    private fun setQuickQuestionAndSend(text: String) {
        binding.messageInput.setText(text)
        binding.messageInput.setSelection(text.length)
    }

    private fun endCurrentSessionIfNeeded() {
        val sessionId = currentSessionId ?: return  // 세션 없으면 아무것도 안 함

        val request = SessionEndRequest(sessionId = sessionId)
        viewModel.endConsult(request)

        // 클라이언트 쪽 상태 초기화
        currentSessionId = null
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

    override fun onDestroyView() {
        endCurrentSessionIfNeeded()
        super.onDestroyView()
        _binding = null
    }
}