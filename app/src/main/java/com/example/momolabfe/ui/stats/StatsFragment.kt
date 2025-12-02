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
import java.util.Locale
import kotlin.math.roundToInt

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
        binding.weightChart.setNoDataText(getString(R.string.no_weight_data))
        binding.ufChart.setNoDataText(getString(R.string.no_uf_data))

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
        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd", Locale.getDefault())

        val labels = points.map { p ->
            p.recordDate.format(dateFormatter)
        }

        val validWeights = points.mapNotNull { it.weight }
        if (validWeights.isNotEmpty()) {
            val avgWeight = validWeights.average().toFloat()
            binding.weeklyAvgWeightContentTv.text =
                String.format(Locale.getDefault(), "체중: %.1fkg", avgWeight)
        } else {
            binding.weeklyAvgWeightContentTv.text = "체중 기록 없음"
        }

        val validUfs = points.mapNotNull { it.totalUf }
        if (validUfs.isNotEmpty()) {
            val avgUf = validUfs.average()
            binding.weeklyAvgUfContentTv.text =
                String.format(Locale.getDefault(), "%.0fg/일", avgUf)
        } else {
            binding.weeklyAvgUfContentTv.text = "제수량 기록 없음"
        }

        val bp = stats.bpSummary
        binding.bpAvgValueTv.text = if (bp.avgSystolic != null && bp.avgDiastolic != null) {
            String.format(Locale.getDefault(), "%.0f/%.0f mmHg", bp.avgSystolic, bp.avgDiastolic)
        } else "기록 없음"

        binding.bpMaxValueTv.text = if (bp.maxSystolic != null && bp.maxDiastolic != null) {
            String.format(Locale.getDefault(), "%d/%d mmHg", bp.maxSystolic, bp.maxDiastolic)
        } else "기록 없음"

        val minSys = bp.minSystolic
        val minDia = bp.minDiastolic
        binding.bpMinValueTv.text = if (minSys != null && minDia != null) {
            String.format(Locale.getDefault(), "%d/%d mmHg", minSys, minDia)
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
            binding.weightChart.setNoDataText(getString(R.string.no_weight_data))
        }

        // 제수량 데이터가 1개 이상 있을 때만 그래프 세팅
        if (ufsForChart.any { it != null }) {
            setupUfChart(binding.ufChart, labels, ufsForChart)
        } else {
            binding.ufChart.clear()
            binding.ufChart.setNoDataText(getString(R.string.no_uf_data))
        }
    }

    private fun setupWeightChart(
        chart: LineChart,
        labels: List<String>,
        weights: List<Float?>
    ) {
        // 1. 값 있는 날만 Entry 생성
        val entries = mutableListOf<Entry>()
        val nonNullWeights = mutableListOf<Float>()

        weights.forEachIndexed { index, w ->
            if (w != null) {
                entries += Entry(index.toFloat(), w)
                nonNullWeights += w
            }
        }

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
                    return String.format(Locale.getDefault(), "%.1f", entry.y)
                }
            }
        }

        chart.data = LineData(dataSet)

        // 2. X축: 날짜 라벨
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            setDrawAxisLine(false)

            axisMinimum = -0.5f
            axisMaximum = labels.size - 0.5f
            setLabelCount(labels.size, /*force=*/false)

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    // 소수점 반올림 + 안전 범위 제한
                    val index = value.roundToInt()
                        .coerceIn(0, labels.lastIndex)

                    return labels[index]
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
        val nullIndexSet = mutableSetOf<Int>() // null이었던 인덱스 저장

        // 양수 or 음수 색상 다르게 적용
        ufs.forEachIndexed { index, uf ->

            val value = uf ?: 0f // Entry는 7개 유지 (null = 0)

            entries += BarEntry(index.toFloat(), value)

            val color = if (uf == null) {
                // null → 투명하게
                nullIndexSet += index
                ContextCompat.getColor(chart.context, R.color.transparent)
            } else if (uf >= 0f) {
                ContextCompat.getColor(chart.context, R.color.uf_positive_graph)
            } else {
                ContextCompat.getColor(chart.context, R.color.uf_negative_graph)
            }

            colors += color
        }

        val dataSet = BarDataSet(entries, "").apply {
            setDrawValues(true)
            setColors(colors)
            valueTextSize = 10f
            valueTextColor = ContextCompat.getColor(chart.context, R.color.text_primary)

            // null이었던 인덱스에는 라벨 출력 안 함
            valueFormatter = object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry?): String {
                    if (barEntry == null) return ""

                    val idx = barEntry.x.toInt()

                    // 원래 uf가 null이었던 날이면 라벨 숨김
                    if (nullIndexSet.contains(idx)) return ""

                    return String.format(Locale.getDefault(), "%.0f", barEntry.y)
                }
            }
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

            axisMinimum = -0.5f
            axisMaximum = labels.size - 0.5f
            setLabelCount(labels.size, /*force=*/false)

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    // 소수점 반올림 + 안전 범위 제한
                    val index = value.roundToInt()
                        .coerceIn(0, labels.lastIndex)
                    return labels[index]
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
        chart.notifyDataSetChanged() // 내부 버퍼 재계산
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