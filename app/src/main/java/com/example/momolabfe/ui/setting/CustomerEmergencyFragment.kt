package com.example.momolabfe.ui.setting

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.momolabfe.databinding.FragmentCustomerEmergencyBinding
import com.example.momolabfe.ui.setting.viewModel.SettingViewModel
import kotlinx.coroutines.launch

class CustomerEmergencyFragment : Fragment() {

    private var _binding: FragmentCustomerEmergencyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingViewModel by activityViewModels()

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

        viewModel.getHospitalInfo()
        observeHospitalResult()

        binding.emergencyCallBtn.setOnClickListener {
            val phone = binding.emergencyPhoneTv.text.toString()
            val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
            startActivity(intent)
        }
    }

    private fun observeHospitalResult() {
        viewModel.getHospitalResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                binding.emergencyHospitalNameTv.text = it.name
                binding.emergencyPhoneTv.text = it.emergencyPhone
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorEvent.collect { errorMsg ->
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}