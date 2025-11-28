package com.example.momolabfe.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentStatsBinding
import com.example.momolabfe.remote.stats.model.Last7DaysStats
import com.example.momolabfe.ui.stats.renderer.RoundedBarChartRenderer
import com.example.momolabfe.ui.stats.viewModel.StatsViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.time.format.DateTimeFormatter

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 기본 no-data 문구 세팅
        binding.weightChart.setNoDataText("체중 기록이 없습니다.")
        binding.ufChart.setNoDataText("제수량 기록이 없습니다.")

        setupObservers()
        viewModel.getLast7Days()
    }

    private fun setupObservers() {
        viewModel.getStatsResult.observe(viewLifecycleOwner) { stats ->
            bindStats(stats)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 통계 응답을 UI에 바인딩
    private fun bindStats(stats: Last7DaysStats) {
        val points = stats.points
        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")

        val labels = points.map { p ->
            p.recordDate.format(dateFormatter)
        }

        val validWeights = points.mapNotNull { it.weight }
        if (validWeights.isNotEmpty()) {
            val avgWeight = validWeights.average().toFloat()
            binding.weeklyAvgWeightContentTv.text =
                String.format("체중: %.1fkg", avgWeight)
        } else {
            binding.weeklyAvgWeightContentTv.text = "체중 기록 없음"
        }

        val validUfs = points.mapNotNull { it.totalUf }
        if (validUfs.isNotEmpty()) {
            val avgUf = validUfs.average()
            binding.weeklyAvgUfContentTv.text =
                String.format("%.0fg/일", avgUf)
        } else {
            binding.weeklyAvgUfContentTv.text = "제수량 기록 없음"
        }

        val bp = stats.bpSummary
        binding.bpAvgValueTv.text = if (bp.avgSystolic != null && bp.avgDiastolic != null) {
            String.format("%.0f/%.0f mmHg", bp.avgSystolic, bp.avgDiastolic)
        } else "기록 없음"

        binding.bpMaxValueTv.text = if (bp.maxSystolic != null && bp.maxDiastolic != null) {
            "${bp.maxSystolic}/${bp.maxDiastolic} mmHg"
        } else "기록 없음"

        binding.bpMinValueTv.text = if (bp.minSystolic != null && bp.minDiastolic != null) {
            "${bp.minSystolic}/${bp.minDiastolic} mmHg"
        } else "기록 없음"

        val minSys = bp.minSystolic
        val minDia = bp.minDiastolic
        binding.bpMinValueTv.text = if (minSys != null && minDia != null) {
            "$minSys/$minDia mmHg"
        } else {
            "기록 없음"
        }

        val weightsForChart: List<Float?> = points.map { it.weight }
        val ufsForChart: List<Float?> = points.map { it.totalUf?.toFloat() }

        // 체중 데이터가 1개 이상 있을 때만 그래프 세팅
        if (weightsForChart.any { it != null }) {
            setupWeightChart(binding.weightChart, labels, weightsForChart)
        } else {
            binding.weightChart.clear()
            binding.weightChart.setNoDataText("체중 기록이 없습니다.")
        }

        // 제수량은 날짜 기준으로 항상 7개 막대를 그리고, 값이 없으면 0으로 처리
        if (ufsForChart.any { it != null }) {
            setupUfChart(binding.ufChart, labels, ufsForChart)
        } else {
            binding.ufChart.clear()
            binding.ufChart.setNoDataText("제수량 기록이 없습니다.")
        }
    }

    private fun setupWeightChart(
        chart: LineChart,
        labels: List<String>,
        weights: List<Float?>
    ) {
        // 1. Entry 만들기
        val entries = mutableListOf<Entry>()
        weights.forEachIndexed { index, w ->
            if (w != null) {
                entries += Entry(index.toFloat(), w)
            }
        }

        val yValues = entries.map { it.y }

        val dataSet = LineDataSet(entries, "").apply {
            setDrawCircles(true)
            circleRadius = 4f
            setDrawCircleHole(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.LINEAR // 직선 형태
            color = ContextCompat.getColor(chart.context, R.color.weight_graph)
            setCircleColor(color)

            // 값 표시 설정
            setDrawValues(true)
            valueTextSize = 10f
            valueTextColor = ContextCompat.getColor(chart.context, R.color.text_primary)

            // 소수 1자리까지 표시
            valueFormatter = object : ValueFormatter() {
                override fun getPointLabel(entry: Entry?): String {
                    if (entry == null) return ""
                    return String.format("%.1f", entry.y)
                }
            }
        }

        chart.data = LineData(dataSet)

        // 2. X축: 날짜 라벨
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)

            // labels 개수에 맞춰 0 ~ size-1 범위로 강제
            axisMinimum = -0.5f
            axisMaximum = labels.size - 0.5f
            setLabelCount(labels.size, /*force=*/true)

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return labels.getOrNull(index) ?: ""
                }
            }
        }

        // 3. Y축: 체중 데이터
        chart.axisRight.isEnabled = false
        chart.axisLeft.apply {
            setDrawGridLines(true)

            // Y축 자동 스케일링
            resetAxisMinimum()
            resetAxisMaximum()
        }

        // 4. 기타 옵션
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(false) // 통계 화면에서는 줌/드래그 필요 없으면 false

        chart.invalidate()
    }

    private fun setupUfChart(
        chart: BarChart,
        labels: List<String>,
        ufs: List<Float?>
    ) {

        // 1. Entry 만들기
        val entries = mutableListOf<BarEntry>()
        val colors = mutableListOf<Int>()
        val nonNullUfs = mutableListOf<Float>() // min/max 계산용

        // 양수 or 음수 색상 다르게 적용
        ufs.forEachIndexed { index, uf ->
            if (uf != null) {
                entries += BarEntry(index.toFloat(), uf)
                nonNullUfs += uf

                val c = if (uf >= 0f) {
                    ContextCompat.getColor(chart.context, R.color.uf_positive_graph)
                } else {
                    ContextCompat.getColor(chart.context, R.color.uf_negative_graph)
                }
                colors += c
            }
        }

        // 표시할 값이 하나도 없으면 no-data 처리
        if (entries.isEmpty()) {
            chart.clear()
            chart.setNoDataText("제수량 기록이 없습니다.")
            return
        }

        val dataSet = BarDataSet(entries, "").apply {
            setDrawValues(true)
            setColors(colors)
            valueTextSize = 10f
            valueTextColor = ContextCompat.getColor(chart.context, R.color.text_primary)
        }

        chart.data = BarData(dataSet).apply {
            barWidth = 0.6f
        }

        // 2. X축: 날짜 라벨
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            setDrawAxisLine(false)

            // labels 길이에 맞춰 0 ~ size-1 까지 라벨을 강제로 모두 찍도록
            axisMinimum = -0.5f
            axisMaximum = labels.size - 0.5f
            setLabelCount(labels.size, /*force=*/true)

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return labels.getOrNull(index) ?: ""
                }
            }
        }

        // 3. Y축: 자동 스케일링
        chart.axisRight.isEnabled = false
        chart.axisLeft.apply {
            setDrawGridLines(true)

            // Y축 자동
            resetAxisMinimum()
            resetAxisMaximum()
        }

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(false)

        // 모서리 둥글게
        if (chart.renderer !is RoundedBarChartRenderer) {
            chart.renderer = RoundedBarChartRenderer(
                chart,
                chart.animator,
                chart.viewPortHandler
            )
        }
        chart.notifyDataSetChanged()

        chart.invalidate()
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.main_bnv)
        bottomNav?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}