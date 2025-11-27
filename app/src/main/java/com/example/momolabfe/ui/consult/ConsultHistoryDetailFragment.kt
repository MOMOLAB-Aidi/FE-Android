package com.example.momolabfe.ui.consult

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentConsultHistoryDetailBinding
import com.example.momolabfe.ui.consult.adapter.ChatAdapter
import com.example.momolabfe.ui.consult.data.ChatMessage
import com.example.momolabfe.ui.consult.viewModel.ConsultViewModel
import com.example.momolabfe.remote.consult.data.MessageRole
import com.google.android.material.bottomnavigation.BottomNavigationView

class ConsultHistoryDetailFragment : Fragment() {

    companion object {
        private const val ARG_SESSION_ID = "session_id"

        fun newInstance(sessionId: String) = ConsultHistoryDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_SESSION_ID, sessionId)
            }
        }
    }

    private var _binding: FragmentConsultHistoryDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConsultViewModel by activityViewModels()
    private val chatAdapter by lazy { ChatAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConsultHistoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        setupRecyclerView()
        setupObservers()

        val sessionId = arguments?.getString(ARG_SESSION_ID)
        if (!sessionId.isNullOrBlank()) {
            viewModel.getConsult(sessionId)
        }
    }

    private fun setupRecyclerView() {
        binding.chatRv.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = false
            }
            itemAnimator = null
        }
    }

    private fun setupObservers() {
        viewModel.consult.observe(viewLifecycleOwner) { list ->
            val chatList: List<ChatMessage> = list.mapIndexed { index, item ->
                ChatMessage(
                    id = index.toLong(),
                    text = item.content,
                    isUser = (item.role == MessageRole.USER)
                )
            }
            chatAdapter.submitList(chatList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}