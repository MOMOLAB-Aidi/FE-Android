package com.example.momolabfe.ui.record

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.momolabfe.data.remote.record.model.DayWeek
import com.example.momolabfe.data.remote.record.model.RecordResponse
import com.example.momolabfe.data.remote.record.model.Turbidity
import com.example.momolabfe.databinding.FragmentRecordExchangeListBinding
import com.example.momolabfe.ui.record.adapter.ExchangeInfoAdapter
import com.example.momolabfe.ui.record.data.RecordExchangeInfo
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class RecordExchangeListFragment : Fragment() {

    private var _binding: FragmentRecordExchangeListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecordViewModel by viewModels()

    private lateinit var exchangeInfoAdapter: ExchangeInfoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.clearOcr()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordExchangeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupObservers()

        // 전달된 날짜 문자열 꺼내기 (기본값: 오늘 날짜 포맷 or 빈 문자열)
        val dateText = arguments?.getString("selected_date_text")
            ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
        val dwKorean = arguments?.getString("selected_date_dw")
            ?: weekdayShortKorean(LocalDate.now().dayOfWeek)

        // 넘겨준 이미지 Uri로 OCR 호출
        val imageUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("image_uri", Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("image_uri")
        }

        if (imageUri != null) {
            viewModel.getRecordByOcr(imageUri)
        }

    }

    private fun setupRecycler() {
        binding.exchangeRv.layoutManager = GridLayoutManager(requireContext(), 2)
        exchangeInfoAdapter = ExchangeInfoAdapter(mutableListOf())
        binding.exchangeRv.adapter = exchangeInfoAdapter
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

    private fun weekdayShortKorean(dow: DayOfWeek): String = when (dow) {
        DayOfWeek.SUNDAY -> "일"
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
    }

    private fun bindRecordToViews(record: RecordResponse) {
        binding.dateTv.text = record.recordDate.toKoreanDate()
        binding.dwTv.text = record.recordDw.korean()

        binding.weightDataEt.setText(safeNumber(record.weight))
        binding.systolicEt.setText(record.systolic.toString())
        binding.diastolicEt.setText(record.diastolic.toString())
        binding.fastingGlucoseEt.setText(record.fastingGlucose.toString())
        binding.urineCountEt.setText(record.urineCount.toString())
        binding.totalUfEt.setText(record.totalUf.toString())

        when (record.turbidity) {
            Turbidity.NONE -> {
                binding.turbidityNCheckbox.isChecked = true
                binding.turbidityYCheckbox.isChecked = false
            }
            Turbidity.PRESENT -> {
                binding.turbidityNCheckbox.isChecked = false
                binding.turbidityYCheckbox.isChecked = true
            }
        }

        binding.notesEt.setText(record.notes.orEmpty())

        val mapped = record.exchanges.map {
            RecordExchangeInfo(
                exchangeNo = it.exchangeNo,
                exchangeTimeText = it.exchangeTime.toKoreanTime()
            )
        }
        exchangeInfoAdapter.setItems(mapped)
    }

    private fun LocalDate.toKoreanDate(): String {
        val f = DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREA)
        return this.format(f)
    }

    private fun LocalTime.toKoreanTime(): String {
        val f = DateTimeFormatter.ofPattern("a h시 mm분", Locale.KOREA)
        return this.format(f)
    }

    private fun safeNumber(d: Double?): String = when {
        d == null -> ""
        d % 1.0 == 0.0 -> d.toInt().toString()
        else -> d.toString()
    }

    private fun setupObservers() {
        // OCR 결과 관찰 → UI 바인딩
        viewModel.ocrResult.observe(viewLifecycleOwner) { record ->
            if (record == null) {
                // 초기 상태: 빈 값/체크 해제/리스트 비우기
                binding.weightDataEt.setText("")
                binding.systolicEt.setText("")
                binding.diastolicEt.setText("")
                binding.fastingGlucoseEt.setText("")
                binding.urineCountEt.setText("")
                binding.totalUfEt.setText("")
                binding.notesEt.setText("")
                binding.turbidityNCheckbox.isChecked = false
                binding.turbidityYCheckbox.isChecked = false
                setupRecycler()
                return@observe
            }
            bindRecordToViews(record)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let { Log.d("RECORD_BY_OCR", it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearOcr()
        _binding = null
    }
}