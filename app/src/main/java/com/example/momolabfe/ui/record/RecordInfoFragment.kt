package com.example.momolabfe.ui.record

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordInfoBinding
import com.example.momolabfe.databinding.ItemExchangeInfoBinding
import com.example.momolabfe.remote.record.model.DayWeek
import com.example.momolabfe.remote.record.model.RecordGetResponse
import com.example.momolabfe.remote.record.model.Turbidity
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RecordInfoFragment : Fragment() {

    private var _binding: FragmentRecordInfoBinding? = null
    private val binding get() = _binding!!

    private var recordId: Long = -1L

    private val viewModel: RecordViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordInfoBinding.inflate(inflater, container, false)

        recordId = arguments?.getLong("record_id") ?: -1L
        Log.d("RECORD_INFO_FRAGMENT", "onCreateView recordId=$recordId")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        if (recordId != -1L) {
            viewModel.getRecord(recordId)
        }

        setupObservers()

        binding.editBtn.setOnClickListener {

            if (recordId == -1L) {
                binding.editBtn.isEnabled = true
                return@setOnClickListener
            }

            val fragment = RecordEditFragment().apply {
                arguments = Bundle().apply {
                    putLong("record_id", recordId)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.deleteBtn.setOnClickListener {
            if (recordId == -1L) {
                Log.e("RECORD_INFO_FRAGMENT", "삭제할 recordId가 없습니다.")
                return@setOnClickListener
            }

            if (parentFragmentManager.findFragmentByTag("RecordDeleteDialog") != null) {
                return@setOnClickListener
            }

            val dialog = DialogRecordDeleteFragment().apply {
                arguments = Bundle().apply {
                    putLong("record_id", recordId)
                }
            }
            dialog.show(parentFragmentManager, "RecordDeleteDialog")
        }
    }

    private fun setupObservers() {
        viewModel.record.observe(viewLifecycleOwner) { recordItem ->
            if (recordItem != null) {
                binding.contentTv.text = formatRecordDate(recordItem.recordDate, recordItem.recordDw)
                binding.weightValueTv.text = recordItem.weight.toString()
                binding.bloodPressureSystolicTv.text = recordItem.systolic.toString()
                binding.bloodPressureDiastolicTv.text = recordItem.diastolic.toString()
                binding.fastingGlucoseValueTv.text = recordItem.fastingGlucose.toString()
                binding.urineCountValueTv.text = recordItem.urineCount.toString()

                // 복막액 혼탁 표기
                binding.turbidityValueTv.text = when (recordItem.turbidity) {
                    Turbidity.NONE -> "없음"
                    Turbidity.PRESENT -> "있음"
                }

                binding.totalUfValueTv.text = recordItem.totalUf.toString()
                binding.noteValueTv.text = recordItem.notes

                // 회차별 정보 바인딩
                renderExchangeInfo(recordItem)

                applyUfValidation(recordItem)

                val imageUrl = recordItem.ocrImageUrl

                if (!imageUrl.isNullOrBlank()) {
                    binding.ocrImageTitleTv.visibility = View.VISIBLE
                    binding.ocrImageIv.visibility = View.VISIBLE

                    Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_thumbnail_sv)
                        .error(R.drawable.ic_thumbnail_sv)
                        .into(binding.ocrImageIv)

                    // 클릭 시 전체 화면 프리뷰
                    binding.ocrImageIv.setOnClickListener {
                        val tag = "ocr_image_preview"
                        if (parentFragmentManager.findFragmentByTag(tag) == null) {
                            val fragment = OcrImagePreviewFragment.newInstance(imageUrl)
                            parentFragmentManager.beginTransaction()
                                .add(R.id.main_frm, fragment, tag) // 전체 화면 위에 쌓기
                                .addToBackStack(null)
                                .commit()
                        }
                    }
                } else {
                    binding.ocrImageTitleTv.visibility = View.GONE
                    binding.ocrImageIv.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.deleteSuccess.collect {
                        Log.d("RECORD_INFO_FRAGMENT", "deleteSuccess 수신 - 기록 상세 화면 종료")

                        parentFragmentManager.setFragmentResult("record_delete", Bundle())
                        parentFragmentManager.popBackStack()
                    }
                }

                launch {
                    viewModel.errorEvent.collect { errorMsg ->
                        Log.e("RECORD_INFO_FRAGMENT", "기록 상세 작업 실패: $errorMsg")
                        Toast.makeText(requireContext(), "기록을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun formatRecordDate(date: LocalDate?, dw: DayWeek?): String {
        if (date == null) return ""

        val year = date.year
        val month = date.monthValue
        val day = date.dayOfMonth
        val dayKorean = convertDayWeekToKorean(dw)

        // 예: 2025년 11월 14일 금
        return "${year}년 ${month}월 ${day}일 $dayKorean"
    }

    private fun convertDayWeekToKorean(dw: DayWeek?): String {
        return when (dw) {
            DayWeek.MON -> "월"
            DayWeek.TUE -> "화"
            DayWeek.WED -> "수"
            DayWeek.THU -> "목"
            DayWeek.FRI -> "금"
            DayWeek.SAT -> "토"
            DayWeek.SUN -> "일"
            else -> ""
        }
    }

    // 제수량 검증 + 색상 적용
    private fun applyUfValidation(recordItem: RecordGetResponse) {
        val exchanges = recordItem.exchanges

        var sumUf = 0
        var hasPerExchangeMismatch = false

        for (item in exchanges) {
            val uf = item.uf
            sumUf += uf

            val calculatedUf = item.drainVolume - item.fillVolume
            if (item.uf != null && item.uf != calculatedUf) {
                hasPerExchangeMismatch = true
            }
        }

        val hasTotalMismatch = (sumUf != recordItem.totalUf)

        // 기본 / 에러 색상
        val normalColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        val errorColor = ContextCompat.getColor(requireContext(), R.color.red)

        // 둘 중 하나라도 불일치면 합계 텍스트를 빨간색으로
        if (hasPerExchangeMismatch || hasTotalMismatch) {
            binding.totalUfValueTv.setTextColor(errorColor)
        } else {
            binding.totalUfValueTv.setTextColor(normalColor)
        }
    }

    private fun renderExchangeInfo(recordItem: RecordGetResponse) {
        val container = binding.exchangeInfoContainer
        container.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

        val normalColor = ContextCompat.getColor(requireContext(), R.color.secondary_text)
        val errorColor = ContextCompat.getColor(requireContext(), R.color.red)

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        recordItem.exchanges.forEachIndexed { index, ex ->
            val itemBinding = ItemExchangeInfoBinding.inflate(inflater, container, false)

            itemBinding.exchangeTitleTv.text = "${index + 1}회차"

            itemBinding.exchangeTimeValueTv.text =
                ex.exchangeTime.format(timeFormatter) ?: "-"

            itemBinding.fillConcentrationValueTv.text =
                ex.fillConcentration.toString()

            itemBinding.drainVolumeValueTv.text = ex.drainVolume.toString()
            itemBinding.fillVolumeValueTv.text = ex.fillVolume.toString()

            val uf = ex.uf
            val drain = ex.drainVolume
            val fill = ex.fillVolume

            val calculatedUf =
                if (drain != null && fill != null) drain - fill else null

            itemBinding.ufValueTv.text = uf.toString()

            // 불일치 → 빨간색
            if (uf != null && calculatedUf != null && uf != calculatedUf) {
                itemBinding.ufValueTv.setTextColor(errorColor)
            } else {
                itemBinding.ufValueTv.setTextColor(normalColor)
            }

            container.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}