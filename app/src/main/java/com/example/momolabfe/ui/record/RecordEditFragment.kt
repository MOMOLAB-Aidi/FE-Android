package com.example.momolabfe.ui.record

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordEditBinding
import com.example.momolabfe.remote.record.model.DayWeek
import com.example.momolabfe.remote.record.model.RecordExchangeGetResponse
import com.example.momolabfe.remote.record.model.RecordExchangeUpdateRequest
import com.example.momolabfe.remote.record.model.RecordUpdateRequest
import com.example.momolabfe.remote.record.model.Turbidity
import com.example.momolabfe.ui.record.adapter.RecordExchangeInfoAdapter
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.time.LocalDate

class RecordEditFragment : Fragment() {

    private var _binding: FragmentRecordEditBinding? = null
    private val binding get() = _binding!!

    private var recordId: Long = -1L

    private val viewModel: RecordViewModel by activityViewModels()

    private lateinit var adapter: RecordExchangeInfoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordEditBinding.inflate(inflater, container, false)

        recordId = arguments?.getLong("record_id") ?: -1L

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        adapter = RecordExchangeInfoAdapter(emptyList())
        binding.exchangeInfoRv.adapter = adapter

        setupObservers()

        binding.saveBtn.setOnClickListener {
            binding.saveBtn.isEnabled = false
            edit()
        }

        binding.cancelBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun getRecordRequest(): RecordUpdateRequest {
        val currentRecord = viewModel.record.value

        val weight = binding.weightValueEt.text.toString().toDoubleOrNull()
        val systolic = binding.bloodPressureSystolicEt.text.toString().toIntOrNull()
        val diastolic = binding.bloodPressureDiastolicEt.text.toString().toIntOrNull()
        val fastingGlucose = binding.fastingGlucoseValueEt.text.toString().toIntOrNull()
        val urineCount = binding.urineCountValueEt.text.toString().toIntOrNull()

        val turbidity = when {
            binding.turbidityYCheckbox.isChecked -> Turbidity.PRESENT
            binding.turbidityNCheckbox.isChecked -> Turbidity.NONE
            else -> null
        }

        val totalUf = binding.totalUfValueEt.text.toString()
            .filter { it.isDigit() }
            .toIntOrNull()

        val notes = binding.noteValueEt.text.toString().ifBlank { null }

        val currentExchangeList: List<RecordExchangeGetResponse> =
            adapter.getItems()

        val exchanges = currentExchangeList.map { item ->
            RecordExchangeUpdateRequest(
                id = item.id,
                exchangeTime = item.exchangeTime.toString(),
                drainVolume = item.drainVolume,
                fillVolume = item.fillVolume,
                fillConcentration = item.fillConcentration,
                uf = item.uf
            )
        }.takeIf { it.isNotEmpty() }

        return RecordUpdateRequest(
            recordDate = currentRecord?.recordDate,   // 수정하지 않으면 기존 값 유지
            recordDw = currentRecord?.recordDw,
            weight = weight,
            systolic = systolic,
            diastolic = diastolic,
            fastingGlucose = fastingGlucose,
            urineCount = urineCount,
            turbidity = turbidity,
            notes = notes,
            totalUf = totalUf,
            exchanges = exchanges
        )
    }

    private fun edit() {
        // 1순위: ViewModel에 로드된 기록의 id 사용
        val currentRecord = viewModel.record.value
        val currentRecordId = currentRecord?.id ?: -1L

        // 2순위: arguments 로 받은 recordId
        val finalId = if (currentRecordId != -1L) currentRecordId else recordId

        if (finalId == -1L) {
            return
        }

        // 나중에 editSuccess에서 다시 조회할 수 있도록 기억해두기
        recordId = finalId

        val request = getRecordRequest()
        viewModel.updateRecord(finalId, request)
    }

    private fun setupObservers() {
        viewModel.record.observe(viewLifecycleOwner) { record ->
            if (record == null) return@observe

            binding.contentTv.text = formatRecordDate(record.recordDate, record.recordDw)

            // 공통 정보 세팅
            binding.weightValueEt.setText(record.weight.toString())
            binding.bloodPressureSystolicEt.setText(record.systolic.toString())
            binding.bloodPressureDiastolicEt.setText(record.diastolic.toString())
            binding.fastingGlucoseValueEt.setText(record.fastingGlucose.toString())
            binding.urineCountValueEt.setText(record.urineCount.toString())
            binding.totalUfValueEt.setText(record.totalUf.toString())
            binding.noteValueEt.setText(record.notes ?: "")

            when (record.turbidity) {
                Turbidity.PRESENT -> {
                    binding.turbidityYCheckbox.isChecked = true
                    binding.turbidityNCheckbox.isChecked = false
                }
                Turbidity.NONE -> {
                    binding.turbidityYCheckbox.isChecked = false
                    binding.turbidityNCheckbox.isChecked = true
                }
            }

            // 회차 리스트 세팅
            val exchanges: List<RecordExchangeGetResponse> = record.exchanges
            adapter.updateList(exchanges)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recordSuccess.collect {
                        Log.d("RECORD_EDIT_FRAGMENT", "기록이 성공적으로 수정되었습니다.")
                        navigateToRecordInfo()
                    }
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            Log.e("RECORD_EDIT_FRAGMENT", errorMsg.toString())
            binding.saveBtn.isEnabled = true
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

    private fun navigateToRecordInfo() {
        // 1순위: 현재 ViewModel에 로드된 record의 id
        val idFromVm = viewModel.record.value?.id ?: -1L
        // 2순위: arguments 에서 받은 recordId
        val finalId = if (idFromVm != -1L) idFromVm else recordId

        if (finalId == -1L) {
            Log.e("RECORD_EDIT_FRAGMENT", "navigateToRecordInfo: 유효한 recordId가 없어 이동 불가")
            return
        }

        val fragment = RecordInfoFragment().apply {
            arguments = Bundle().apply {
                putLong("record_id", finalId)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frm, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}