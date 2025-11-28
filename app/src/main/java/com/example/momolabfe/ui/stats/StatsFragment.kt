package com.example.momolabfe.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentStatsBinding
import com.example.momolabfe.ui.stats.renderer.RoundedBarChartRenderer
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

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: 나중에 API 응답으로 교체
        val labels = listOf("11/08", "11/09", "11/10", "11/11", "11/12", "11/13", "11/14")
        val weights = listOf(58.7f, 58.3f, 57.9f, 57.6f, 57.1f, 56.8f, 56.2f)
        val ufs = listOf(-420f, 560f, 380f, 470f, 610f, 390f, 520f)

        setupWeightChart(binding.weightChart, labels, weights)
        setupUfChart(binding.ufChart, labels, ufs)
    }

    private fun setupWeightChart(
        chart: LineChart,
        labels: List<String>,
        weights: List<Float>
    ) {
        // 1. Entry 만들기
        val entries = weights.mapIndexed { index, w ->
            Entry(index.toFloat(), w)
        }

        val dataSet = LineDataSet(entries, "").apply {
            setDrawCircles(true)
            circleRadius = 4f
            setDrawCircleHole(false)
            lineWidth = 2f
            setDrawValues(false) // 점 위 숫자 숨김
            mode = LineDataSet.Mode.LINEAR // 직선 형태
            color = ContextCompat.getColor(chart.context, R.color.weight_graph)
            setCircleColor(color)
        }

        chart.data = LineData(dataSet)

        // 2. X축: 날짜 라벨
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return labels.getOrNull(index) ?: ""
            }
        }

        // 3. Y축: 체중 데이터
        chart.axisRight.isEnabled = false
        chart.axisLeft.apply {
            setDrawGridLines(true)
            val min = (weights.minOrNull() ?: 0f) - 1f
            val max = (weights.maxOrNull() ?: 0f) + 1f
            axisMinimum = min
            axisMaximum = max
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
        ufs: List<Float>
    ) {

        // 1. Entry 만들기
        val entries = mutableListOf<BarEntry>()
        val colors = mutableListOf<Int>()

        // 양수 or 음수 색상 다르게 적용
        ufs.forEachIndexed { index, uf ->
            entries += BarEntry(index.toFloat(), uf)

            val c = if (uf >= 0f) {
                ContextCompat.getColor(chart.context, R.color.uf_positive_graph)
            } else {
                ContextCompat.getColor(chart.context, R.color.uf_negative_graph)
            }
            colors += c
        }

        val dataSet = BarDataSet(entries, "").apply {
            setDrawValues(false)
            setColors(colors)
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
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return labels.getOrNull(index) ?: ""
                }
            }
        }

        // 3. Y축: 일별 제수량(0을 기준으로 위/아래 대칭)
        val minUf = ufs.minOrNull() ?: 0f
        val maxUf = ufs.maxOrNull() ?: 0f
        var maxAbs = maxOf(kotlin.math.abs(minUf), kotlin.math.abs(maxUf))

        if (maxAbs < 500f) maxAbs = 500f // 너무 좁으면 보기 어려우니 최소 범위 보정
        if (maxAbs > 5000f) maxAbs = 5000f // 안전상 캡

        chart.axisRight.isEnabled = false
        chart.axisLeft.apply {
            setDrawGridLines(true)
            axisMinimum = -maxAbs
            axisMaximum = maxAbs
        }

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(false)

        // 모서리 둥글게
        chart.renderer = RoundedBarChartRenderer(
            chart,
            chart.animator,
            chart.viewPortHandler
        )

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