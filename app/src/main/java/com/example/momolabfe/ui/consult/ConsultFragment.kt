package com.example.momolabfe.ui.consult

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

        // 처음 진입 시 세션 없으면 상담 세션 생성
        if (currentSessionId == null) {
            viewModel.startConsult()
        }

        binding.sendBtn.setOnClickListener {

            val message = binding.messageInput.text.toString().trim()

            if (message.isBlank()) {
                Toast.makeText(requireContext(), "내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sessionId = currentSessionId
            if (sessionId.isNullOrBlank()) {
                Toast.makeText(requireContext(), "상담 세션이 없습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 환자가 보낸 메시지를 오른쪽 말풍선으로 먼저 추가
            viewModel.appendUserMessage(message)

            val request = ChatRequest(
                sessionId = sessionId,
                message = message
            )

            // 채팅 스트리밍 시작
            viewModel.chatStream(request)

            // 입력창 비우기
            binding.messageInput.text?.clear()
        }

        binding.plusIv.setOnClickListener {
            endCurrentSessionIfNeeded()
            viewModel.resetMessages()
            viewModel.startConsult()
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
        }
    }

    private fun setupObservers() {
        viewModel.startConsult.observe(viewLifecycleOwner) { response ->
            currentSessionId = response.sessionId

            viewModel.resetMessages() // 새 세션이면 이전 채팅 비우기
        }

        viewModel.messages.observe(viewLifecycleOwner) { list ->
            chatAdapter.submitList(list)
            if (list.isNotEmpty()) {
                binding.chatRv.post {
                    binding.chatRv.scrollToPosition(list.size - 1)
                }
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

    override fun onDestroyView() {
        endCurrentSessionIfNeeded()
        super.onDestroyView()
        _binding = null
    }
}