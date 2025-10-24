package com.example.momolabfe.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordExchangeInfoBinding
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

        // 전달된 날짜 문자열 꺼내기 (기본값: 오늘 날짜 포맷 or 빈 문자열)
        val dateText = arguments?.getString("selected_date_text")
            ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))

        binding.dateTv.text = dateText

        binding.searchBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, RecordExchangeListFragment())
                .addToBackStack(null)
                .commit()
        }

    }
}