package com.example.momolabfe.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordOcrBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class RecordOcrFragment : Fragment() {

    private var _binding: FragmentRecordOcrBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordOcrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        binding.closeIv.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.cameraCv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, RecordWrite01Fragment()) // 카메라
                .addToBackStack(null)
                .commit()
        }

        binding.albumCv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, RecordWrite01Fragment()) // 앨범
                .addToBackStack(null)
                .commit()
        }
    }
}