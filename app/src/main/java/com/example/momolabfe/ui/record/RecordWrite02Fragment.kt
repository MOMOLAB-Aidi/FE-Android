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
import java.time.LocalTime
import java.util.Locale

@AndroidEntryPoint
class RecordWrite02Fragment : Fragment(), RecordExchangeAdapter.OnTimePickerClickListener {

    private var _binding: FragmentRecordWrite02Binding? = null
    private val binding get() = _binding!!

    private var recordId: Long = -1L

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

        recordId = arguments?.getLong("recordId") ?: -1L
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

    private fun applyDialogWindow(dialog: AlertDialog) {
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnShowListener {
            dialog.window?.let { window ->
                val layoutParams = window.attributes
                layoutParams.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                layoutParams.gravity = Gravity.CENTER
                layoutParams.dimAmount = 0.5f
                window.attributes = layoutParams
                window.setDimAmount(0.5f)
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
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
        binding.exchangeRv.scrollToPosition(currentExchanges.size - 1)
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

        val draft = commonDraft
        if (draft == null) {
            Toast.makeText(requireContext(), "공통 정보가 없습니다. 처음 화면부터 다시 작성해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val totalUfText = binding.totalUfEt.text.toString()
        val totalUf = totalUfText.toIntOrNull()
        if (totalUf == null) {
            Toast.makeText(requireContext(), "제수량 합계를 숫자로 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val exchanges = exchangeAdapter.items

        // 각 회차 데이터 유효성 검사 및 DTO 리스트 생성
        val requestList = mutableListOf<RecordExchangeCreateRequest>()
        for (item in exchanges) {

            if (item.drainVolume <= 0 || item.fillVolume <= 0 || item.fillConcentration <= 0.0) {
                Toast.makeText(
                    requireContext(),
                    "${item.exchangeNo}회차 기록을 확인해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
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

        val fullRequest = RecordCreateRequest(
            recordDate = draft.recordDate,
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

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearOcr()
        _binding = null
    }
}