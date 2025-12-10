package com.example.momolabfe.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentHomeBinding
import com.example.momolabfe.databinding.ItemRecentRecordBinding
import com.example.momolabfe.ui.main.viewModel.EducationViewModel
import com.example.momolabfe.ui.record.RecordInfoFragment
import com.example.momolabfe.ui.record.RecordListFragment
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.example.momolabfe.ui.setting.SettingFragment
import com.example.momolabfe.ui.stats.viewModel.StatsViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val recordViewModel: RecordViewModel by activityViewModels()
    private val educationViewModel: EducationViewModel by viewModels()
    private val statsViewModel: StatsViewModel by activityViewModels()

    private var todayRecordId: Long? = null // 오늘 날짜 기록 id 저장용

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.main_bnv)
        bottomNav?.visibility = View.VISIBLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeTodayTipResult()
        educationViewModel.getTodayTip()

        setupObservers()
        recordViewModel.getRecentRecords()
        statsViewModel.getLast7DaysAverage()
        recordViewModel.getTodayExchangeSummary()

        try {
            // 날짜 포맷
            val now = Date()
            val format = SimpleDateFormat("yyyy년 MM월 d일 EEEE", Locale.KOREA)

            val formattedDate = format.format(now)
            binding.contentTv.text = formattedDate

        } catch (e: Exception) {
            // 날짜 포맷팅 중 오류 발생 시 로그 출력
            e.printStackTrace()
        }

        binding.settingIv.setOnClickListener {
            it.isEnabled = false

            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, SettingFragment())
                .addToBackStack(null)
                .commit()

            it.postDelayed({ it.isEnabled = true }, 500)
        }

        binding.recordCv.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.main_bnv)
            bottomNav.selectedItemId = R.id.fragment_record
        }

        binding.detailStatisticsBtn.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.main_bnv)
            bottomNav.selectedItemId = R.id.fragment_statistics
        }

        binding.totalTv.setOnClickListener {
            it.isEnabled = false

            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, RecordListFragment())
                .addToBackStack(null)
                .commit()

            it.postDelayed({ it.isEnabled = true }, 500)
        }

        binding.tipRefreshIv.setOnClickListener {
            educationViewModel.getTodayTip()
        }

        binding.completeCv.setOnClickListener {
            navigateToTodayRecordDetail()
        }

        binding.ufCv.setOnClickListener {
            navigateToTodayRecordDetail()
        }
    }

    private fun setupObservers() {
        recordViewModel.todayExchangeSummary.observe(viewLifecycleOwner) { summary ->
            val defaultColor = ContextCompat.getColor(requireContext(), R.color.secondary_text)

            if (summary == null || !summary.hasRecord) {
                // 오늘 기록이 없을 때 기본 표시
                binding.completeContentTv.text = "-회"
                binding.ufContentTv.text = "-g"
                binding.ufContentTv.setTextColor(defaultColor)
                return@observe
            }

            binding.completeContentTv.text = "${summary.exchangeCount} / 5회"
            binding.ufContentTv.text = String.format(Locale.KOREA, "%dg", summary.totalUf)

            val isMismatch = summary.hasUfMismatch

            val colorResId = if (isMismatch) R.color.red else R.color.secondary_text
            binding.ufContentTv.setTextColor(
                ContextCompat.getColor(requireContext(), colorResId)
            )
        }

        recordViewModel.recordlistItems.observe(viewLifecycleOwner) { itemList ->
            val container = binding.recentRecordContainer
            container.removeAllViews()

            if (itemList.isEmpty()) {
                container.visibility = View.GONE
                todayRecordId = null
                return@observe
            } else {
                container.visibility = View.VISIBLE
            }

            val inflater = LayoutInflater.from(requireContext())

            val normalColor = ContextCompat.getColor(requireContext(), R.color.main_text)
            val errorColor = ContextCompat.getColor(requireContext(), R.color.red)

            val today = LocalDate.now()
            todayRecordId = null

            itemList.forEach { item ->
                val itemBinding = ItemRecentRecordBinding.inflate(inflater, container, false)

                itemBinding.dateTv.text = item.recordDate.toString()

                itemBinding.weightTv.text = item.weight.let { weight ->
                    String.format(Locale.KOREA, "%.1fkg", weight)
                }

                itemBinding.totalUfTv.text = item.totalUf.let { uf ->
                    String.format(Locale.KOREA, "%dg", uf)
                }

                // 오늘 날짜 기록이면 todayRecordId에 저장
                if (item.recordDate == today) {
                    todayRecordId = item.id
                }

                // 제수량 검증 로직
                var sumUf = 0
                var hasPerExchangeMismatch = false

                item.exchanges.forEach { ex ->

                    if (ex.uf != null) {
                        sumUf += ex.uf
                    }

                    val calculatedUf =
                        if (ex.drainVolume != null && ex.fillVolume != null) ex.drainVolume - ex.fillVolume else null

                    if (ex.uf != null && calculatedUf != null && ex.uf != calculatedUf) {
                        hasPerExchangeMismatch = true
                    }
                }

                val hasTotalMismatch = (sumUf != item.totalUf)

                val isMismatch = hasPerExchangeMismatch || hasTotalMismatch

                // 하나라도 불일치하면 제수량 텍스트를 빨간색으로 표시
                itemBinding.totalUfTv.setTextColor(
                    if (isMismatch) errorColor else normalColor
                )

                val exchangeCount = item.exchanges.size
                itemBinding.completeTv.text = "${exchangeCount}회차 완료"

                itemBinding.root.setOnClickListener {
                    val args = Bundle().apply {
                        putLong("record_id", item.id)
                    }

                    val recordInfoFragment = RecordInfoFragment().apply {
                        arguments = args
                    }

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_frm, recordInfoFragment)
                        .addToBackStack(null)
                        .commit()
                }

                container.addView(itemBinding.root)
            }
        }

        statsViewModel.last7DaysAverage.observe(viewLifecycleOwner) { last7DaysResponse ->

            if (last7DaysResponse != null) {

                val data = last7DaysResponse.data
                val rangeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.KOREA)

                val startDateStr = last7DaysResponse.startDate.format(rangeFormatter)
                val endDateStr = last7DaysResponse.endDate.format(rangeFormatter)

                binding.weeklyAvgTv.text = String.format(
                    Locale.KOREA,
                    "최근 7일 평균 (%s ~ %s)",
                    startDateStr,
                    endDateStr
                )

                binding.weeklyWeightTv.text =
                    if (data.weightAvg != null) {
                        String.format(Locale.KOREA, "체중: %.1fkg", data.weightAvg)
                    } else {
                        "체중: -.-kg"
                    }

                binding.weeklyUfTv.text =
                    if (data.totalUfAvg != null) {
                        String.format(Locale.KOREA, "제수량 합계: %.0fg/일", data.totalUfAvg)
                    } else {
                        "제수량 합계: -g/일"
                    }

            } else {
                binding.weeklyAvgTv.text = "최근 7일 평균 (데이터 없음)"
                binding.weeklyWeightTv.text = "체중: -.-kg"
                binding.weeklyUfTv.text = "제수량 합계: -g/일"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                recordViewModel.errorEvent.collect { errorMsg ->
                    Log.e("HOME_FRAGMENT", "오늘의 요약 조회 실패: $errorMsg")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                statsViewModel.errorEvent.collect { errorMsg ->
                    Log.e("HOME_FRAGMENT", "최근 7일 평균 조회 실패: $errorMsg")
                }
            }
        }
    }

    private fun observeTodayTipResult() {
        educationViewModel.getTipResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                binding.tipBodyTv.text = it.body
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                educationViewModel.errorEvent.collect { errorMsg ->
                    Log.e("HomeFragment", "복막투석 관리 팁 조회 실패: $errorMsg")
                }
            }
        }
    }

    private fun navigateToTodayRecordDetail() {
        val recordId = todayRecordId
        if (recordId == null) {
            Toast.makeText(requireContext(), "오늘 작성된 기록이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val args = Bundle().apply {
            putLong("record_id", recordId)
        }

        val recordInfoFragment = RecordInfoFragment().apply {
            arguments = args
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frm, recordInfoFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}