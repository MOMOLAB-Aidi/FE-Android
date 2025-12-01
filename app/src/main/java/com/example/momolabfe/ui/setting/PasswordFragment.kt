package com.example.momolabfe.ui.setting

import android.os.Bundle
import android.text.InputType
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
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentPasswordBinding
import com.example.momolabfe.remote.user.model.UpdatePassword
import com.example.momolabfe.ui.setting.viewModel.SettingViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class PasswordFragment : Fragment() {

    private var _binding: FragmentPasswordBinding? = null
    private val binding get() = _binding!!

    private var isCurrentPasswordVisible = false
    private var isNewPasswordVisible = false
    private var isNewPasswordCheckVisible = false

    private val viewModel: SettingViewModel by activityViewModels()

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

        setupPasswordToggle()
        setupObservers()

        binding.cancelBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.changeBtn.setOnClickListener {
            submitPasswordChange()
        }
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

    private fun submitPasswordChange() {
        val current = binding.currentPasswordEt.text?.toString()?.trim() ?: ""
        val newPw = binding.newPasswordEt.text?.toString()?.trim() ?: ""
        val newPwCheck = binding.newPasswordCheckEt.text?.toString()?.trim() ?: ""

        // 입력 체크
        if (current.isEmpty() || newPw.isEmpty() || newPwCheck.isEmpty()) {
            showError("모든 비밀번호 항목을 입력해주세요.")
            return
        }

        if (newPw != newPwCheck) {
            showError("새 비밀번호와 확인이 일치하지 않습니다.")
            return
        }

        val request = UpdatePassword(
            currentPassword = current,
            newPassword = newPw,
            newPasswordCheck = newPwCheck
        )

        binding.changeBtn.isEnabled = false

        viewModel.updatePassword(request)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.passwordSuccess.collect {
                        clearError()
                        Toast.makeText(requireContext(), "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                        binding.changeBtn.isEnabled = true
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            Log.e("PASSWORD_FRAGMENT", errorMsg.toString())
            binding.changeBtn.isEnabled = true
        }
    }

    // 에러 표기
    private fun showError(message: String) {
        binding.passwordErrorTv.text = message
        binding.passwordErrorTv.visibility = View.VISIBLE
    }

    private fun clearError() {
        binding.passwordErrorTv.text = ""
        binding.passwordErrorTv.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}