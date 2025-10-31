package com.example.momolabfe.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.data.remote.record.model.DayWeek
import com.example.momolabfe.databinding.FragmentRecordExchangeInfoBinding
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

        // 전달된 날짜 문자열 꺼내기 (기본값: 오늘 날짜 포맷 or 빈 문자열)
        val dateText = arguments?.getString("selected_date_text")
            ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
        val dwKorean = arguments?.getString("selected_date_dw")
            ?.let { DayWeek.valueOf(it).korean() }
            ?: LocalDate.now().dayOfWeek.toDayWeek().korean()

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

    private fun DayWeek.korean(): String = when (this) {
        DayWeek.MON -> "(월)"
        DayWeek.TUE -> "(화)"
        DayWeek.WED -> "(수)"
        DayWeek.THU -> "(목)"
        DayWeek.FRI -> "(금)"
        DayWeek.SAT -> "(토)"
        DayWeek.SUN -> "(일)"
    }

    // DayOfWeek → DayWeek 매핑
    private fun DayOfWeek.toDayWeek(): DayWeek = when (this) {
        DayOfWeek.MONDAY    -> DayWeek.MON
        DayOfWeek.TUESDAY   -> DayWeek.TUE
        DayOfWeek.WEDNESDAY -> DayWeek.WED
        DayOfWeek.THURSDAY  -> DayWeek.THU
        DayOfWeek.FRIDAY    -> DayWeek.FRI
        DayOfWeek.SATURDAY  -> DayWeek.SAT
        DayOfWeek.SUNDAY    -> DayWeek.SUN
    }
}