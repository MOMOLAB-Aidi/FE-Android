package com.example.momolabfe.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.data.remote.record.model.DayWeek
import com.example.momolabfe.databinding.FragmentRecordExchangeInfoBinding
import com.example.momolabfe.utils.korean
import com.example.momolabfe.utils.toDayWeek
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RecordExchangeInfoFragment : Fragment() {

    private var _binding: FragmentRecordExchangeInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordExchangeInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        // 전달된 날짜 문자열 꺼내기
        val dateText = arguments?.getString("record_date_text").orEmpty()
        val dwKorean = arguments?.getString("record_dw_korean").orEmpty()

        binding.dateTv.text = dateText
        binding.dwTv.text = dwKorean

        binding.searchBtn.setOnClickListener {
            val args = Bundle().apply {
                putString("selected_date_text", dateText)
                putString("selected_date_dw", dwKorean)
            }
            val fragment = RecordExchangeListFragment().apply { arguments = args }

            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
}