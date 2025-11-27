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
import com.example.momolabfe.remote.record.data.DayWeek
import com.example.momolabfe.remote.record.data.Turbidity
import com.example.momolabfe.ui.record.adapter.RecordExchangeInfoAdapter
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.time.LocalDate

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
        Log.d("RECORD_INFO_FRAGMENT", "onCreateView recordId=$recordId")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        adapter = RecordExchangeInfoAdapter(emptyList())
        binding.exchangeInfoRv.adapter = adapter

        if (recordId != -1L) {
            viewModel.getRecord(recordId)
        }

        setupObservers()

        binding.editBtn.setOnClickListener {

            if (recordId == -1L) {
                return@setOnClickListener
            }

            val fragment = RecordEditFragment().apply {
                arguments = Bundle().apply {
                    putLong("record_id", recordId)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupObservers() {
        viewModel.record.observe(viewLifecycleOwner) { recordItem ->
            if (recordItem != null) {
                binding.contentTv.text = formatRecordDate(recordItem.recordDate, recordItem.recordDw)
                binding.weightValueTv.text = recordItem.weight.toString()
                binding.bloodPressureSystolicTv.text = recordItem.systolic.toString()
                binding.bloodPressureDiastolicTv.text = recordItem.diastolic.toString()
                binding.fastingGlucoseValueTv.text = recordItem.fastingGlucose.toString()
                binding.urineCountValueTv.text = recordItem.urineCount.toString()

                // 복막액 혼탁 표기
                binding.turbidityValueTv.text = when (recordItem.turbidity) {
                    Turbidity.NONE -> "없음"
                    Turbidity.PRESENT -> "있음"
                }

                binding.totalUfValueTv.text = recordItem.totalUf.toString()
                binding.noteValueTv.text = recordItem.notes

                // 회차별 정보 바인딩
                adapter.updateList(recordItem.exchanges)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            Log.e("RECORD_INFO_FRAGMENT", errorMsg.toString())
        }
    }

    private fun formatRecordDate(date: LocalDate?, dw: DayWeek?): String {
        if (date == null) return ""

        val year = date.year
        val month = date.monthValue
        val day = date.dayOfMonth
        val dayKorean = convertDayWeekToKorean(dw)

        // 예: 2025년 11월 14일 금
        return "${year}년 ${month}월 ${day}일 $dayKorean"
    }

    private fun convertDayWeekToKorean(dw: DayWeek?): String {
        return when (dw) {
            DayWeek.MON -> "월"
            DayWeek.TUE -> "화"
            DayWeek.WED -> "수"
            DayWeek.THU -> "목"
            DayWeek.FRI -> "금"
            DayWeek.SAT -> "토"
            DayWeek.SUN -> "일"
            else -> ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.VISIBLE // 가시성 복원
        _binding = null
    }
}