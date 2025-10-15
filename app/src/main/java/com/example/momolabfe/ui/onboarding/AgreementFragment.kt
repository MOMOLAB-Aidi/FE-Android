package com.example.momolabfe.ui.onboarding

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentAgreementBinding

class AgreementFragment : Fragment() {

    private var _binding: FragmentAgreementBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgreementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        setupCheckBoxListeners()
        setupSelectAllCheckbox()
        updateNextButtonState()

        listOf(
            binding.term1Checkbox,
            binding.term2Checkbox,
            binding.term3Checkbox,
            binding.term4Checkbox,
            binding.term5Checkbox,
            binding.term6Checkbox,
            binding.allCheckbox
        ).forEach { setCheckBoxTint(it, it.isChecked) }
    }

    private fun setupCheckBoxListeners() {
        val listener = { checkBox: CompoundButton ->
            updateNextButtonState()
            syncSelectAllCheckbox()
            setCheckBoxTint(checkBox, checkBox.isChecked)
        }

        listOf(
            binding.term1Checkbox,
            binding.term2Checkbox,
            binding.term3Checkbox,
            binding.term4Checkbox,
            binding.term5Checkbox,
            binding.term6Checkbox
        ).forEach {
            it.setOnCheckedChangeListener { _, _ -> listener(it) }
        }
    }

    private fun updateNextButtonState() {
        val enabled = binding.term1Checkbox.isChecked && binding.term2Checkbox.isChecked
                && binding.term3Checkbox.isChecked && binding.term4Checkbox.isChecked
        binding.nextBtn.isEnabled = enabled
        binding.nextBtn.setBackgroundColor(
            if (enabled) requireContext().getColor(R.color.text_primary) else Color.parseColor("#F6F6F6")
        )
        binding.nextBtn.setTextColor(
            if (enabled) requireContext().getColor(R.color.white) else requireContext().getColor(R.color.text_primary)
        )
    }

    private fun setupSelectAllCheckbox() {
        binding.allCheckbox.setOnCheckedChangeListener { checkBox, isChecked ->
            setAllAgreementChecked(isChecked)
            setCheckBoxTint(checkBox, isChecked)
        }
    }

    private fun syncSelectAllCheckbox() {
        val allChecked = listOf(
            binding.term1Checkbox,
            binding.term2Checkbox,
            binding.term3Checkbox,
            binding.term4Checkbox,
            binding.term5Checkbox,
            binding.term6Checkbox
        ).all { it.isChecked }

        if (binding.allCheckbox.isChecked != allChecked) {
            binding.allCheckbox.setOnCheckedChangeListener(null)
            binding.allCheckbox.isChecked = allChecked
            setCheckBoxTint(binding.allCheckbox, allChecked)
            binding.allCheckbox.setOnCheckedChangeListener { _, isChecked ->
                setAllAgreementChecked(isChecked)
                setCheckBoxTint(binding.allCheckbox, isChecked)
            }
        }
    }

    private fun setAllAgreementChecked(isChecked: Boolean) {
        listOf(
            binding.term1Checkbox,
            binding.term2Checkbox,
            binding.term3Checkbox,
            binding.term4Checkbox,
            binding.term5Checkbox,
            binding.term6Checkbox
        ).forEach { it.isChecked = isChecked }
    }

    private fun setCheckBoxTint(checkBox: CompoundButton, isChecked: Boolean) {
        val color = ContextCompat.getColor(
            requireContext(), if (isChecked) R.color.text_primary else R.color.gray
        )
        checkBox.buttonTintList = ColorStateList.valueOf(color)
    }
}