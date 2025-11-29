package com.example.momolabfe.ui.record

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.momolabfe.R
import com.example.momolabfe.remote.record.model.OcrRecordExchangeData
import com.example.momolabfe.databinding.DialogTimePickerBinding
import com.example.momolabfe.databinding.FragmentRecordWrite02Binding
import com.example.momolabfe.remote.record.model.RecordCreateRequest
import com.example.momolabfe.remote.record.model.RecordExchangeCreateRequest
import com.example.momolabfe.ui.record.adapter.RecordExchangeAdapter
import com.example.momolabfe.ui.record.data.RecordCommonDraft
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@AndroidEntryPoint
class RecordWrite02Fragment : Fragment(), RecordExchangeAdapter.OnTimePickerClickListener {

    private var _binding: FragmentRecordWrite02Binding? = null
    private val binding get() = _binding!!

    private val viewModel: RecordViewModel by activityViewModels()
    private lateinit var exchangeAdapter: RecordExchangeAdapter

    private val MAX_EXCHANGES = RecordExchangeAdapter.MAX_EXCHANGES

    private val isFromOcr: Boolean by lazy {
        arguments?.getBoolean("fromOcr", false) ?: false
    }

    // write01에서 넘긴 공통 정보 받기
    private val commonDraft: RecordCommonDraft? by lazy {
        val args = arguments ?: return@lazy null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            args.getParcelable("record_common", RecordCommonDraft::class.java)
        } else {
            @Suppress("DEPRECATION")
            args.getParcelable("record_common")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordWrite02Binding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        setupRecyclerView()
        fillExchangesFromOcr()

        if (isFromOcr) {
            downloadOcrImageIfAvailable()
        } else {
            binding.ocrImagePreview.visibility = View.GONE
        }

        binding.addExchangeBtn.setOnClickListener {
            addExchange()
        }

        binding.saveBtn.setOnClickListener {
            binding.saveBtn.isEnabled = false
            collectDataAndCallApi()
        }

        setupObservers()
    }

    // 어댑터에서 호출되는 콜백 함수 구현
    override fun onTimePickerClick(position: Int, targetEditText: EditText) {
        showTimePickerDialog(targetEditText, position)
    }

    private fun showTimePickerDialog(targetEditText: EditText, position: Int) {
        val dialogBinding = DialogTimePickerBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
            .setView(dialogBinding.root)
            .create()

        setupPickersInDialog(dialogBinding)

        dialogBinding.applyTv.setOnClickListener {
            applySelectedTime(dialogBinding, targetEditText, position)
            dialog.dismiss()
        }

        dialogBinding.cancelTv.setOnClickListener {
            dialog.dismiss()
        }

        applyDialogWindow(dialog)
        dialog.show()
    }

    private fun setupPickersInDialog(dialogBinding: DialogTimePickerBinding) {
        dialogBinding.ampmPickerNp.apply {
            minValue = 0
            maxValue = 1
            displayedValues = arrayOf("오전", "오후")
            wrapSelectorWheel = false
            post { applyTextStyleToNumberPicker(this, requireContext()) }
        }

        dialogBinding.hourPickerNp.apply {
            minValue = 1
            maxValue = 12
            wrapSelectorWheel = true
            value = 12
            post { applyTextStyleToNumberPicker(this, requireContext()) }
        }

        dialogBinding.minutePickerNp.apply {
            minValue = 0
            maxValue = 5
            displayedValues = arrayOf("00", "10", "20", "30", "40", "50")
            wrapSelectorWheel = true
            post { applyTextStyleToNumberPicker(this, requireContext()) }
        }
    }

    private fun applySelectedTime(dialogBinding: DialogTimePickerBinding, targetEditText: EditText, position: Int) {
        val ampmPicker = dialogBinding.ampmPickerNp
        val hourPicker = dialogBinding.hourPickerNp
        val minutePicker = dialogBinding.minutePickerNp

        val ampm = ampmPicker.value
        val hour12 = hourPicker.value
        val minute = arrayOf("00", "10", "20", "30", "40", "50")[minutePicker.value]

        // 서버 전송용 24시간 형식으로 변환
        val hour24 = convertTo24Hour(ampm, hour12)
        val serverTimeText = String.format(Locale.KOREA, "%02d:%s", hour24, minute)

        val currentList = exchangeAdapter.items
        if (position >= 0 && position < currentList.size) {
            val updatedItem = currentList[position].copy(
                // 24시간 형식 문자열을 LocalTime 객체로 파싱
                exchangeTime = LocalTime.parse(serverTimeText, RecordExchangeAdapter.TIME_FORMATTER)
            )
            currentList[position] = updatedItem
            exchangeAdapter.notifyItemChanged(position)
        }

        // ViewModel에 24시간 형식 저장 (서버 전송용)
        viewModel.setExchangeTime(serverTimeText)
    }

    private fun convertTo24Hour(ampm: Int, hour12: Int): Int {
        return when {
            ampm == 0 && hour12 == 12 -> 0  // 오전 12시 -> 00시
            ampm == 0 -> hour12              // 오전 1~11시 -> 1~11시
            ampm == 1 && hour12 == 12 -> 12 // 오후 12시 -> 12시
            else -> hour12 + 12              // 오후 1~11시 -> 13~23시
        }
    }

    private fun applyDialogWindow(
        dialog: AlertDialog,
        transparentBackground: Boolean = false
    ) {
        if (transparentBackground) {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        dialog.setOnShowListener {
            dialog.window?.let { window ->
                val layoutParams = window.attributes
                layoutParams.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                layoutParams.gravity = Gravity.CENTER
                layoutParams.dimAmount = 0.8f
                window.attributes = layoutParams
                window.setDimAmount(0.8f)
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }

            // 버튼 텍스트 색상
            val positiveColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            val negativeColor = ContextCompat.getColor(requireContext(), R.color.secondary_text)

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(positiveColor)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(negativeColor)
        }
    }

    private fun applyTextStyleToNumberPicker(picker: NumberPicker, context: Context) {
        try {
            val count = picker.childCount
            for (i in 0 until count) {
                val child = picker.getChildAt(i)
                if (child is EditText) {
                    child.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    child.textSize = 15f
                    child.typeface = ResourcesCompat.getFont(context, R.font.noto_sans_kr_regular)
                    child.includeFontPadding = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        exchangeAdapter = RecordExchangeAdapter().apply {
            onTimePickerClickListener = this@RecordWrite02Fragment
        }
        binding.exchangeRv.apply {
            adapter = exchangeAdapter
            setHasFixedSize(false)
            setItemViewCacheSize(MAX_EXCHANGES)
        }
    }

    // 회차 추가 버튼 클릭 시 새로운 교환 항목 추가 + 업데이트
    private fun addExchange() {
        val currentExchanges = exchangeAdapter.items

        if (currentExchanges.size >= MAX_EXCHANGES) {
            return
        }

        // 새 항목의 exchangeNo 계산
        val newExchangeNo = currentExchanges.size + 1

        val newExchange = OcrRecordExchangeData(
            id = -1, // 임시 ID 할당
            exchangeNo = newExchangeNo,
            exchangeTime = LocalTime.now(),
            drainVolume = 0,
            fillConcentration = 0.0,
            fillVolume = 0,
            uf = 0
        )

        exchangeAdapter.addExchangeItem(newExchange)

        updateAddButtonVisibility()
        binding.exchangeRv.requestLayout()

        // 추가된 항목 위치로 스크롤
        binding.exchangeRv.scrollToPosition(exchangeAdapter.itemCount - 1)
    }

    private fun fillExchangesFromOcr() {
        val ocr = viewModel.ocrRecordResult.value

        // OCR 결과가 null이거나 exchanges 리스트가 비어있는지 확인
        val exchanges = ocr?.ocrData?.exchanges.orEmpty().toMutableList()

        if (exchanges.isEmpty()) {
            // 1회차 항목을 생성하여 추가
            val firstExchange = OcrRecordExchangeData(
                id = -1, // 임시 ID
                exchangeNo = 1,
                exchangeTime = LocalTime.now(),
                drainVolume = 0,
                fillConcentration = 0.0,
                fillVolume = 0,
                uf = 0
            )
            exchanges.add(firstExchange)
        }

        exchangeAdapter.items = exchanges
        if (exchanges.size == 1) {
            exchangeAdapter.notifyItemInserted(0)
        } else {
            exchangeAdapter.notifyItemRangeInserted(0, exchanges.size)
        }
        updateAddButtonVisibility()
        binding.exchangeRv.requestLayout() // 레이아웃 다시 계산하도록 요청

        if (isFromOcr) {
            val totalUfFromOcr = ocr?.ocrData?.totalUf

            if (totalUfFromOcr != null) {
                binding.totalUfEt.setText(totalUfFromOcr.toString())
            }
        }
    }

    private fun updateAddButtonVisibility() {
        // 현재 어댑터의 항목 개수를 확인
        if (exchangeAdapter.itemCount >= MAX_EXCHANGES) {
            binding.addExchangeBtn.visibility = View.GONE
        } else {
            binding.addExchangeBtn.visibility = View.VISIBLE
        }
    }

    private fun collectDataAndCallApi() {
        fun enableButtonAndReturn() {
            binding.saveBtn.isEnabled = true
        }

        val draft = commonDraft
        if (draft == null) {
            Toast.makeText(requireContext(), "공통 정보가 없습니다. 처음 화면부터 다시 작성해주세요.", Toast.LENGTH_SHORT).show()
            enableButtonAndReturn()
            return
        }

        val totalUfText = binding.totalUfEt.text.toString()
        val totalUf = totalUfText.toIntOrNull()
        if (totalUf == null) {
            Toast.makeText(requireContext(), "제수량 합계를 입력해주세요.", Toast.LENGTH_SHORT).show()
            enableButtonAndReturn()
            return
        }

        if (totalUf < -5000 || totalUf > 5000) {
            Toast.makeText(
                requireContext(),
                "제수량 합계는 -5000 ~ 5000 g 사이로 입력해주세요. (현재: $totalUf)",
                Toast.LENGTH_SHORT
            ).show()
            enableButtonAndReturn()
            return
        }

        val exchanges = exchangeAdapter.items

        // 각 회차 데이터 유효성 검사 및 DTO 리스트 생성
        val requestList = mutableListOf<RecordExchangeCreateRequest>()
        for (item in exchanges) {

            if (item.drainVolume <= 0) {
                Toast.makeText(
                    requireContext(),
                    "${item.exchangeNo}회차 배액량을 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                enableButtonAndReturn()
                return
            }

            if (item.fillVolume <= 0) {
                Toast.makeText(
                    requireContext(),
                    "${item.exchangeNo}회차 주입량을 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                enableButtonAndReturn()
                return
            }

            if (item.fillConcentration <= 0.0) {
                Toast.makeText(
                    requireContext(),
                    "${item.exchangeNo}회차 주입액 농도를 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                enableButtonAndReturn()
                return
            }

            if (item.uf == null) {
                Toast.makeText(
                    requireContext(),
                    "${item.exchangeNo}회차 제수량을 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                enableButtonAndReturn()
                return
            }

            if (item.drainVolume < 0 || item.drainVolume > 6000) {
                Toast.makeText(
                    requireContext(),
                    "${item.exchangeNo}회차 배액량은 0 ~ 6000 g 사이로 입력해주세요. (현재: ${item.drainVolume})",
                    Toast.LENGTH_SHORT
                ).show()
                enableButtonAndReturn()
                return
            }

            if (item.fillVolume < 0 || item.fillVolume > 6000) {
                Toast.makeText(
                    requireContext(),
                    "${item.exchangeNo}회차 주입량은 0 ~ 6000 g 사이로 입력해주세요. (현재: ${item.fillVolume})",
                    Toast.LENGTH_SHORT
                ).show()
                enableButtonAndReturn()
                return
            }

            if (item.fillConcentration < 0.0 || item.fillConcentration > 100.0) {
                Toast.makeText(
                    requireContext(),
                    "${item.exchangeNo}회차 주입액 농도는 0.0 ~ 100.0 % 사이로 입력해주세요. (현재: ${item.fillConcentration})",
                    Toast.LENGTH_SHORT
                ).show()
                enableButtonAndReturn()
                return
            }

            if (item.uf < -500 || item.uf > 500) {
                Toast.makeText(
                    requireContext(),
                    "${item.exchangeNo}회차 제수량은 -500 ~ 500 g 사이로 입력해주세요. (현재: ${item.uf})",
                    Toast.LENGTH_SHORT
                ).show()
                enableButtonAndReturn()
                return
            }

            requestList += RecordExchangeCreateRequest(
                exchangeNo = item.exchangeNo,
                exchangeTime = item.exchangeTime.format(RecordExchangeAdapter.TIME_FORMATTER),
                drainVolume = item.drainVolume,
                fillVolume = item.fillVolume,
                fillConcentration = item.fillConcentration,
                uf = item.uf
            )
        }

        val recordDate = runCatching { LocalDate.parse(draft.recordDate) }.getOrElse {
            Toast.makeText(requireContext(), "날짜 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            enableButtonAndReturn()
            return
        }

        // 제수량 검증
        validateUfAndShowDialogIfNeeded(
            totalUf = totalUf,
            exchanges = exchanges,
            onProceed = {
                // 팝업에서 [예] 선택하거나, 불일치 없을 때 호출
                val fullRequest = RecordCreateRequest(
                    recordDate = recordDate,
                    recordDw = draft.recordDw,
                    weight = draft.weight,
                    systolic = draft.systolic,
                    diastolic = draft.diastolic,
                    fastingGlucose = draft.fastingGlucose,
                    urineCount = draft.urineCount,
                    turbidity = draft.turbidity,
                    notes = draft.notes,
                    totalUf = totalUf,
                    gcsPath = draft.gcsPath,
                    exchanges = requestList
                )
                viewModel.createRecord(fullRequest)
            },
            onCancel = {
                // 팝업에서 [아니오] 눌렀을 때 → 저장 취소
                enableButtonAndReturn()
            }
        )
    }

    private fun downloadOcrImageIfAvailable() {
        val ocr = viewModel.ocrRecordResult.value

        val gcsPath = ocr?.gcsPath
        if (!gcsPath.isNullOrEmpty()) {
            // gcs path를 사용하여 이미지 다운로드 시작
            viewModel.downloadOcrImage(gcsPath)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recordSuccess.collectLatest {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_frm, RecordListFragment())
                        .commit()
                }
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            Log.e("RECORD_WRITE_02_FRAGMENT", errorMsg.toString())
            binding.saveBtn.isEnabled = true
        }
    }

    // 제수량 검증 후 일치하지 않으면 경고 팝업창 표시
    private fun validateUfAndShowDialogIfNeeded(
        totalUf: Int,
        exchanges: List<OcrRecordExchangeData>,
        onProceed: () -> Unit,
        onCancel: () -> Unit
    ) {
        var hasPerExchangeMismatch = false
        var hasTotalMismatch = false

        var sumUf = 0

        exchanges.forEach { item ->
            val uf = item.uf
            sumUf += uf

            val calculatedUf = item.drainVolume - item.fillVolume
            if (uf != calculatedUf) {
                hasPerExchangeMismatch = true
            }
        }

        if (sumUf != totalUf) {
            hasTotalMismatch = true
        }

        // 둘 다 없으면 바로 저장 진행
        if (!hasPerExchangeMismatch && !hasTotalMismatch) {
            onProceed()
            return
        }

        // 경고 메시지 구성
        val messageBuilder = StringBuilder().apply {
            append("입력한 제수량과 계산된 제수량이 다릅니다.\n\n")
            if (hasPerExchangeMismatch) {
                append("- 일부 회차: (배액량 - 주입량) 값이 회차별 제수량과 다릅니다.\n")
            }
            if (hasTotalMismatch) {
                append("- 제수량 합계: 회차별 제수량을 모두 더한 값이 제수량 합계와 다릅니다.\n")
            }
            append("\n그래도 이대로 저장하시겠습니까?")
        }

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
            .setTitle("제수량 확인")
            .setMessage(messageBuilder.toString())
            .setNegativeButton("아니오") { d, _ ->
                d.dismiss()
                onCancel()
            }
            .setPositiveButton("예") { d, _ ->
                d.dismiss()
                onProceed()
            }
            .setCancelable(false)
            .create()

        applyDialogWindow(dialog)

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearOcr()
        _binding = null
    }
}