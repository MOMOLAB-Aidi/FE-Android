package com.example.momolabfe.ui.auth

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.momolabfe.R
import com.example.momolabfe.remote.auth.model.LoginRequest
import com.example.momolabfe.databinding.FragmentLoginBinding
import com.example.momolabfe.ui.auth.viewModel.AuthViewModel
import com.example.momolabfe.ui.main.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        val initialChecked = binding.idSaveCheckbox.isChecked
        setCheckBoxTint(binding.idSaveCheckbox, initialChecked)

        binding.idSaveCheckbox.setOnCheckedChangeListener { checkBox, isChecked ->
            setCheckBoxTint(checkBox, isChecked)
        }

        setupPasswordToggle()
        observeLoginResult()

        viewModel.getSavedPatientId().observe(viewLifecycleOwner) { savedId ->
            if (!savedId.isNullOrEmpty()) {
                binding.idEt.setText(savedId)
                binding.idSaveCheckbox.isChecked = true
                setCheckBoxTint(binding.idSaveCheckbox, true)
            }
        }

        binding.loginBtn.setOnClickListener {
            val loginId = binding.idEt.text.toString()
            val password = binding.passwordEt.text.toString()

            if (loginId.isBlank() || password.isBlank()) {
                showError("아이디와 비밀번호를 모두 입력해주세요.")
                return@setOnClickListener
            }

            // ID 저장 상태 처리
            if (binding.idSaveCheckbox.isChecked) {
                viewModel.savePatientId(loginId)
            } else {
                viewModel.clearSavedPatientId()
            }

            val request = LoginRequest(loginId, password)
            viewModel.login(request)
        }

        binding.forgetPasswordTv.setOnClickListener {

            // 이미 표시된 BottomSheet가 있는지 확인
            if (parentFragmentManager.findFragmentByTag("BottomSheetForgetPassword") != null) {
                return@setOnClickListener
            }

            BottomSheetForgetPasswordFragment().show(parentFragmentManager, "BottomSheetForgetPassword")
        }

        binding.idEt.addTextChangedListener {
            clearError()
        }

        binding.passwordEt.addTextChangedListener {
            clearError()
        }
    }

    private fun showError(message: String) {
        binding.loginErrorTv.text = message
        binding.loginErrorTv.visibility = View.VISIBLE
    }

    private fun clearError() {
        binding.loginErrorTv.text = ""
        binding.loginErrorTv.visibility = View.GONE
    }

    private fun setupPasswordToggle() {
        binding.passwordTil.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) { // 비밀번호 보이기
                binding.passwordEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.passwordTil.setEndIconDrawable(R.drawable.ic_eye_opened_sv)
            } else { // 비밀번호 숨기기
                binding.passwordEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.passwordTil.setEndIconDrawable(R.drawable.ic_eye_closed_sv)
            }

            // 커서를 텍스트 끝으로 이동
            binding.passwordEt.setSelection(binding.passwordEt.text?.length ?: 0)
        }
    }

    private fun observeLoginResult() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginSuccess.collectLatest {
                    Log.d("Login", "로그인 성공!")

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_frm, HomeFragment())
                        .commit()
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Log.e("Login", "로그인 실패: $it")
                showError("아이디 또는 비밀번호가 일치하지 않습니다.")
            }
        }
    }

    private fun setCheckBoxTint(checkBox: CompoundButton, isChecked: Boolean) {
        val color = ContextCompat.getColor(
            requireContext(), if (isChecked) R.color.text_primary else R.color.gray
        )
        checkBox.buttonTintList = ColorStateList.valueOf(color)
    }

    override fun onDestroyView() {
        viewModel.clearError()
        _binding = null
        super.onDestroyView()
    }
}