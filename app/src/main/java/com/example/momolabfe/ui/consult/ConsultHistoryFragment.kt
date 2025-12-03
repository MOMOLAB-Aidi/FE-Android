package com.example.momolabfe.ui.consult

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.momolabfe.databinding.FragmentConsultHistoryBinding
import com.example.momolabfe.ui.consult.adapter.ConsultHistoryAdapter
import com.example.momolabfe.ui.consult.viewModel.ConsultViewModel
import com.example.momolabfe.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class ConsultHistoryFragment : Fragment() {

    private var _binding: FragmentConsultHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConsultViewModel by activityViewModels()

    private val historyAdapter by lazy {
        ConsultHistoryAdapter(
            onClick = { item ->
                val fragment = ConsultHistoryDetailFragment.newInstance(item.sessionId)

                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_frm, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDelete = { item ->
                viewModel.deleteConsult(item.sessionId)
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConsultHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        setupRecyclerView()
        setupObservers()

        // 서버에서 상담 목록 조회
        viewModel.getConsultList()
    }

    private fun setupRecyclerView() {
        binding.historyRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
            itemAnimator = null
        }
    }

    private fun setupObservers() {
        viewModel.history.observe(viewLifecycleOwner) { list ->
            historyAdapter.submitList(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}