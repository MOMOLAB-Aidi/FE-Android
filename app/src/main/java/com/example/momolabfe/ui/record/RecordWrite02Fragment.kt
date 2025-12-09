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
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.momolabfe.R
import com.example.momolabfe.remote.record.model.OcrRecordExchangeData
import com.example.momolabfe.databinding.DialogTimePickerBinding
import com.example.momolabfe.databinding.FragmentRecordWrite02Binding
import com.example.momolabfe.databinding.ItemRecordExchangeBinding
import com.example.momolabfe.remote.record.model.RecordCreateRequest
import com.example.momolabfe.remote.record.model.RecordExchangeCreateRequest
import com.example.momolabfe.ui.main.HomeFragment
import com.example.momolabfe.ui.record.data.RecordCommonDraft
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class RecordWrite02Fragment : Fragment() {

    private var _binding: FragmentRecordWrite02Binding? = null
    private val binding get() = _binding!!

    private val viewModel: RecordViewModel by activityViewModels()

    // 회차 리스트 직접 관리
    private val exchangeList = mutableListOf<OcrRecordExchangeData>()

    // 각 회차별 드롭다운 펼침/접힘 상태
    private val expandedStates = mutableListOf<Boolean>()

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

        fillExchangesFromOcr()

        binding.addExchangeBtn.setOnClickListener {
            addExchange()
        }

        binding.saveBtn.setOnClickListener {
            binding.saveBtn.isEnabled = false
            collectDataAndCallApi()
        }

        setupObservers()
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

    private fun applySelectedTime(
        dialogBinding: DialogTimePickerBinding,
        targetEditText: EditText,
        position: Int
    ) {
        val ampmPicker = dialogBinding.ampmPickerNp
        val hourPicker = dialogBinding.hourPickerNp
        val minutePicker = dialogBinding.minutePickerNp

        val ampm = ampmPicker.value
        val hour12 = hourPicker.value
        val minute = arrayOf("00", "10", "20", "30", "40", "50")[minutePicker.value]

        // 서버 전송용 24시간 형식으로 변환
        val hour24 = convertTo24Hour(ampm, hour12)
        val serverTimeText = String.format(Locale.KOREA, "%02d:%s", hour24, minute)

        // 리스트 데이터 업데이트
        if (position >= 0 && position < exchangeList.size) {
            val updatedItem = exchangeList[position].copy(
                exchangeTime = LocalTime.parse(serverTimeText, TIME_FORMATTER)
            )
            exchangeList[position] = updatedItem
        }

        // 화면에 표시
        targetEditText.setText(serverTimeText)

        // ViewModel에 24시간 형식 저장 (서버 전송용 – 기존 로직 유지)
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

    // 회차 추가 버튼 클릭 시 새로운 교환 항목 추가 + 업데이트
    private fun addExchange() {
        if (exchangeList.size >= MAX_EXCHANGES) {
            return
        }

        val newExchangeNo = exchangeList.size + 1

        val newExchange = OcrRecordExchangeData(
            id = null,
            exchangeNo = newExchangeNo,
            exchangeTime = null,
            drainVolume = null,
            fillConcentration = null,
            fillVolume = null,
            uf = null
        )

        exchangeList.add(newExchange)
        expandedStates.add(true)

        renderExchangeViews()
        updateAddButtonVisibility()
    }

    private fun fillExchangesFromOcr() {
        val ocr = viewModel.ocrRecordResult.value

        // OCR 결과가 null이거나 exchanges 리스트가 비어있는지 확인
        val exchangesFromOcr = ocr?.ocrData?.exchanges.orEmpty().toMutableList()

        if (exchangesFromOcr.isEmpty()) {
            // 1회차 항목을 생성하여 추가
            val firstExchange = OcrRecordExchangeData(
                id = null,
                exchangeNo = 1,
                exchangeTime = null,
                drainVolume = null,
                fillConcentration = null,
                fillVolume = null,
                uf = null
            )
            exchangesFromOcr.add(firstExchange)
        }

        exchangeList.clear()
        exchangeList.addAll(exchangesFromOcr)

        // 회차 번호 정렬
        exchangeList.forEachIndexed { index, item ->
            exchangeList[index] = item.copy(exchangeNo = index + 1)
        }

        // 펼침 상태도 같이 초기화
        expandedStates.clear()
        repeat(exchangeList.size) {
            expandedStates.add(true) // 처음엔 전부 펼침
        }

        renderExchangeViews()
        updateAddButtonVisibility()

        if (isFromOcr) {
            val totalUfFromOcr = ocr?.ocrData?.totalUf

            if (totalUfFromOcr != null) {
                binding.totalUfEt.setText(totalUfFromOcr.toString())
            }
        }
    }

    private fun updateAddButtonVisibility() {
        if (exchangeList.size >= MAX_EXCHANGES) {
            binding.addExchangeBtn.visibility = View.GONE
        } else {
            binding.addExchangeBtn.visibility = View.VISIBLE
        }
    }

    private fun showError(message: String) {
        binding.recordErrorTv.text = message
        binding.recordErrorTv.visibility = View.VISIBLE
    }

    private fun clearError() {
        binding.recordErrorTv.text = ""
        binding.recordErrorTv.visibility = View.GONE
    }

    private fun collectDataAndCallApi() {
        fun enableButtonAndReturn() {
            binding.saveBtn.isEnabled = true
        }

        clearError() // 이전 에러 초기화

        val draft = commonDraft
        if (draft == null) {
            showError("공통 정보가 없습니다. 처음 화면부터 다시 작성해주세요.")
            enableButtonAndReturn()
            return
        }

        val totalUfText = binding.totalUfEt.text.toString()
        val totalUf = totalUfText.toIntOrNull()
        if (totalUf == null) {
            showError("제수량 합계를 입력해주세요.")
            enableButtonAndReturn()
            return
        }

        if (totalUf < -2500 || totalUf > 2500) {
            showError("제수량 합계는 -2500 ~ 2500 g 사이로 입력해주세요. (현재: $totalUf)")
            enableButtonAndReturn()
            return
        }

        val exchanges = exchangeList

        val requestList = mutableListOf<RecordExchangeCreateRequest>()
        for (item in exchanges) {
            val exNo = item.exchangeNo ?: 0

            if (item.exchangeTime == null) {
                showError("${exNo}회차 시간을 입력해주세요.")
                enableButtonAndReturn()
                return
            }

            if (item.drainVolume == null) {
                showError("${exNo}회차 배액량을 입력해주세요.")
                enableButtonAndReturn()
                return
            }

            if (item.fillVolume == null) {
                showError("${exNo}회차 주입량을 입력해주세요.")
                enableButtonAndReturn()
                return
            }

            if (item.fillConcentration == null) {
                showError("${exNo}회차 주입액 농도를 입력해주세요.")
                enableButtonAndReturn()
                return
            }

            if (item.uf == null) {
                showError("${exNo}회차 제수량을 입력해주세요.")
                enableButtonAndReturn()
                return
            }

            if (item.drainVolume < 0 || item.drainVolume > 6000) {
                showError("${exNo}회차 배액량은 0 ~ 6000 g 사이로 입력해주세요. (현재: ${item.drainVolume})")
                enableButtonAndReturn()
                return
            }

            if (item.fillVolume < 0 || item.fillVolume > 6000) {
                showError("${exNo}회차 주입량은 0 ~ 6000 g 사이로 입력해주세요. (현재: ${item.fillVolume})")
                enableButtonAndReturn()
                return
            }

            if (item.fillConcentration < 0.0 || item.fillConcentration > 100.0) {
                showError("${exNo}회차 주입액 농도는 0.0 ~ 100.0 % 사이로 입력해주세요. (현재: ${item.fillConcentration})")
                enableButtonAndReturn()
                return
            }

            if (item.uf < -500 || item.uf > 500) {
                showError("${exNo}회차 제수량은 -500 ~ 500 g 사이로 입력해주세요. (현재: ${item.uf})")
                enableButtonAndReturn()
                return
            }

            requestList += RecordExchangeCreateRequest(
                exchangeNo = item.exchangeNo,
                exchangeTime = item.exchangeTime.format(TIME_FORMATTER),
                drainVolume = item.drainVolume,
                fillVolume = item.fillVolume,
                fillConcentration = item.fillConcentration,
                uf = item.uf
            )
        }

        val recordDate = runCatching { LocalDate.parse(draft.recordDate) }.getOrElse {
            showError("날짜 형식이 올바르지 않습니다.")
            enableButtonAndReturn()
            return
        }

        validateUfAndShowDialogIfNeeded(
            totalUf = totalUf,
            exchanges = exchanges,
            onProceed = {
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
                enableButtonAndReturn()
            }
        )
    }

    private fun setupObservers() {

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recordSuccess.collectLatest {
                    // 기록 저장 성공 시에만 OCR 상태 정리
                    viewModel.clearOcr()

                    // 기존 백스택 다 비우기
                    parentFragmentManager.popBackStack(
                        null,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_frm, HomeFragment())
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
            val uf = item.uf ?: return@forEach
            sumUf += uf

            val drainVol = item.drainVolume ?: return@forEach
            val fillVol = item.fillVolume ?: return@forEach
            val calculatedUf = drainVol - fillVol

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

    private fun renderExchangeViews() {
        val container = binding.exchangeContainer
        container.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

        exchangeList.forEachIndexed { index, item ->
            val itemBinding = ItemRecordExchangeBinding.inflate(inflater, container, false)

            // 혹시 expandedStates 길이가 모자라면 채워주기
            if (expandedStates.size <= index) {
                expandedStates.add(true)
            }
            val isExpanded = expandedStates[index]

            itemBinding.exchangeNoTv.text = "${item.exchangeNo ?: (index + 1)}회차"

            // 드롭다운 상태에 따라 보이기/숨기기
            itemBinding.detailsContainer.visibility =
                if (isExpanded) View.VISIBLE else View.GONE

            // 아이콘도 위/아래로 변경
            itemBinding.dropdownIv.setImageResource(
                if (isExpanded) R.drawable.ic_arrow_up_sv
                else R.drawable.ic_arrow_down_sv
            )

            // 토글용 공용 리스너
            val toggleListener = View.OnClickListener {
                val newState = !(expandedStates.getOrNull(index) ?: true)
                expandedStates[index] = newState

                itemBinding.detailsContainer.visibility =
                    if (newState) View.VISIBLE else View.GONE

                itemBinding.dropdownIv.setImageResource(
                    if (newState) R.drawable.ic_arrow_up_sv
                    else R.drawable.ic_arrow_down_sv
                )
            }

            // 리스너 연결
            itemBinding.dropdownIv.setOnClickListener(toggleListener)

            itemBinding.deleteIv.setOnClickListener {
                if (exchangeList.size <= 1) {
                    Toast.makeText(requireContext(), "최소 1회차는 유지해야 합니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // 삭제 대상 백업
                val removedIndex = index
                val removedItem = exchangeList[removedIndex]
                val removedExpanded = expandedStates[removedIndex]
                val removedOrder = removedItem.exchangeNo

                // 실제 삭제
                exchangeList.removeAt(removedIndex)
                expandedStates.removeAt(removedIndex)

                // 회차 번호 다시 정렬
                exchangeList.forEachIndexed { i, ex ->
                    exchangeList[i] = ex.copy(exchangeNo = i + 1)
                }

                renderExchangeViews()
                updateAddButtonVisibility()

                // Snackbar로 "되돌리기" 제공
                Snackbar.make(
                    binding.root,
                    "${removedOrder}회차를 삭제했습니다.",
                    Snackbar.LENGTH_LONG
                ).setAction("되돌리기") {
                    // 원래 위치에 다시 추가
                    val safeIndex = removedIndex.coerceIn(0, exchangeList.size)
                    exchangeList.add(safeIndex, removedItem)
                    expandedStates.add(safeIndex, removedExpanded)

                    // 다시 회차 번호 정렬
                    exchangeList.forEachIndexed { i, ex ->
                        exchangeList[i] = ex.copy(exchangeNo = i + 1)
                    }

                    renderExchangeViews()
                    updateAddButtonVisibility()
                }.show()
            }

            val timeText = item.exchangeTime?.format(TIME_FORMATTER) ?: ""
            itemBinding.exchangeTimeEt.setText(timeText)
            itemBinding.exchangeTimeEt.setOnClickListener {
                showTimePickerDialog(itemBinding.exchangeTimeEt, index)
            }

            itemBinding.drainVolumeEt.setText(
                item.drainVolume?.toString() ?: ""
            )
            itemBinding.fillVolumeEt.setText(
                item.fillVolume?.toString() ?: ""
            )
            itemBinding.fillConcentrationEt.setText(
                item.fillConcentration?.toString() ?: ""
            )
            itemBinding.ufEt.setText(
                item.uf?.toString() ?: ""
            )

            itemBinding.drainVolumeEt.addTextChangedListener {
                val text = it?.toString()?.trim()
                val v = text?.toIntOrNull()
                exchangeList[index] = exchangeList[index].copy(drainVolume = v)
            }

            itemBinding.fillVolumeEt.addTextChangedListener {
                val text = it?.toString()?.trim()
                val v = text?.toIntOrNull()
                exchangeList[index] = exchangeList[index].copy(fillVolume = v)
            }

            itemBinding.fillConcentrationEt.addTextChangedListener {
                val text = it?.toString()?.trim()
                val v = text?.toDoubleOrNull()
                exchangeList[index] = exchangeList[index].copy(fillConcentration = v)
            }

            itemBinding.ufEt.addTextChangedListener {
                val text = it?.toString()?.trim()
                val v = text?.toIntOrNull()
                exchangeList[index] = exchangeList[index].copy(uf = v)
            }

            container.addView(itemBinding.root)
        }
    }

    companion object {
        private const val MAX_EXCHANGES = 5
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}