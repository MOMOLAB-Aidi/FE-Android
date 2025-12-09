package com.example.momolabfe.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentOnboardingBinding
import com.example.momolabfe.ui.main.adapter.OnboardingAdapter
import com.example.momolabfe.ui.main.data.OnboardingPage
import com.google.android.material.bottomnavigation.BottomNavigationView

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val pages by lazy {
        listOf(
            OnboardingPage(
                iconRes = R.drawable.ic_onboarding_home_sv,
                title = "환영합니다!",
                description = "복막투석 환자를 위한\n체계적인 건강 관리 앱입니다"
            ),
            OnboardingPage(
                iconRes = R.drawable.ic_onboarding_description_sv,
                title = "투석 기록 작성",
                description = "날짜, 체중, 혈압 등 공통 정보와\n회차별 투석 정보를 쉽게 기록하세요"
            ),
            OnboardingPage(
                iconRes = R.drawable.ic_onboarding_camera_sv,
                title = "사진으로 간편 입력",
                description = "수기 작성 또는 사진 인식을 통해\n더욱 편리하게 기록할 수 있습니다"
            ),
            OnboardingPage(
                iconRes = R.drawable.ic_onboarding_calendar_sv,
                title = "기록 조회",
                description = "캘린더에서 기록이 있는 날짜를 확인하고\n상세 정보를 한눈에 볼 수 있습니다"
            ),
            OnboardingPage(
                iconRes = R.drawable.ic_onboarding_graph_sv,
                title = "통계 분석",
                description = "체중, 혈압, 제수량 등의 변화 추이를\n그래프로 확인하세요"
            ),
            OnboardingPage(
                iconRes = R.drawable.ic_onboarding_chat_sv,
                title = "AI 건강 상담",
                description = "복막투석 관련 궁금한 점을\nAI 상담사와 대화하며 해결하세요"
            ),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        binding.viewPager.adapter = OnboardingAdapter(pages)
        binding.viewPager.isUserInputEnabled = true

        setupIndicators()
        setCurrentIndicator(0)
        setupListeners()
    }

    // 인디케이터 설정
    private fun setupIndicators() = with(binding) {
        indicatorContainer.removeAllViews()
        val indicators = Array(pages.size) { View(requireContext()) }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            val m = 6.dp
            leftMargin = m
            rightMargin = m
        }

        indicators.forEach { v ->
            v.setBackgroundResource(R.drawable.indicator_onboarding_inactive)
            v.layoutParams = params
            indicatorContainer.addView(v)
        }
    }

    private fun setCurrentIndicator(position: Int) = with(binding) {
        for (i in 0 until indicatorContainer.childCount) {
            val v = indicatorContainer.getChildAt(i)
            if (i == position) {
                v.setBackgroundResource(R.drawable.indicator_onboarding_active)
            } else {
                v.setBackgroundResource(R.drawable.indicator_onboarding_inactive)
            }
        }

        prevBtn.isEnabled = position != 0
        nextBtn.text = if (position == pages.lastIndex) "시작하기" else "다음"
    }

    private fun setupListeners() = with(binding) {

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setCurrentIndicator(position)
            }
        })

        prevBtn.setOnClickListener {
            val current = viewPager.currentItem
            if (current > 0) viewPager.currentItem = current - 1
        }

        nextBtn.setOnClickListener {
            val current = viewPager.currentItem
            if (current < pages.lastIndex) {
                viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        skipTv.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun finishOnboarding() {
//        val prefs = requireContext()
//            .getSharedPreferences("aidi_prefs", Context.MODE_PRIVATE)
//        prefs.edit().putBoolean("has_seen_onboarding", true).apply()

        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frm, HomeFragment())
            .commit()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}