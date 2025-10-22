package com.example.momolabfe.ui.onboarding

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentOnboardingUserInfoBinding
import com.example.momolabfe.ui.record.SelectRecordMethodFragment
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class OnboardingUserInfoFragment : Fragment() {

    private var _binding: FragmentOnboardingUserInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingUserInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        binding.nameEt.filters = arrayOf(InputFilter.LengthFilter(10))
        binding.phoneEt.filters = arrayOf(InputFilter.LengthFilter(11))
        binding.registEt.filters = arrayOf(InputFilter.LengthFilter(6))
        binding.genderEt.filters = arrayOf(InputFilter.LengthFilter(1))

        // 텍스트 변경 감지 → 버튼 상태 갱신
        val watcher: (Editable?) -> Unit = { updateNextButtonState() }
        binding.nameEt.addTextChangedListener(afterTextChanged = watcher)
        binding.phoneEt.addTextChangedListener(afterTextChanged = watcher)
        binding.registEt.addTextChangedListener(afterTextChanged = watcher)
        binding.genderEt.addTextChangedListener(afterTextChanged = watcher)

        updateNextButtonState()

        binding.nextBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, SelectRecordMethodFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun updateNextButtonState() {
        val name = binding.nameEt.text?.toString()?.trim().orEmpty()
        val phone = binding.phoneEt.text?.toString()?.trim().orEmpty()
        val birth = binding.registEt.text?.toString()?.trim().orEmpty()
        val gender = binding.genderEt.text?.toString()?.trim().orEmpty()

        val enabled = isValidName(name) &&
                isValidPhone(phone) &&
                isValidBirth(birth) &&
                isValidGenderDigit(gender)

        binding.nextBtn.isEnabled = enabled
        binding.nextBtn.setBackgroundColor(
            if (enabled) requireContext().getColor(R.color.text_primary) else Color.parseColor("#F6F6F6")
        )
        binding.nextBtn.setTextColor(
            if (enabled) requireContext().getColor(R.color.white) else requireContext().getColor(R.color.text_primary)
        )
    }

    // 이름: 공백이 아닌 1~10자
    private fun isValidName(name: String): Boolean {
        return name.isNotBlank() && name.length <= 10
    }

    // 전화번호: 숫자 11자리 (010 포함)
    private fun isValidPhone(phone: String): Boolean {
        return phone.matches(Regex("^010\\d{8}$"))
    }

    // 생년월일: 숫자 6자리 + 실제 달력상 유효한 날짜(YYMMDD)인지 검증
    private fun isValidBirth(birth: String): Boolean {
        if (!birth.matches(Regex("^\\d{6}$"))) return false
        val sdf = SimpleDateFormat("yyMMdd", Locale.KOREA).apply { isLenient = false }
        return try {
            sdf.parse(birth) != null
        } catch (_: ParseException) {
            false
        }
    }

    // 주민등록번호 뒷자리의 첫 숫자: 1(남자) | 2(여자) | 3(남자) | 4(여자)
    private fun isValidGenderDigit(g: String): Boolean {
        return g.matches(Regex("^[1-4]$"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}