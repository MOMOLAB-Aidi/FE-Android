package com.example.momolabfe.ui.consult

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.momolabfe.databinding.FragmentConsultHistoryBinding
import com.example.momolabfe.ui.consult.adapter.ConsultHistoryAdapter
import com.example.momolabfe.ui.consult.viewModel.ConsultViewModel
import com.example.momolabfe.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

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
                showDeleteConfirmDialog(item.sessionId)
            },
            onRetrySummary = { item ->
                viewModel.retrySummaryFromHistory(item.sessionId)
                Toast.makeText(requireContext(), "요약을 생성하고 있어요.", Toast.LENGTH_SHORT).show()
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

        viewModel.summaryResult.observe(viewLifecycleOwner) { row ->
            if (row == null) return@observe

            viewModel.applySummaryToHistory(row) // 기록 화면에서 재시도한 요약도 반영
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorEvent.collect { errorMsg ->
                    Log.e("CONSULT_HISTORY_FRAGMENT", "상담 목록 조회 실패: $errorMsg")
                }
            }
        }
    }

    private fun showDeleteConfirmDialog(sessionId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("상담 삭제")
            .setMessage("해당 상담 기록을 삭제하시겠습니까?\n\n삭제 후에는 되돌릴 수 없습니다.")
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("삭제") { dialog, _ ->
                dialog.dismiss()
                viewModel.deleteConsult(sessionId)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}