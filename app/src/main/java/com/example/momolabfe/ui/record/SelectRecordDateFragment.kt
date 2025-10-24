package com.example.momolabfe.ui.record

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordSelectDateBinding
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class SelectRecordDateFragment : Fragment() {

    private var _binding: FragmentRecordSelectDateBinding? = null
    private val binding get() = _binding!!

    private lateinit var monthCalendar: CalendarView

    private val today: LocalDate = LocalDate.now()
    private var selectedDate: LocalDate = today

    private var visibleMonth: YearMonth = YearMonth.now()
    private val headerFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN)
    private val sendFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")

    // 중복 호출 방지용 캐시: 마지막으로 서버에 요청했던 [시작일, 종료일]
    private var lastRequestedRange: Pair<LocalDate, LocalDate>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordSelectDateBinding.inflate(inflater, container, false)

        selectedDate = today
        updateHeaderForCurrentMode()

        binding.calendarPreviousDateIv.setOnClickListener {
            visibleMonth = visibleMonth.minusMonths(1)
            monthCalendar.smoothScrollToMonth(visibleMonth)
            updateHeaderForCurrentMode()
        }

        binding.calendarNextDateIv.setOnClickListener {
            visibleMonth = visibleMonth.plusMonths(1)
            monthCalendar.smoothScrollToMonth(visibleMonth)
            updateHeaderForCurrentMode()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        monthCalendar = binding.calenderView

        // 해당 라이브러리는 캘린더 범위를 무제한으로 설정할 수 없어 일단은 +-50년으로 설정...
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusYears(50) // 50년 전
        val endMonth = currentMonth.plusYears(50)  // 50년 후
        val firstDayOfWeek = DayOfWeek.SUNDAY

        // 월 달력: 월 범위로 설정
        monthCalendar.setup(startMonth, endMonth, firstDayOfWeek)
        monthCalendar.scrollToMonth(currentMonth)

        // 월 스크롤 리스너 (헤더 갱신용)
        monthCalendar.monthScrollListener = { month ->
            visibleMonth = month.yearMonth
            updateHeaderForCurrentMode()
            requestForMonth(month)
        }

        setupWeekdayLabels(firstDayOfWeek)

        // 월별 DayBinder
        monthCalendar.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View): DayViewContainer = DayViewContainer(view)

            override fun bind(container: DayViewContainer, day: CalendarDay) {
                val tv = container.textView

                // 기본 스타일 초기화
                tv.text = day.date.dayOfMonth.toString()
                tv.typeface = Typeface.DEFAULT
                tv.background = null

                // 이번 달 셀만 활성화, out-date는 비활성화/회색
                val isThisMonth = day.position == DayPosition.MonthDate

                // 회색 텍스트 적용
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
                if (day.date == selectedDate && isThisMonth) {
                    tv.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    tv.background = circleFill(ContextCompat.getColor(requireContext(), R.color.main_1))
                }

                // 클릭으로 선택 처리
                container.view.setOnClickListener {
                    if (!isThisMonth) return@setOnClickListener  // 전환/선택 방지

                    val old = selectedDate
                    selectedDate = day.date

                    // 월 갱신
                    monthCalendar.notifyDateChanged(old)
                    monthCalendar.notifyDateChanged(selectedDate)

                    updateHeaderForCurrentMode()
                }
            }
        }

        view.post {
            monthCalendar.findFirstVisibleMonth()?.let { requestForMonth(it) }
        }

        binding.nextBtn.setOnClickListener {
            val dateText = selectedDate.format(sendFormatter) // 선택한 날짜 값 전달
            val args = Bundle().apply { putString("selected_date_text", dateText) }
            val fragment = RecordExchangeInfoFragment().apply { arguments = args } // 추후에 CommonRecordInfoFragment로 수정 필요

            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun updateHeaderForCurrentMode() {
        binding.homeSelectedDateTv.text = visibleMonth.format(headerFormatter)
    }

    // 요일 텍스트 설정 (일~토)
    private fun setupWeekdayLabels(firstDayOfWeek: DayOfWeek) {
        val container = binding.calendarWeekdaysRow
        container.removeAllViews()

        val days = (0..6).map { firstDayOfWeek.plus(it.toLong()) }
        days.forEach { dow ->
            val tv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
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
    }

    companion object {
        private const val DATE_PATTERN = "yyyy년 M월"
    }

    // 채운 동그라미 배경
    private fun circleFill(fillColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fillColor)
        }
    }

    // DayView의 뷰 홀더
    private inner class DayViewContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.calendar_day_tv)
    }
}