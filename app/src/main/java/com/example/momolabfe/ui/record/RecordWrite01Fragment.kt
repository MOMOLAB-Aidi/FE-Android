package com.example.momolabfe.ui.record

import android.app.AlertDialog
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.DialogCalendarBinding
import com.example.momolabfe.databinding.FragmentRecordWrite01Binding
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

class RecordWrite01Fragment : Fragment() {

    private var _binding: FragmentRecordWrite01Binding? = null
    private val binding get() = _binding!!

    private val today: LocalDate = LocalDate.now()
    private var selectedDate: LocalDate = LocalDate.now()

    private var visibleMonth: YearMonth = YearMonth.now()
    private val headerFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN)
    private val displayFormatter = DateTimeFormatter.ofPattern(DATE_DISPLAY_PATTERN)

    // 중복 호출 방지용 캐시: 마지막으로 서버에 요청했던 [시작일, 종료일]
    private var lastRequestedRange: Pair<LocalDate, LocalDate>? = null

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

                // 오늘 표시
                if (day.date == today) {
                    tv.background = circleFill(
                        fillColor = ContextCompat.getColor(requireContext(), R.color.gray)
                    )
                }

                // 날짜 선택
                if (day.date == dialogSelectedDate && isThisMonth) {
                    tv.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    tv.background = circleFill(ContextCompat.getColor(requireContext(), R.color.main_1))
                }

                // 클릭으로 선택 처리
                container.view.setOnClickListener {
                    if (!isThisMonth) return@setOnClickListener

                    val old = dialogSelectedDate
                    dialogSelectedDate = day.date

                    monthCalendar.notifyDateChanged(old)
                    monthCalendar.notifyDateChanged(dialogSelectedDate)
                }
            }
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
        val start = month.weekDays.first().first().date
        val end = month.weekDays.last().last().date
        val range = start to end
        if (lastRequestedRange == range) return
        lastRequestedRange = range
        // TODO: 서버에 해당 범위의 데이터를 요청하는 실제 API 호출 로직 추가
    }

    // DayView의 뷰 홀더
    private inner class DayViewContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.calendar_day_tv)
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
                layoutParams.width  = (resources.displayMetrics.widthPixels * 0.85).toInt()
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
        private const val DATE_DISPLAY_PATTERN = "yyyy.MM.dd"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}