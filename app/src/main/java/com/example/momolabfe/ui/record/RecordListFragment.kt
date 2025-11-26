package com.example.momolabfe.ui.record

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordListBinding
import com.example.momolabfe.remote.record.data.GetCalendarResponse
import com.example.momolabfe.remote.record.data.RecordGetResponse
import com.example.momolabfe.ui.record.adapter.RecordExchangeDetailAdapter
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.example.momolabfe.utils.dpToPx
import com.example.momolabfe.utils.weekdayShortKorean
import com.google.android.material.bottomnavigation.BottomNavigationView
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
import java.util.Locale

class RecordListFragment : Fragment() {

    private var _binding: FragmentRecordListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RecordExchangeDetailAdapter

    private val monthCalendar: CalendarView
        get() = binding.calenderView

    private val today: LocalDate = LocalDate.now()
    private var selectedDate: LocalDate = today

    private var visibleMonth: YearMonth = YearMonth.now()
    private val headerFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN)
    private val DATE_DISPLAY_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREA)

    // 중복 호출 방지용 캐시: 마지막으로 서버에 요청했던 [시작일, 종료일]
    private var lastRequestedRange: Pair<Int, Int>? = null

    // 일정 있는 날짜들 캐시
    private val eventDates = hashSetOf<LocalDate>()
    private val viewModel: RecordViewModel by activityViewModels()

    private var monthRecordList: List<RecordGetResponse> = emptyList()
    private var selectedRecordId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordListBinding.inflate(inflater, container, false)

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

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        adapter = RecordExchangeDetailAdapter(emptyList())
        binding.exchangeDetailRv.adapter = adapter

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
                val dot = container.dotView

                // dot 간격 설정
                (dot.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                    params.topMargin = this@RecordListFragment.dpToPx(3)
                    dot.layoutParams = params
                }

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
                if (day.date == selectedDate && isThisMonth) {
                    tv.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    tv.background = circleFill(ContextCompat.getColor(requireContext(), R.color.dot))
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

                    // 선택한 날짜에 해당하는 기록 찾기
                    val target = monthRecordList.firstOrNull { rec ->
                        rec.recordDate == selectedDate
                    }

                    if (target != null) {
                        selectedRecordId = target.id
                        showDetailViews()
                        viewModel.getRecord(target.id)
                    } else {
                        selectedRecordId = null
                        hideDetailViews()
                        clearRecordViews()
                        adapter.updateList(emptyList())
                    }
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

        setupObservers()

        binding.detailEditBtn.setOnClickListener {
            binding.detailEditBtn.isEnabled = false
            val currentId = selectedRecordId

            if (currentId == null) {
                binding.detailEditBtn.isEnabled = true
                return@setOnClickListener
            }
            try {
                val fragment = RecordInfoFragment().apply {
                    arguments = Bundle().apply {
                        putLong("record_id", currentId)
                    }
                }

                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_frm, fragment)
                    .addToBackStack(null)
                    .commit()
            } finally {
                binding.detailEditBtn.isEnabled = true
            }

        }

        binding.deleteBtn.setOnClickListener {
            val currentId = selectedRecordId
            if (currentId == null) {
                Log.e("RECORD_LIST_FRAGMENT", "삭제할 recordId가 없습니다.")
                return@setOnClickListener
            }

            // 이미 표시된 BottomSheet가 있는지 확인
            if (parentFragmentManager.findFragmentByTag("BottomSheetRecordDelete") != null) {
                return@setOnClickListener
            }

            val bottomSheet = BottomSheetRecordDeleteFragment().apply {
                arguments = Bundle().apply {
                    putLong("record_id", currentId)
                }
            }
            bottomSheet.show(parentFragmentManager, "BottomSheetRecordDelete")
        }

        // 기록 삭제 성공 이벤트 수신
        parentFragmentManager.setFragmentResultListener("record_delete", viewLifecycleOwner) { _, _ ->
            // 현재 보여지는 달 기준으로 다시 조회
            val year = visibleMonth.year
            val monthValue = visibleMonth.monthValue

            viewModel.getCalendar(year, monthValue)
            viewModel.getRecordList(year, monthValue)
        }

        hideDetailViews()
    }

    private fun updateHeaderForCurrentMode() {
        binding.selectedDateTv.text = visibleMonth.format(headerFormatter)
        // 선택된 날짜 표시
        binding.selectedDateDisplayTv.text = selectedDate.format(DATE_DISPLAY_FORMATTER)
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
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                text = weekdayShortKorean(dow)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
            container.addView(tv)
        }
    }

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
        viewModel.getRecordList(year, monthValue)
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
        val dotView: View = view.findViewById(R.id.dot_view)
    }

    private fun setupObservers() {
        viewModel.recordlistItems.observe(viewLifecycleOwner) { itemList ->
            monthRecordList = itemList

            // 현재 선택된 날짜에 기록 있으면 자동으로 상세 조회
            val target = monthRecordList.firstOrNull { rec ->
                rec.recordDate == selectedDate
            }
            if (target != null) {
                selectedRecordId = target.id
                showDetailViews()
                viewModel.getRecord(target.id)
            } else {
                selectedRecordId = null
                hideDetailViews()
                clearRecordViews()
                adapter.updateList(emptyList())
            }
        }

        // 단일 기록 조회 결과
        viewModel.record.observe(viewLifecycleOwner) { recordItem ->
            if (recordItem != null) {
                selectedRecordId = recordItem.id
                showDetailViews()

                binding.selectedDateDisplayTv.text =
                    recordItem.recordDate.format(DATE_DISPLAY_FORMATTER)

                binding.weightValueTv.text = String.format(Locale.KOREA, "%.1fkg", recordItem.weight)

                val bpText = "${recordItem.systolic}/${recordItem.diastolic}"
                binding.bloodPressureValueTv.text = bpText

                binding.fastingGlucoseValueTv.text = "${recordItem.fastingGlucose} mg/dL"
                binding.totalUfValueTv.text = "${recordItem.totalUf}g"
                binding.noteContentTv.text = recordItem.notes ?: ""

                adapter.updateList(recordItem.exchanges)
                binding.exchangeDetailRv.visibility =
                    if (recordItem.exchanges.isEmpty()) View.GONE else View.VISIBLE
            } else {
                selectedRecordId = null
                hideDetailViews()
                clearRecordViews()
                adapter.updateList(emptyList())
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            Log.e("RECORD_LIST_FRAGMENT", errorMsg.toString())
        }
    }

    private fun clearRecordViews() {

        // 건강 정보 카드 초기화
        binding.weightValueTv.text = "-kg"
        binding.bloodPressureValueTv.text = "-/-"
        binding.fastingGlucoseValueTv.text = "- mg/dL"
        binding.totalUfValueTv.text = "-g"

        // 비고 내용 초기화
        binding.noteContentTv.text = ""
    }

    private fun hideDetailViews() {
        with(binding) {
            selectedDateInfoContainer.visibility = View.GONE
            healthInfoCv.visibility = View.GONE
            sessionDetailTitleTv.visibility = View.GONE
            exchangeDetailRv.visibility = View.GONE
            noteCv.visibility = View.GONE
            detailEditBtn.visibility = View.GONE
            deleteBtn.visibility = View.GONE
        }
    }

    private fun showDetailViews() {
        with(binding) {
            selectedDateInfoContainer.visibility = View.VISIBLE
            healthInfoCv.visibility = View.VISIBLE
            sessionDetailTitleTv.visibility = View.VISIBLE
            // exchangeDetailRv는 회차 없으면 GONE 처리하니까 여기서 VISIBLE 해놔도 됨
            exchangeDetailRv.visibility = View.VISIBLE
            noteCv.visibility = View.VISIBLE
            detailEditBtn.visibility = View.VISIBLE
            deleteBtn.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}