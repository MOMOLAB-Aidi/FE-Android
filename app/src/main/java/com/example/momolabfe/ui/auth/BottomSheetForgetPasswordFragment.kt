package com.example.momolabfe.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.momolabfe.databinding.BottomSheetForgetPasswordBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.core.net.toUri

class BottomSheetForgetPasswordFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetForgetPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetForgetPasswordBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 전화번호 클릭 → 전화 앱 열기 (ACTION_DIAL: 권한 필요 없음)
        binding.phoneNumTv.setOnClickListener {
            val rawPhone = "010-1234-1234"
            val phone = rawPhone.replace("-", "") // tel:01012341234 형태로 변환
            val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
            startActivity(intent)
        }

        // 이메일 클릭 → 이메일 앱 열기
        binding.emailTv.setOnClickListener {
            val email = "aidi2025@gmail.com"
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:$email".toUri()
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}