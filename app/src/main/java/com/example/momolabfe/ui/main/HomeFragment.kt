package com.example.momolabfe.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentHomeBinding
import com.example.momolabfe.databinding.ItemRecentRecordBinding
import com.example.momolabfe.ui.record.RecordFragment
import com.example.momolabfe.ui.record.RecordInfoFragment
import com.example.momolabfe.ui.record.RecordListFragment
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.example.momolabfe.ui.setting.SettingFragment
import com.example.momolabfe.ui.stats.StatsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecordViewModel by activityViewModels()

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

        setupObservers()
        viewModel.getRecentRecords()
        viewModel.getWeeklyAvgRecords()
        viewModel.getTodayExchangeSummary()

        try {
            // 날짜 포맷
            val now = Date()
            val format = SimpleDateFormat("yyyy년 MM월 dd일 EEEE", Locale.KOREA)

            val formattedDate = format.format(now)
            binding.contentTv.text = formattedDate

        } catch (e: Exception) {
            // 날짜 포맷팅 중 오류 발생 시 로그 출력
            e.printStackTrace()
        }

        binding.settingIv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, SettingFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.recordCv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, RecordFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.detailStatisticsBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, StatsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.totalTv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, RecordListFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupObservers() {
        viewModel.todayExchangeSummary.observe(viewLifecycleOwner) { summary ->
            if (summary == null || !summary.hasRecord) {
                // 오늘 기록이 없을 때 기본 표시
                binding.completeContentTv.text = "-회"
                binding.ufContentTv.text = "-g"
                return@observe
            }

            binding.completeContentTv.text = "${summary.exchangeCount} / 5회"
            binding.ufContentTv.text = String.format(Locale.KOREA, "%dg", summary.totalUf)
        }

        viewModel.recordlistItems.observe(viewLifecycleOwner) { itemList ->
            val container = binding.recentRecordContainer
            container.removeAllViews()

            if (itemList.isEmpty()) {
                container.visibility = View.GONE
                return@observe
            } else {
                container.visibility = View.VISIBLE
            }

            val inflater = LayoutInflater.from(requireContext())

            itemList.forEach { item ->
                val itemBinding = ItemRecentRecordBinding.inflate(inflater, container, false)

                itemBinding.dateTv.text = item.recordDate.toString()

                itemBinding.weightTv.text = item.weight.let { weight ->
                    String.format(Locale.KOREA, "%.1fkg", weight)
                }

                itemBinding.totalUfTv.text = item.totalUf.let { uf ->
                    String.format(Locale.KOREA, "%dg", uf)
                }

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

        viewModel.weeklyAverageData.observe(viewLifecycleOwner) { weeklyAvgResponse ->

            if (weeklyAvgResponse != null) {

                val data = weeklyAvgResponse.data

                val rangeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.KOREA)

                val startDateStr = weeklyAvgResponse.startDate.format(rangeFormatter)
                val endDateStr = weeklyAvgResponse.endDate.format(rangeFormatter)

                binding.weeklyAvgTv.text = String.format(
                    Locale.KOREA,
                    "이번 주 평균 (%s ~ %s)",
                    startDateStr,
                    endDateStr
                )

                binding.weeklyWeightTv.text =
                    String.format(
                        Locale.KOREA,
                        "체중: %.1fkg",
                        data.weightAvg
                    )

                binding.weeklyUfTv.text =
                    String.format(
                        Locale.KOREA,
                        "제수량 합계: %.0fg/일",
                        data.totalUfAvg
                    )
            } else {
                binding.weeklyAvgTv.text = "이번 주 평균 (데이터 없음)"
                binding.weeklyWeightTv.text = "체중: -.-kg"
                binding.weeklyUfTv.text = "제수량 합계: -g/일"
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            Log.e("HOME_FRAGMENT", errorMsg.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}