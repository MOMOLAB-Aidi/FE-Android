package com.example.momolabfe.ui.record

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.momolabfe.R
import com.example.momolabfe.data.remote.record.model.GetCalendarResponse
import com.example.momolabfe.data.remote.record.model.Turbidity
import com.example.momolabfe.databinding.DialogCalendarBinding
import com.example.momolabfe.databinding.FragmentRecordWrite01Binding
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class RecordWrite01Fragment : Fragment() {

    private var _binding: FragmentRecordWrite01Binding? = null
    private val binding get() = _binding!!

    private val today: LocalDate = LocalDate.now()
    private var selectedDate: LocalDate = LocalDate.now()

    private var visibleMonth: YearMonth = YearMonth.now()
    private val headerFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN)
    private val displayFormatter : DateTimeFormatter =
        DateTimeFormatter.ofPattern(DATE_DISPLAY_PATTERN, Locale.KOREA)

    // 중복 호출 방지용 캐시: 마지막으로 서버에 요청했던 [시작일, 종료일]
    private var lastRequestedRange: Pair<Int, Int>? = null

    // 일정 있는 날짜들 캐시
    private val eventDates = hashSetOf<LocalDate>()

    private val viewModel: RecordViewModel by activityViewModels()

    // OCR 값으로 한 번만 채우기 위한 플래그
    private var isOcrApplied = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordWrite01Binding.inflate(inflater, container, false)

        binding.dateEt.setText(selectedDate.format(displayFormatter))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        // 최초 가시 월 기준으로 한 번 조회
        visibleMonth = YearMonth.now()

        binding.dateCv.setOnClickListener {
            showCalendarDialog()
        }

        binding.dateEt.setOnClickListener {
            showCalendarDialog()
        }

        binding.nextBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, RecordWrite02Fragment())
                .addToBackStack(null)
                .commit()
        }

        setupTurbidityCheckboxes()
        observeOcrAndFillFields()
    }

    private fun setupTurbidityCheckboxes() {
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
            requireContext(), if (isChecked) R.color.text_primary else R.color.gray
        )
        checkBox.buttonTintList = ColorStateList.valueOf(color)
    }

    private fun showCalendarDialog() {
        val dialogBinding = DialogCalendarBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
            .setView(dialogBinding.root)
            .create()

        // 캘린더 초기 상태 설정 (현재 선택된 날짜로 시작)
        var dialogSelectedDate: LocalDate = selectedDate
        var dialogVisibleMonth: YearMonth = YearMonth.of(selectedDate.year, selectedDate.month)

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

        // 월 스크롤 리스너 (헤더 갱신용)
        monthCalendar.monthScrollListener = { month ->
            dialogVisibleMonth = month.yearMonth
            dialogBinding.selectedDateTv.text = dialogVisibleMonth.format(headerFormatter)
            requestForMonth(month)
        }

        // 월별 DayBinder
        monthCalendar.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View): DayViewContainer = DayViewContainer(view)

            override fun bind(container: DayViewContainer, day: CalendarDay) {
                val tv = container.textView
                val dot = container.dotView

                // dot 간격 설정
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

                // 날짜 선택
                if (day.date == dialogSelectedDate && isThisMonth) {
                    tv.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    tv.background =
                        circleFill(ContextCompat.getColor(requireContext(), R.color.main_1))
                }

                // 클릭으로 선택 처리
                container.view.setOnClickListener {
                    if (!isThisMonth) return@setOnClickListener

                    // 이미 기록이 존재하는 날짜인지 확인
                    val hasRecord = eventDates.contains(day.date)

                    if (hasRecord) {
                        Toast.makeText(requireContext(), "해당 날짜에 이미 기록이 존재합니다.", Toast.LENGTH_SHORT).show()
                        // 기록이 있는 날짜는 선택 상태를 변경하지 않고 종료
                        return@setOnClickListener
                    }

                    val old = dialogSelectedDate
                    dialogSelectedDate = day.date

                    monthCalendar.notifyDateChanged(old)
                    monthCalendar.notifyDateChanged(dialogSelectedDate)
                }
            }
        }

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
            selectedDate = dialogSelectedDate
            binding.dateEt.setText(selectedDate.format(displayFormatter))
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
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
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

    // 월 범위 요청 함수 (캘린더 조회용)
    private fun requestForMonth(month: CalendarMonth) {
        val year = month.yearMonth.year
        val monthValue = month.yearMonth.monthValue

        val requestedYearMonth = month.yearMonth.year to month.yearMonth.monthValue

        if (lastRequestedRange?.first == requestedYearMonth.first &&
            lastRequestedRange?.second == requestedYearMonth.second
        ) {
            return
        }

        lastRequestedRange = requestedYearMonth
        viewModel.getCalendar(year, monthValue)
    }

    // DayView의 뷰 홀더
    private inner class DayViewContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.calendar_day_tv)
        val dotView: View = view.findViewById(R.id.dot_view)
    }

    // 채운 동그라미 배경
    private fun circleFill(fillColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fillColor)
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

    companion object {
        private const val DATE_PATTERN = "yyyy년 M월"
        private const val DATE_DISPLAY_PATTERN = "yyyy-MM-dd(E)"
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun observeOcrAndFillFields() {
        viewModel.ocrRecordResult.observe(viewLifecycleOwner) { ocr ->
            // null 이거나 이미 한 번 반영했다면 스킵
            if (ocr == null || isOcrApplied) return@observe

            isOcrApplied = true  // 다시 덮어쓰지 않도록 플래그 ON

            selectedDate = ocr.ocrData.recordDate
            binding.dateEt.setText(ocr.ocrData.recordDate.format(displayFormatter))
            binding.weightEt.setText(ocr.ocrData.weight.toString())
            binding.systolicEt.setText(ocr.ocrData.systolic.toString())
            binding.diastolicEt.setText(ocr.ocrData.diastolic.toString())
            binding.fastingGlucoseEt.setText(ocr.ocrData.fastingGlucose.toString())
            binding.urineCountEt.setText(ocr.ocrData.urineCount.toString())

            binding.turbidityNCheckbox.isChecked = false
            binding.turbidityYCheckbox.isChecked = false

            when (ocr.ocrData.turbidity) {
                Turbidity.NONE -> binding.turbidityNCheckbox.isChecked = true
                Turbidity.PRESENT -> binding.turbidityYCheckbox.isChecked = true
            }

            binding.totalUfEt.setText(ocr.ocrData.totalUf.toString())
            binding.notesEt.setText(ocr.ocrData.notes ?: "")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}