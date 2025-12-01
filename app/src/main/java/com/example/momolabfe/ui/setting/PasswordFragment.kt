package com.example.momolabfe.ui.setting

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentPasswordBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class PasswordFragment : Fragment() {

    private var _binding: FragmentPasswordBinding? = null
    private val binding get() = _binding!!

    private var isCurrentPasswordVisible = false
    private var isNewPasswordVisible = false
    private var isNewPasswordCheckVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        binding.cancelBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupPasswordToggle()
    }

    private fun setupPasswordToggle() {
        // 현재 비밀번호
        setupSinglePasswordToggle(
            til = binding.currentPasswordTil,
            et = binding.currentPasswordEt,
            getVisible = { isCurrentPasswordVisible },
            setVisible = { isCurrentPasswordVisible = it }
        )

        // 새 비밀번호
        setupSinglePasswordToggle(
            til = binding.newPasswordTil,
            et = binding.newPasswordEt,
            getVisible = { isNewPasswordVisible },
            setVisible = { isNewPasswordVisible = it }
        )

        // 새 비밀번호 확인
        setupSinglePasswordToggle(
            til = binding.newPasswordCheckTil,
            et = binding.newPasswordCheckEt,
            getVisible = { isNewPasswordCheckVisible },
            setVisible = { isNewPasswordCheckVisible = it }
        )
    }

    private fun setupSinglePasswordToggle(
        til: TextInputLayout,
        et: TextInputEditText,
        getVisible: () -> Boolean,
        setVisible: (Boolean) -> Unit
    ) {
        til.setEndIconOnClickListener {
            val newVisible = !getVisible()
            setVisible(newVisible)

            if (newVisible) {
                // 비밀번호 보이기
                et.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                til.setEndIconDrawable(R.drawable.ic_eye_opened_sv)
            } else {
                // 비밀번호 숨기기
                et.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                til.setEndIconDrawable(R.drawable.ic_eye_closed_sv)
            }

            // 커서를 텍스트 끝으로 이동
            et.setSelection(et.text?.length ?: 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}