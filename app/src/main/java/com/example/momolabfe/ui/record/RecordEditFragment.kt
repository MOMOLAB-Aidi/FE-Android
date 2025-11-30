package com.example.momolabfe.ui.record

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.momolabfe.R
import com.example.momolabfe.databinding.DialogCalendarBinding
import com.example.momolabfe.databinding.DialogTimePickerBinding
import com.example.momolabfe.databinding.FragmentRecordEditBinding
import com.example.momolabfe.databinding.ItemExchangeEditBinding
import com.example.momolabfe.remote.record.model.DayWeek
import com.example.momolabfe.remote.record.model.GetCalendarResponse
import com.example.momolabfe.remote.record.model.RecordExchangeGetResponse
import com.example.momolabfe.remote.record.model.RecordExchangeUpdateRequest
import com.example.momolabfe.remote.record.model.RecordUpdateRequest
import com.example.momolabfe.remote.record.model.Turbidity
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.example.momolabfe.utils.dpToPx
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class RecordEditFragment : Fragment() {

    private var _binding: FragmentRecordEditBinding? = null
    private val binding get() = _binding!!

    private var recordId: Long = -1L

    private val viewModel: RecordViewModel by activityViewModels()

    // 회차 원본 리스트
    private var currentExchanges: List<RecordExchangeGetResponse> = emptyList()

    // 사용자가 수정한 날짜/요일
    private var editedRecordDate: LocalDate? = null
    private var editedRecordDw: DayWeek? = null

    private val today: LocalDate = LocalDate.now()
    private val headerFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN)

    // 중복 호출 방지용 캐시: 마지막으로 서버에 요청했던 [시작일, 종료일]
    private var lastRequestedRange: Pair<Int, Int>? = null

    // 일정 있는 날짜들 캐시
    private val eventDates = hashSetOf<LocalDate>()

    companion object {
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)
        private const val DATE_PATTERN = "yyyy년 M월"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordEditBinding.inflate(inflater, container, false)

        recordId = arguments?.getLong("record_id") ?: -1L

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showBackConfirmDialog()
                }
            }
        )
    }

    private fun showBackConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("기록 수정 취소")
            .setMessage("기록 수정을 취소하시겠습니까?\n\n" +
                    "현재까지 수정한 내용은 저장되지 않습니다.")
            .setNegativeButton("아니오") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("예") { dialog, _ ->
                dialog.dismiss()
                viewModel.clearOcr()
                parentFragmentManager.popBackStack()
            }
            .show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        setupTurbidityCheckboxes()
        setupObservers()

        if (recordId != -1L && viewModel.record.value?.id != recordId) {
            viewModel.getRecord(recordId)
        }

        binding.saveBtn.setOnClickListener {
            binding.saveBtn.isEnabled = false
            edit()
        }

        binding.cancelBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.changeDateBtn.setOnClickListener {
            showCalendarDialogForEdit()
        }
    }

    private fun setupTurbidityCheckboxes() {
        // 초기 상태 색상 적용
        val initialNChecked = binding.turbidityNCheckbox.isChecked
        setCheckBoxTint(binding.turbidityNCheckbox, initialNChecked)

        val initialYChecked = binding.turbidityYCheckbox.isChecked
        setCheckBoxTint(binding.turbidityYCheckbox, initialYChecked)

        binding.turbidityNCheckbox.setOnCheckedChangeListener { checkBox, isChecked ->
            setCheckBoxTint(checkBox, isChecked)
            if (isChecked) {
                binding.turbidityYCheckbox.isChecked = false
            }
        }

        binding.turbidityYCheckbox.setOnCheckedChangeListener { checkBox, isChecked ->
            setCheckBoxTint(checkBox, isChecked)
            if (isChecked) {
                binding.turbidityNCheckbox.isChecked = false
            }
        }
    }

    private fun setCheckBoxTint(checkBox: CompoundButton, isChecked: Boolean) {
        val color = ContextCompat.getColor(
            requireContext(),
            if (isChecked) R.color.text_primary else R.color.gray
        )
        checkBox.buttonTintList = ColorStateList.valueOf(color)
    }

    private fun getRecordRequest(): RecordUpdateRequest? {
        val currentRecord = viewModel.record.value

        val weight = binding.weightValueEt.text.toString().toDoubleOrNull()
        val systolic = binding.bloodPressureSystolicEt.text.toString().toIntOrNull()
        val diastolic = binding.bloodPressureDiastolicEt.text.toString().toIntOrNull()
        val fastingGlucose = binding.fastingGlucoseValueEt.text.toString().toIntOrNull()
        val urineCount = binding.urineCountValueEt.text.toString().toIntOrNull()

        val turbidity = when {
            binding.turbidityYCheckbox.isChecked -> Turbidity.PRESENT
            binding.turbidityNCheckbox.isChecked -> Turbidity.NONE
            else -> null
        }

        val totalUf = binding.totalUfValueEt.text.toString().toIntOrNull()
        val notes = binding.noteValueEt.text.toString().ifBlank { null }

        weight?.let {
            if (it < 20.0 || it > 300.0) {
                Toast.makeText(
                    requireContext(),
                    "체중은 20.0 ~ 300.0 kg 사이로 입력해주세요. (현재: $it)",
                    Toast.LENGTH_SHORT
                ).show()
                return null
            }
        }

        systolic?.let {
            if (it < 70 || it > 240) {
                Toast.makeText(
                    requireContext(),
                    "수축기 혈압은 70 ~ 240 mmHg 사이로 입력해주세요. (현재: $it)",
                    Toast.LENGTH_SHORT
                ).show()
                return null
            }
        }

        diastolic?.let {
            if (it < 40 || it > 160) {
                Toast.makeText(
                    requireContext(),
                    "이완기 혈압은 40 ~ 160 mmHg 사이로 입력해주세요. (현재: $it)",
                    Toast.LENGTH_SHORT
                ).show()
                return null
            }
        }

        fastingGlucose?.let {
            if (it < 40 || it > 600) {
                Toast.makeText(
                    requireContext(),
                    "공복 혈당은 40 ~ 600 mg/dL 사이로 입력해주세요. (현재: $it)",
                    Toast.LENGTH_SHORT
                ).show()
                return null
            }
        }

        urineCount?.let {
            if (it < 0 || it > 50) {
                Toast.makeText(
                    requireContext(),
                    "하루 소변 횟수는 0 ~ 50회 사이로 입력해주세요. (현재: $it)",
                    Toast.LENGTH_SHORT
                ).show()
                return null
            }
        }

        totalUf?.let {
            if (it < -5000 || it > 5000) {
                Toast.makeText(
                    requireContext(),
                    "제수량 합계는 -5000 ~ 5000 g 사이로 입력해주세요. (현재: $it)",
                    Toast.LENGTH_SHORT
                ).show()
                return null
            }
        }

        val finalRecordDate = editedRecordDate ?: currentRecord?.recordDate
        val finalRecordDw = editedRecordDw ?: currentRecord?.recordDw

        // --- 회차별 정보(동적 View에서 읽기) ---
        val container = binding.exchangeEditContainer
        val childCount = container.childCount

        if (childCount == 0) {
            // 회차 정보가 없으면 null 로 보내도 됨(백엔드 Optional이면)
            return RecordUpdateRequest(
                recordDate = finalRecordDate,
                recordDw = finalRecordDw,
                weight = weight,
                systolic = systolic,
                diastolic = diastolic,
                fastingGlucose = fastingGlucose,
                urineCount = urineCount,
                turbidity = turbidity,
                notes = notes,
                totalUf = totalUf,
                exchanges = null
            )
        }

        val exchanges = mutableListOf<RecordExchangeUpdateRequest>()

        for (i in 0 until childCount) {
            val child = container.getChildAt(i)
            val itemBinding = ItemExchangeEditBinding.bind(child)

            val no = currentExchanges.getOrNull(i)?.exchangeNo ?: (i + 1)

            // 교환 시각
            val timeStr = itemBinding.exchangeTimeValueEt.text.toString().trim()
            val exchangeTime = try {
                LocalTime.parse(timeStr, TIME_FORMATTER)
            } catch (e: Exception) {
                Log.w("RECORD_EDIT_FRAGMENT", "시간 파싱 실패: $timeStr", e)
                Toast.makeText(requireContext(),"${no}회차 교환 시각을 올바른 형식(HH:mm)으로 입력해주세요. (현재: '$timeStr')", Toast.LENGTH_SHORT).show()
                return null
            }

            // 배액량
            val drainStr = itemBinding.drainVolumeValueEt.text.toString()
            val drainVolume = drainStr.toIntOrNull()
            if (drainVolume == null) {
                Toast.makeText(requireContext(),"${no}회차 배액량을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return null
            }
            if (drainVolume < 0 || drainVolume > 6000) {
                Toast.makeText(requireContext(),"${no}회차 배액량은 0 ~ 6000 g 사이로 입력해주세요. (현재: $drainVolume)", Toast.LENGTH_SHORT).show()
                return null
            }

            // 주입량
            val fillStr = itemBinding.fillVolumeValueEt.text.toString()
            val fillVolume = fillStr.toIntOrNull()
            if (fillVolume == null) {
                Toast.makeText(requireContext(),"${no}회차 주입액 중량을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return null
            }
            if (fillVolume < 0 || fillVolume > 6000) {
                Toast.makeText(requireContext(),"${no}회차 주입액 중량은 0 ~ 6000 g 사이로 입력해주세요. (현재: $fillVolume)", Toast.LENGTH_SHORT).show()
                return null
            }

            // 주입액 농도
            val concStr = itemBinding.fillConcentrationValueEt.text.toString()
            val fillConcentration = concStr.toDoubleOrNull()
            if (fillConcentration == null) {
                Toast.makeText(requireContext(),"${no}회차 주입액 농도를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return null
            }
            if (fillConcentration < 0.0 || fillConcentration > 100.0) {
                Toast.makeText(requireContext(),"${no}회차 주입액 농도는 0.0 ~ 100.0 % 사이로 입력해주세요. (현재: $fillConcentration)", Toast.LENGTH_SHORT).show()
                return null
            }

            // 제수량
            val ufStr = itemBinding.ufValueEt.text.toString()
            val uf = ufStr.toIntOrNull()
            if (uf == null) {
                Toast.makeText(requireContext(),"${no}회차 제수량을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return null
            }
            if (uf < -500 || uf > 500) {
                Toast.makeText(requireContext(),"${no}회차 제수량은 -500 ~ 500 g 사이로 입력해주세요. (현재: $uf)", Toast.LENGTH_SHORT).show()
                return null
            }

            val original = currentExchanges.getOrNull(i)

            exchanges.add(
                RecordExchangeUpdateRequest(
                    id = original?.id, // 원래 회차 id 유지
                    exchangeTime = exchangeTime.toString(),
                    drainVolume = drainVolume,
                    fillVolume = fillVolume,
                    fillConcentration = fillConcentration,
                    uf = uf
                )
            )
        }

        val exchangesOrNull = exchanges.takeIf { it.isNotEmpty() }

        return RecordUpdateRequest(
            recordDate = finalRecordDate,
            recordDw = finalRecordDw,
            weight = weight,
            systolic = systolic,
            diastolic = diastolic,
            fastingGlucose = fastingGlucose,
            urineCount = urineCount,
            turbidity = turbidity,
            notes = notes,
            totalUf = totalUf,
            exchanges = exchangesOrNull
        )
    }

    private fun edit() {
        // 1순위: ViewModel에 로드된 기록의 id 사용
        val currentRecord = viewModel.record.value
        val currentRecordId = currentRecord?.id ?: -1L

        // 2순위: arguments 로 받은 recordId
        val finalId = if (currentRecordId != -1L) currentRecordId else recordId

        if (finalId == -1L) {
            binding.saveBtn.isEnabled = true
            return
        }

        // 나중에 editSuccess에서 다시 조회할 수 있도록 기억해두기
        recordId = finalId

        val request = getRecordRequest()
        if (request == null) {
            binding.saveBtn.isEnabled = true
            return
        }

        viewModel.updateRecord(finalId, request)
    }

    private fun setupObservers() {
        viewModel.record.observe(viewLifecycleOwner) { record ->
            if (record == null) return@observe

            // 아직 사용자가 날짜를 수정하지 않았다면, 서버에서 받은 날짜로 표시
            val displayDate = editedRecordDate ?: record.recordDate
            val displayDw = editedRecordDw ?: record.recordDw

            binding.contentTv.text = formatRecordDate(displayDate, displayDw)

            // 공통 정보 세팅
            binding.weightValueEt.setText(record.weight.toString())
            binding.bloodPressureSystolicEt.setText(record.systolic.toString())
            binding.bloodPressureDiastolicEt.setText(record.diastolic.toString())
            binding.fastingGlucoseValueEt.setText(record.fastingGlucose.toString())
            binding.urineCountValueEt.setText(record.urineCount.toString())
            binding.totalUfValueEt.setText(record.totalUf.toString())
            binding.noteValueEt.setText(record.notes ?: "")

            when (record.turbidity) {
                Turbidity.PRESENT -> {
                    binding.turbidityYCheckbox.isChecked = true
                    binding.turbidityNCheckbox.isChecked = false
                }
                Turbidity.NONE -> {
                    binding.turbidityYCheckbox.isChecked = false
                    binding.turbidityNCheckbox.isChecked = true
                }
            }

            // 체크 상태 바꾼 뒤 색상도 동기화
            setCheckBoxTint(binding.turbidityNCheckbox, binding.turbidityNCheckbox.isChecked)
            setCheckBoxTint(binding.turbidityYCheckbox, binding.turbidityYCheckbox.isChecked)

            // 회차 리스트 세팅
            val exchangesForEdit = record.exchanges.map { item ->
                RecordExchangeGetResponse(
                    id = item.id,
                    exchangeNo = item.exchangeNo,
                    exchangeTime = item.exchangeTime,
                    drainVolume = item.drainVolume,
                    fillConcentration = item.fillConcentration,
                    fillVolume = item.fillVolume,
                    uf = item.uf
                )
            }
            currentExchanges = exchangesForEdit
            renderExchangeEditViews(exchangesForEdit)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recordSuccess.collect {
                        Log.d("RECORD_EDIT_FRAGMENT", "기록이 성공적으로 수정되었습니다.")
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            Log.e("RECORD_EDIT_FRAGMENT", errorMsg.toString())
            binding.saveBtn.isEnabled = true
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

    private fun showTimePickerDialogForView(exchangeIndex: Int, targetEditText: EditText) {
        val dialogBinding = DialogTimePickerBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
            .setView(dialogBinding.root)
            .create()

        setupPickersInDialog(dialogBinding)

        dialogBinding.applyTv.setOnClickListener {
            val (timeText, _) = buildSelectedTime(dialogBinding)
            targetEditText.setText(timeText)
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

    private fun buildSelectedTime(dialogBinding: DialogTimePickerBinding): Pair<String, LocalTime> {
        val ampmPicker = dialogBinding.ampmPickerNp
        val hourPicker = dialogBinding.hourPickerNp
        val minutePicker = dialogBinding.minutePickerNp

        val ampm = ampmPicker.value
        val hour12 = hourPicker.value
        val minuteStr = arrayOf("00", "10", "20", "30", "40", "50")[minutePicker.value]

        val hour24 = convertTo24Hour(ampm, hour12)
        val timeText = String.format(Locale.KOREA, "%02d:%s", hour24, minuteStr)
        val time = LocalTime.parse(timeText, TIME_FORMATTER)

        return timeText to time
    }

    private fun convertTo24Hour(ampm: Int, hour12: Int): Int {
        return when {
            ampm == 0 && hour12 == 12 -> 0
            ampm == 0 -> hour12
            ampm == 1 && hour12 == 12 -> 12
            else -> hour12 + 12
        }
    }

    private fun applyDialogWindow(dialog: AlertDialog) {
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

    private fun renderExchangeEditViews(exchanges: List<RecordExchangeGetResponse>) {
        val container = binding.exchangeEditContainer
        container.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

        exchanges.forEachIndexed { index, item ->
            val itemBinding = ItemExchangeEditBinding.inflate(inflater, container, false)

            itemBinding.exchangeTitleTv.text = "${index + 1}회차"

            val timeText = item.exchangeTime.format(TIME_FORMATTER) ?: "-"
            itemBinding.exchangeTimeValueEt.setText(timeText)

            itemBinding.exchangeTimeValueEt.setOnClickListener {
                showTimePickerDialogForView(index, itemBinding.exchangeTimeValueEt)
            }

            itemBinding.drainVolumeValueEt.setText(item.drainVolume.toString())
            itemBinding.fillVolumeValueEt.setText(item.fillVolume.toString())
            itemBinding.fillConcentrationValueEt.setText(item.fillConcentration.toString())
            itemBinding.ufValueEt.setText(item.uf.toString())

            container.addView(itemBinding.root)
        }
    }

    private fun showCalendarDialogForEdit() {
        val currentRecord = viewModel.record.value

        val dialogBinding = DialogCalendarBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
            .setView(dialogBinding.root)
            .create()

        var dialogSelectedDate: LocalDate =
            editedRecordDate ?: currentRecord?.recordDate ?: today

        var dialogVisibleMonth: YearMonth =
            YearMonth.of(dialogSelectedDate.year, dialogSelectedDate.month)

        // 캘린더 뷰 초기화
        val monthCalendar: CalendarView = dialogBinding.calendarView
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusYears(50)
        val endMonth = currentMonth.plusYears(50)
        val firstDayOfWeek = daysOfWeek().first()

        monthCalendar.setup(startMonth, endMonth, firstDayOfWeek)
        monthCalendar.scrollToMonth(dialogVisibleMonth)

        dialogBinding.selectedDateTv.text = dialogVisibleMonth.format(headerFormatter)
        setupWeekdayLabels(dialogBinding.calendarWeekdaysRow, firstDayOfWeek)

        dialogBinding.calendarPreviousDateIv.setOnClickListener {
            dialogVisibleMonth = dialogVisibleMonth.minusMonths(1)
            monthCalendar.smoothScrollToMonth(dialogVisibleMonth)
            dialogBinding.selectedDateTv.text = dialogVisibleMonth.format(headerFormatter)
        }

        dialogBinding.calendarNextDateIv.setOnClickListener {
            dialogVisibleMonth = dialogVisibleMonth.plusMonths(1)
            monthCalendar.smoothScrollToMonth(dialogVisibleMonth)
            dialogBinding.selectedDateTv.text = dialogVisibleMonth.format(headerFormatter)
        }

        val initialYear = dialogVisibleMonth.year
        val initialMonthValue = dialogVisibleMonth.monthValue
        val initialPair = initialYear to initialMonthValue

        if (lastRequestedRange != initialPair) {
            lastRequestedRange = initialPair
            viewModel.getCalendar(initialYear, initialMonthValue)
        }

        // 월 스크롤 리스너
        monthCalendar.monthScrollListener = { month ->
            dialogVisibleMonth = month.yearMonth
            dialogBinding.selectedDateTv.text = dialogVisibleMonth.format(headerFormatter)
            requestForMonth(month)
        }

        // DayBinder
        monthCalendar.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View): DayViewContainer = DayViewContainer(view)

            override fun bind(container: DayViewContainer, day: CalendarDay) {
                val tv = container.textView
                val dot = container.dotView

                // dot 위쪽 간격
                (dot.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                    params.topMargin = dpToPx(3)
                    dot.layoutParams = params
                }

                tv.text = day.date.dayOfMonth.toString()
                tv.typeface = Typeface.DEFAULT
                tv.background = null

                val isThisMonth = day.position == DayPosition.MonthDate

                tv.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (isThisMonth) R.color.text_primary else R.color.deactive
                    )
                )

                // 일정 점 표시
                dot.visibility =
                    if (eventDates.contains(day.date) && isThisMonth) View.VISIBLE else View.GONE

                // 오늘 표시
                if (day.date == today) {
                    tv.background = circleFill(
                        fillColor = ContextCompat.getColor(requireContext(), R.color.gray)
                    )
                }

                // 선택된 날짜 표시
                if (day.date == dialogSelectedDate && isThisMonth) {
                    tv.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            android.R.color.white
                        )
                    )
                    tv.background =
                        circleFill(ContextCompat.getColor(requireContext(), R.color.main_1))
                }

                // 날짜 클릭 처리
                container.view.setOnClickListener {
                    if (!isThisMonth) return@setOnClickListener

                    val hasRecord = eventDates.contains(day.date)
                    val isCurrentRecordDate = currentRecord?.recordDate == day.date

                    if (hasRecord && !isCurrentRecordDate) {
                        Toast.makeText(
                            requireContext(),
                            "해당 날짜에 이미 다른 기록이 존재합니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    val old = dialogSelectedDate
                    dialogSelectedDate = day.date

                    monthCalendar.notifyDateChanged(old)
                    monthCalendar.notifyDateChanged(dialogSelectedDate)
                }
            }
        }

        // 기록이 있는 날짜에 점 표시
        val observer = Observer<List<GetCalendarResponse>> { items ->
            eventDates.clear()
            items.forEach { ev ->
                if (ev.hasSchedule) {
                    eventDates += ev.date
                }
            }
            monthCalendar.notifyCalendarChanged()
        }

        viewModel.calendarData.observe(viewLifecycleOwner, observer)

        dialog.setOnDismissListener {
            viewModel.calendarData.removeObserver(observer)
        }

        dialogBinding.applyTv.setOnClickListener {
            editedRecordDate = dialogSelectedDate
            editedRecordDw = convertDayOfWeekToDayWeek(dialogSelectedDate.dayOfWeek)

            binding.contentTv.text = formatRecordDate(editedRecordDate, editedRecordDw)
            dialog.dismiss()
        }

        dialogBinding.cancelTv.setOnClickListener {
            dialog.dismiss()
        }

        applyDialogWindow(dialog)
        dialog.show()
    }

    private fun setupWeekdayLabels(container: LinearLayout, firstDayOfWeek: DayOfWeek) {
        container.removeAllViews()

        val days = daysOfWeek()
        days.forEach { dow ->
            val tv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                text = weekdayShortKorean(dow)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
            container.addView(tv)
        }
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

    private fun convertDayOfWeekToDayWeek(d: DayOfWeek): DayWeek = when (d) {
        DayOfWeek.MONDAY -> DayWeek.MON
        DayOfWeek.TUESDAY -> DayWeek.TUE
        DayOfWeek.WEDNESDAY -> DayWeek.WED
        DayOfWeek.THURSDAY -> DayWeek.THU
        DayOfWeek.FRIDAY -> DayWeek.FRI
        DayOfWeek.SATURDAY -> DayWeek.SAT
        DayOfWeek.SUNDAY -> DayWeek.SUN
    }

    // 월 단위 조회
    private fun requestForMonth(month: CalendarMonth) {
        val year = month.yearMonth.year
        val monthValue = month.yearMonth.monthValue

        val requestedYearMonth = year to monthValue

        if (lastRequestedRange == requestedYearMonth) {
            return
        }

        lastRequestedRange = requestedYearMonth
        viewModel.getCalendar(year, monthValue)
    }

    // DayView 컨테이너
    private inner class DayViewContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.calendar_day_tv)
        val dotView: View = view.findViewById(R.id.dot_view)
    }

    // 동그라미 배경
    private fun circleFill(fillColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fillColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}