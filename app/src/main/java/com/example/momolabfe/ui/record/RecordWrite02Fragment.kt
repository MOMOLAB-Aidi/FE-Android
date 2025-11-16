package com.example.momolabfe.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordWrite02Binding
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecordWrite02Fragment : Fragment() {

    private var _binding: FragmentRecordWrite02Binding? = null
    private val binding get() = _binding!!

    private val viewModel: RecordViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.clearOcr()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordWrite02Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearOcr()
        _binding = null
    }
}