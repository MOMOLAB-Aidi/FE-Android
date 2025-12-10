package com.example.momolabfe.ui.setting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.momolabfe.databinding.FragmentCustomerInquiryBinding
import androidx.core.net.toUri

class CustomerInquiryFragment : Fragment() {

    private var _binding: FragmentCustomerInquiryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerInquiryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.callButton.setOnClickListener {
            val phone = binding.customerCenterPhoneTv.text.toString()
            val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
            startActivity(intent)
        }

        binding.sendEmailButton.setOnClickListener {
            val email = binding.customerCenterEmailTv.text.toString()
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:$email".toUri()
            }
            startActivity(intent)
        }

        binding.openKakaoButton.setOnClickListener {
            val url = "https://pf.kakao.com/aidi"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}