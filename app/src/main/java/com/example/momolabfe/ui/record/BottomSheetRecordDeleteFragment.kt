package com.example.momolabfe.ui.record

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.momolabfe.databinding.BottomSheetRecordDeleteBinding
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class BottomSheetRecordDeleteFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRecordDeleteBinding? = null
    private val binding get() = _binding!!

    private var recordId: Long = -1L
    private val viewModel: RecordViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRecordDeleteBinding.inflate(inflater, container, false)

        recordId = arguments?.getLong("record_id") ?: -1L

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        binding.confirmTv.setOnClickListener {
            binding.confirmTv.isEnabled = false

            if (recordId == -1L) {
                Log.e("BottomSheetRecordDelete", "recordId가 유효하지 않습니다.")
                dismiss()
                return@setOnClickListener
            }

            viewModel.deleteRecord(recordId)
        }

        binding.cancelTv.setOnClickListener {
            dismiss()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.deleteSuccess.collect {
                        Log.d("RECORD_INFO_FRAGMENT", "기록이 성공적으로 삭제되었습니다.")
                        parentFragmentManager.setFragmentResult("record_delete", Bundle())
                        dismiss()
                    }
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            Log.e("RECORD_INFO_FRAGMENT", errorMsg.toString())
            binding.confirmTv.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}