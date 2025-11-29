package com.example.momolabfe.ui.record

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.NumberPicker
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.momolabfe.R
import com.example.momolabfe.databinding.DialogTimePickerBinding
import com.example.momolabfe.databinding.FragmentRecordEditBinding
import com.example.momolabfe.remote.record.model.DayWeek
import com.example.momolabfe.remote.record.model.RecordExchangeGetResponse
import com.example.momolabfe.remote.record.model.RecordExchangeUpdateRequest
import com.example.momolabfe.remote.record.model.RecordUpdateRequest
import com.example.momolabfe.remote.record.model.Turbidity
import com.example.momolabfe.ui.record.adapter.RecordExchangeEditAdapter
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class RecordEditFragment : Fragment() {

    private var _binding: FragmentRecordEditBinding? = null
    private val binding get() = _binding!!

    private var recordId: Long = -1L

    private val viewModel: RecordViewModel by activityViewModels()

    private lateinit var adapter: RecordExchangeEditAdapter

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

        adapter = RecordExchangeEditAdapter().apply {
            onTimePickerClickListener = object : RecordExchangeEditAdapter.OnTimePickerClickListener {
                override fun onTimePickerClick(position: Int, targetEditText: EditText) {
                    showTimePickerDialog(position)
                }
            }
        }
        binding.exchangeInfoRv.adapter = adapter

        setupTurbidityCheckboxes()
        setupObservers()

        if (recordId != -1L && viewModel.record.value?.id != recordId) {
            viewModel.getRecord(recordId)
        }

        binding.saveBtn.setOnClickListener {
            binding.saveBtn.isEnabled = false
            edit()
        }

        binding.cancelBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupTurbidityCheckboxes() {
        // 초기 상태 색상 적용
        val initialNChecked = binding.turbidityNCheckbox.isChecked
        setCheckBoxTint(binding.turbidityNCheckbox, initialNChecked)

        val initialYChecked = binding.turbidityYCheckbox.isChecked
        setCheckBoxTint(binding.turbidityYCheckbox, initialYChecked)

        binding.turbidityNCheckbox.setOnCheckedChangeListener { checkBox, isChecked ->
            setCheckBoxTint(checkBox, isChecked)
            if (isChecked) {
                binding.turbidityYCheckbox.isChecked = false
            }
        }

        binding.turbidityYCheckbox.setOnCheckedChangeListener { checkBox, isChecked ->
            setCheckBoxTint(checkBox, isChecked)
            if (isChecked) {
                binding.turbidityNCheckbox.isChecked = false
            }
        }
    }

    private fun setCheckBoxTint(checkBox: CompoundButton, isChecked: Boolean) {
        val color = ContextCompat.getColor(
            requireContext(),
            if (isChecked) R.color.text_primary else R.color.gray
        )
        checkBox.buttonTintList = ColorStateList.valueOf(color)
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

        val totalUf = binding.totalUfValueEt.text.toString().toIntOrNull()

        val notes = binding.noteValueEt.text.toString().ifBlank { null }

        val currentExchangeList = adapter.items

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
            binding.saveBtn.isEnabled = true
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

            // 체크 상태 바꾼 뒤 색상도 동기화
            setCheckBoxTint(binding.turbidityNCheckbox, binding.turbidityNCheckbox.isChecked)
            setCheckBoxTint(binding.turbidityYCheckbox, binding.turbidityYCheckbox.isChecked)

            // 회차 리스트 세팅
            val exchangesForEdit = record.exchanges.map { item ->
                RecordExchangeGetResponse(
                    id = item.id,
                    exchangeNo = item.exchangeNo,
                    exchangeTime = item.exchangeTime,
                    drainVolume = item.drainVolume,
                    fillConcentration = item.fillConcentration,
                    fillVolume = item.fillVolume,
                    uf = item.uf
                )
            }
            adapter.updateList(exchangesForEdit)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recordSuccess.collect {
                        Log.d("RECORD_EDIT_FRAGMENT", "기록이 성공적으로 수정되었습니다.")
                        parentFragmentManager.popBackStack()
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

    private fun showTimePickerDialog(position: Int) {
        val dialogBinding = DialogTimePickerBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
            .setView(dialogBinding.root)
            .create()

        setupPickersInDialog(dialogBinding)

        dialogBinding.applyTv.setOnClickListener {
            applySelectedTime(dialogBinding, position)
            dialog.dismiss()
        }

        dialogBinding.cancelTv.setOnClickListener {
            dialog.dismiss()
        }

        applyDialogWindow(dialog)
        dialog.show()
    }

    private fun setupPickersInDialog(dialogBinding: DialogTimePickerBinding) {
        dialogBinding.ampmPickerNp.apply {
            minValue = 0
            maxValue = 1
            displayedValues = arrayOf("오전", "오후")
            wrapSelectorWheel = false
            post { applyTextStyleToNumberPicker(this, requireContext()) }
        }

        dialogBinding.hourPickerNp.apply {
            minValue = 1
            maxValue = 12
            wrapSelectorWheel = true
            value = 12
            post { applyTextStyleToNumberPicker(this, requireContext()) }
        }

        dialogBinding.minutePickerNp.apply {
            minValue = 0
            maxValue = 5
            displayedValues = arrayOf("00", "10", "20", "30", "40", "50")
            wrapSelectorWheel = true
            post { applyTextStyleToNumberPicker(this, requireContext()) }
        }
    }

    private fun applySelectedTime(dialogBinding: DialogTimePickerBinding, position: Int) {
        val ampmPicker = dialogBinding.ampmPickerNp
        val hourPicker = dialogBinding.hourPickerNp
        val minutePicker = dialogBinding.minutePickerNp

        val ampm = ampmPicker.value
        val hour12 = hourPicker.value
        val minute = arrayOf("00", "10", "20", "30", "40", "50")[minutePicker.value]

        val hour24 = convertTo24Hour(ampm, hour12)
        val timeText = String.format(Locale.KOREA, "%02d:%s", hour24, minute)

        val currentList = adapter.items
        if (position >= 0 && position < currentList.size) {
            val updated = currentList[position].copy(
                exchangeTime = LocalTime.parse(timeText, RecordExchangeEditAdapter.TIME_FORMATTER)
            )
            currentList[position] = updated
            adapter.notifyItemChanged(position)
        }
    }

    private fun convertTo24Hour(ampm: Int, hour12: Int): Int {
        return when {
            ampm == 0 && hour12 == 12 -> 0
            ampm == 0 -> hour12
            ampm == 1 && hour12 == 12 -> 12
            else -> hour12 + 12
        }
    }

    private fun applyDialogWindow(dialog: AlertDialog) {
        dialog.setOnShowListener {
            dialog.window?.let { window ->
                val layoutParams = window.attributes
                layoutParams.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                layoutParams.gravity = Gravity.CENTER
                layoutParams.dimAmount = 0.8f
                window.attributes = layoutParams
                window.setDimAmount(0.8f)
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }
    }

    private fun applyTextStyleToNumberPicker(picker: NumberPicker, context: Context) {
        try {
            val count = picker.childCount
            for (i in 0 until count) {
                val child = picker.getChildAt(i)
                if (child is EditText) {
                    child.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    child.textSize = 15f
                    child.typeface = ResourcesCompat.getFont(context, R.font.noto_sans_kr_regular)
                    child.includeFontPadding = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}