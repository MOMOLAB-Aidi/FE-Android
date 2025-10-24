package com.example.momolabfe.ui.record

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordCommonInfoBinding
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CommonRecordInfoFragment : Fragment() {

    private var _binding: FragmentRecordCommonInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordCommonInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 전달된 날짜 문자열 꺼내기 (기본값: 오늘 날짜 포맷 or 빈 문자열)
        val dateText = arguments?.getString("selected_date_text")
            ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))

        binding.dateTv.text = dateText

        setupUI()
    }

    private fun setupUI() {

        // 텍스트 변경 감지 → 버튼 상태 갱신
        val watcher: (Editable?) -> Unit = { updateNextButtonState() }
        binding.weightEt.addTextChangedListener(afterTextChanged = watcher)
        binding.systolicEt.addTextChangedListener(afterTextChanged = watcher)
        binding.diastolicEt.addTextChangedListener(afterTextChanged = watcher)
        binding.fastingGlucoseEt.addTextChangedListener(afterTextChanged = watcher)
        binding.urineCountEt.addTextChangedListener(afterTextChanged = watcher)

        // 체크박스 변경 감지
        binding.turbidityNCheckbox.setOnCheckedChangeListener { button, isChecked ->
            if (isChecked) {
                binding.turbidityYCheckbox.isChecked = false
                setCheckBoxTint(binding.turbidityYCheckbox, false)
            }
            setCheckBoxTint(button, isChecked)
            updateNextButtonState()
        }
        binding.turbidityYCheckbox.setOnCheckedChangeListener { button, isChecked ->
            if (isChecked) {
                binding.turbidityNCheckbox.isChecked = false
                setCheckBoxTint(binding.turbidityYCheckbox, false)
            }
            setCheckBoxTint(button, isChecked)
            updateNextButtonState()
        }

        // 초기 진입 시 현재 체크 상태에 맞춰 틴트 정렬
        listOf(binding.turbidityNCheckbox, binding.turbidityYCheckbox).forEach {
            setCheckBoxTint(it, it.isChecked)
        }

        updateNextButtonState()

        binding.nextBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, RecordExchangeInfoFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun updateNextButtonState() {
        val weight = binding.weightEt.text?.toString()?.trim().orEmpty()
        val systolic = binding.systolicEt.text?.toString()?.trim().orEmpty()
        val diastolic = binding.diastolicEt.text?.toString()?.trim().orEmpty()
        val fastingGlucose = binding.fastingGlucoseEt.text?.toString()?.trim().orEmpty()
        val urineCount = binding.urineCountEt.text?.toString()?.trim().orEmpty()

        val turbidityChecked =
            binding.turbidityNCheckbox.isChecked || binding.turbidityYCheckbox.isChecked

        val enabled = isValidWeight(weight) &&
                isValidBpSys(systolic) &&
                isValidBpDia(diastolic) &&
                isValidBpRelation(systolic, diastolic) &&
                isValidGlucose(fastingGlucose) &&
                isValidUrineFreq(urineCount) &&
                turbidityChecked

        binding.nextBtn.isEnabled = enabled
        binding.nextBtn.setBackgroundColor(
            if (enabled) requireContext().getColor(R.color.text_primary) else Color.parseColor("#F6F6F6")
        )
        binding.nextBtn.setTextColor(
            if (enabled) requireContext().getColor(R.color.white) else requireContext().getColor(R.color.text_primary)
        )
    }

    // 체중(kg): 20.0 ~ 300.0
    private fun isValidWeight(s: String): Boolean {
        val bd = s.toBigDecimalOrNull() ?: return false
        return bd >= BigDecimal("20.0") && bd <= BigDecimal("300.0")
    }

    // 혈압 최고(수축): 70 ~ 240
    private fun isValidBpSys(s: String): Boolean =
        s.toIntOrNull()?.let { it in 70..240 } == true

    // 혈압 최저(이완): 40 ~ 160
    private fun isValidBpDia(s: String): Boolean =
        s.toIntOrNull()?.let { it in 40..160 } == true

    // 혈압 최고(수축) > 혈압 최저(이완)
    private fun isValidBpRelation(sys: String, dia: String): Boolean {
        val s = sys.toIntOrNull() ?: return false
        val d = dia.toIntOrNull() ?: return false
        return s > d
    }

    // 공복 혈당: 40 ~ 600
    private fun isValidGlucose(s: String): Boolean =
        s.toIntOrNull()?.let { it in 40..600 } == true

    // 소변 횟수: 0 ~ 50
    private fun isValidUrineFreq(s: String): Boolean =
        s.toIntOrNull()?.let { it in 0..50 } == true

    private fun setCheckBoxTint(checkBox: CompoundButton, isChecked: Boolean) {
        val color = ContextCompat.getColor(
            requireContext(), if (isChecked) R.color.text_primary else R.color.gray
        )
        checkBox.buttonTintList = ColorStateList.valueOf(color)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}