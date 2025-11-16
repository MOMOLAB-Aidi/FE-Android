package com.example.momolabfe.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.momolabfe.R
import com.example.momolabfe.data.remote.auth.data.LoginRequest
import com.example.momolabfe.databinding.FragmentLoginBinding
import com.example.momolabfe.ui.auth.viewModel.AuthViewModel
import com.example.momolabfe.ui.main.HomeFragment
import com.example.momolabfe.utils.TokenManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var tokenManager: TokenManager

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

        observeLoginResult()

        binding.loginBtn.setOnClickListener {
            val loginId = binding.idEt.text.toString()
            val password = binding.passwordEt.text.toString()

            if (loginId.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "아이디와 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = LoginRequest(loginId, password)
            viewModel.login(request)
        }
    }

    private fun observeLoginResult() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginSuccess.collectLatest { authResponse ->
                Log.d("Login", "로그인 성공!")

                // Fragment에서 직접 토큰 저장
                tokenManager.saveTokens(
                    accessToken = authResponse.tokens.accessToken,
                    refreshToken = authResponse.tokens.refreshToken
                )

                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_frm, HomeFragment())
                    .commit()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Log.e("Login", "로그인 실패: $it")
                Toast.makeText(requireContext(), "아이디 또는 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}