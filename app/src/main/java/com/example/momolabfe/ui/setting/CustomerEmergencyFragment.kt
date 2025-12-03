package com.example.momolabfe.ui.setting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.example.momolabfe.databinding.FragmentCustomerEmergencyBinding

class CustomerEmergencyFragment : Fragment() {

    private var _binding: FragmentCustomerEmergencyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerEmergencyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.emergencyCallBtn.setOnClickListener {
            val phone = binding.emergencyPhoneTv.text.toString()
            val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}