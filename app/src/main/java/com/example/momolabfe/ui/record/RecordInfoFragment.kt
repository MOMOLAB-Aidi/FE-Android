package com.example.momolabfe.ui.record

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordInfoBinding
import com.example.momolabfe.ui.record.adapter.RecordExchangeInfoAdapter
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class RecordInfoFragment : Fragment() {

    private var _binding: FragmentRecordInfoBinding? = null
    private val binding get() = _binding!!

    private var recordId: Long = -1L

    private lateinit var adapter: RecordExchangeInfoAdapter
    private val viewModel: RecordViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordInfoBinding.inflate(inflater, container, false)

        recordId = arguments?.getLong("record_id") ?: -1L

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.main_bnv)
        bottomNav?.visibility = View.VISIBLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RecordExchangeInfoAdapter(emptyList())
        binding.exchangeInfoRv.adapter = adapter

        if (recordId != -1L) {
            viewModel.getRecord(recordId)
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.record.observe(viewLifecycleOwner) { recordItem ->
            if (recordItem != null) {
                binding.weightValueTv.text = recordItem.weight.toString()
                binding.bloodPressureSystolicTv.text = recordItem.systolic.toString()
                binding.bloodPressureDiastolicTv.text = recordItem.diastolic.toString()
                binding.fastingGlucoseValueTv.text = recordItem.fastingGlucose.toString()
                binding.urineCountValueTv.text = recordItem.urineCount.toString()
                binding.turbidityValueTv.text = recordItem.turbidity.toString()
                binding.totalUfValueTv.text = recordItem.totalUf.toString()
                binding.noteValueTv.text = recordItem.notes
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            Log.e("RECORD_INFO_FRAGMENT", errorMsg.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}