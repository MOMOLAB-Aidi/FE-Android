package com.example.momolabfe.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class RecordFragment : Fragment() {

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.main_bnv)
        bottomNav?.visibility = View.VISIBLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.manualEntryCv.setOnClickListener {
            it.isEnabled = false

            val fragment = RecordWrite01Fragment().apply {
                arguments = Bundle().apply {
                    putBoolean("fromOcr", false)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, fragment)
                .addToBackStack(null)
                .commit()

            it.postDelayed({ it.isEnabled = true }, 500)
        }

        binding.cameraCv.setOnClickListener {
            it.isEnabled = false

            val fragment = RecordOcrFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("fromOcr", true)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, fragment)
                .addToBackStack(null)
                .commit()

            it.postDelayed({ it.isEnabled = true }, 500)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}