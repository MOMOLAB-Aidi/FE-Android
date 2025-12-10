package com.example.momolabfe.ui.auth

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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

        val rawPhone = "1522-2025"
        val email = "aidi2025@gmail.com"
        val emailSubject = "비밀번호 재설정 문의"

        val phoneText = "에이디 연락처: $rawPhone"
        val phoneSpannable = SpannableString(phoneText)
        val phoneStart = phoneText.indexOf(rawPhone).takeIf { it >= 0 } ?: 0
        val phoneEnd = phoneStart + rawPhone.length

        phoneSpannable.setSpan(
            UnderlineSpan(),
            phoneStart,
            phoneEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        phoneSpannable.setSpan(
            StyleSpan(Typeface.BOLD),
            phoneStart,
            phoneEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        phoneSpannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), com.example.momolabfe.R.color.text_primary)),
            phoneStart,
            phoneEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.phoneNumTv.text = phoneSpannable


        // 이메일 밑줄 + 강조
        val emailText = "에이디 이메일: $email"
        val emailSpannable = SpannableString(emailText)
        val emailStart = emailText.indexOf(email).takeIf { it >= 0 } ?: 0
        val emailEnd = emailStart + email.length

        emailSpannable.setSpan(
            UnderlineSpan(),
            emailStart,
            emailEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        emailSpannable.setSpan(
            StyleSpan(Typeface.BOLD),
            emailStart,
            emailEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        emailSpannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), com.example.momolabfe.R.color.text_primary)),
            emailStart,
            emailEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.emailTv.text = emailSpannable

        // 전화번호 클릭 → 전화 앱 열기 (ACTION_DIAL: 권한 필요 없음)
        binding.phoneNumTv.setOnClickListener {
            val phone = rawPhone.replace("-", "") // tel:01012341234 형태로 변환
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.fromParts("tel", phone, null)
            }
            startActivity(intent)
        }

        // 이메일 클릭 → 이메일 앱 열기
        binding.emailTv.setOnClickListener {

            val uri = "mailto:$email?subject=${Uri.encode(emailSubject)}".toUri()
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}