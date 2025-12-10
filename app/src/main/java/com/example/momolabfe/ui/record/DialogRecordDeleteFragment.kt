package com.example.momolabfe.ui.record

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.momolabfe.databinding.DialogRecordDeleteBinding
import com.example.momolabfe.ui.record.viewModel.RecordViewModel

class DialogRecordDeleteFragment : DialogFragment() {

    private var _binding: DialogRecordDeleteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecordViewModel by activityViewModels()
    private var recordId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRecordDeleteBinding.inflate(inflater, container, false)
        recordId = arguments?.getLong("record_id") ?: -1L
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.confirmTv.setOnClickListener {
            if (recordId == -1L) {
                Log.e("RecordDeleteDialog", "recordId가 유효하지 않습니다.")
                dismiss()
                return@setOnClickListener
            }

            binding.confirmTv.isEnabled = false
            viewModel.deleteRecord(recordId)

            dismiss()
        }

        binding.cancelTv.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}