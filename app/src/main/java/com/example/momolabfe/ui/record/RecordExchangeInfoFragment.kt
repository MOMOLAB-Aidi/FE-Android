package com.example.momolabfe.ui.record

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.momolabfe.R
import com.example.momolabfe.data.remote.record.model.RecordExchangeCreateRequest
import com.example.momolabfe.databinding.FragmentRecordExchangeInfoBinding
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class RecordExchangeInfoFragment : Fragment() {

    private var _binding: FragmentRecordExchangeInfoBinding? = null
    private val binding get() = _binding!!

    private var recId: Long = -1L

    private val viewModel: RecordViewModel by activityViewModels()

    private val fmtHH_MM = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)
    private val fmtHH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.KOREA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recId = arguments?.getLong("rec_id") ?: -1L
    }

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

        setupUI()
        setupObservers()
    }

    private fun setupUI() {

        // 텍스트 변경 감지 → 버튼 상태 갱신
        val watcher: (Editable?) -> Unit = { updateNextButtonState() }
        binding.exchangeTimeEt.addTextChangedListener(afterTextChanged = watcher)
        binding.drainVolumeEt.addTextChangedListener(afterTextChanged = watcher)
        binding.fillVolumeEt.addTextChangedListener(afterTextChanged = watcher)
        binding.fillConcentrationEt.addTextChangedListener(afterTextChanged = watcher)
        binding.ufDataEt.addTextChangedListener(afterTextChanged = watcher)

        updateNextButtonState()

        binding.searchBtn.setOnClickListener {
            if (recId != -1L) {
                val request = createRecordExchangeRequest()
                viewModel.recordExchangeByWriting(recId, request)
            }
        }
    }

    private fun updateNextButtonState() {
        val exchangeTime = binding.exchangeTimeEt.text?.toString()?.trim().orEmpty()
        val drainVolume = binding.drainVolumeEt.text?.toString()?.trim().orEmpty()
        val fillVolume = binding.fillVolumeEt.text?.toString()?.trim().orEmpty()
        val fillConcentration = binding.fillConcentrationEt.text?.toString()?.trim().orEmpty()
        val uf = binding.ufDataEt.text?.toString()?.trim().orEmpty()

        val enabled = isValidExchangeTime(exchangeTime) &&
                isValidDrainVolume(drainVolume) &&
                isValidFillVolume(fillVolume) &&
                isValidFillConcentration(fillConcentration) &&
                isValidUf(uf)

        binding.searchBtn.isEnabled = enabled
        binding.searchBtn.setBackgroundColor(
            if (enabled) requireContext().getColor(R.color.main_2) else Color.parseColor("#F6F6F6")
        )
        binding.searchBtn.setTextColor(
            if (enabled) requireContext().getColor(R.color.white) else requireContext().getColor(R.color.text_primary)
        )
    }

    // 시간 파싱
    private fun parseExchangeTime(raw: String): LocalTime? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        return try {
            LocalTime.parse(s, fmtHH_MM) // 우선 HH:mm 시도
        } catch (_: DateTimeParseException) {
            try { LocalTime.parse(s, fmtHH_MM_SS) } // 안되면 HH:mm:ss
            catch (_: DateTimeParseException) { null }
        }
    }

    // 파싱 성공이면 유효
    private fun isValidExchangeTime(s: String): Boolean = parseExchangeTime(s) != null

    // 배액량: 0 ~ 6000
    private fun isValidDrainVolume(s: String): Boolean =
        s.toIntOrNull()?.let { it in 0..6000 } == true

    // 주입액 중량: 0 ~ 6000
    private fun isValidFillVolume(s: String): Boolean =
        s.toIntOrNull()?.let { it in 0..6000 } == true

    // 주입액 농도: 0 ~ 100
    private fun isValidFillConcentration(s: String): Boolean {
        val bd = s.toBigDecimalOrNull() ?: return false
        return bd >= BigDecimal("0") && bd <= BigDecimal("100.0")
    }

    // 제수량: -500 ~ 500
    private fun isValidUf(s: String): Boolean =
        s.toIntOrNull()?.let { it in -500..500 } == true


    private fun createRecordExchangeRequest(): RecordExchangeCreateRequest {
        val exchangeTimeInput = binding.exchangeTimeEt.text?.toString()?.trim().orEmpty()
        val time = parseExchangeTime(exchangeTimeInput)
            ?: error("교환시각 형식 오류 (예: 09:00 또는 09:00:00)")

        val drainVolume = binding.drainVolumeEt.text?.toString()?.trim()?.toIntOrNull()
            ?: error("배액량 파싱 에러")
        val fillVolume = binding.fillVolumeEt.text?.toString()?.trim()?.toIntOrNull()
            ?: error("주입액 중량 파싱 에러")
        val fillConcentration = binding.fillConcentrationEt.text?.toString()?.trim()?.toFloatOrNull()
            ?: error("주입액 농도 파싱 에러")
        val uf = binding.ufDataEt.text?.toString()?.trim()?.toIntOrNull()
            ?: error("제수량 파싱 에러")

        return RecordExchangeCreateRequest(
            exchangeTime = time,
            drainVolume = drainVolume,
            fillVolume = fillVolume,
            fillConcentration = fillConcentration,
            uf = uf
        )
    }

    private fun setupObservers() {

        // 성공 이벤트는 Flow 수집으로 1회성 처리
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recordSuccess.collect {
                    Log.d("RECORD_EXCHANGE_BY_WRITING_FRAGMENT", "회차별 정보 작성이 정상적으로 완료되었습니다.")

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_frm, RecordExchangeListFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            Log.e("RECORD_EXCHANGE_BY_WRITING_FRAGMENT", errorMsg.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}