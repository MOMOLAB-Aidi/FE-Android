package com.example.momolabfe.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentCustomerFaqBinding

class CustomerFaqFragment : Fragment() {

    private var _binding: FragmentCustomerFaqBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerFaqBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "고객센터 문의하기" 버튼 눌렀을 때 0번 탭으로 이동
        binding.goInquiryButton.setOnClickListener {
            (parentFragment as? CustomerServiceFragment)?.let { parent ->
                parent.view?.findViewById<androidx.viewpager2.widget.ViewPager2>(
                    R.id.customer_service_vp
                )?.currentItem = 0
            }
        }

        // FAQ 토글 설정
        setupFaqToggle(
            questionLayout = binding.faq1QuestionLayout,
            arrowView = binding.faq1ArrowIv,
            answerView = binding.faq1AnswerTv
        )
        setupFaqToggle(
            questionLayout = binding.faq2QuestionLayout,
            arrowView = binding.faq2ArrowIv,
            answerView = binding.faq2AnswerTv
        )
        setupFaqToggle(
            questionLayout = binding.faq3QuestionLayout,
            arrowView = binding.faq3ArrowIv,
            answerView = binding.faq3AnswerTv
        )
        setupFaqToggle(
            questionLayout = binding.faq4QuestionLayout,
            arrowView = binding.faq4ArrowIv,
            answerView = binding.faq4AnswerTv
        )
        setupFaqToggle(
            questionLayout = binding.faq5QuestionLayout,
            arrowView = binding.faq5ArrowIv,
            answerView = binding.faq5AnswerTv
        )
        setupFaqToggle(
            questionLayout = binding.faq6QuestionLayout,
            arrowView = binding.faq6ArrowIv,
            answerView = binding.faq6AnswerTv
        )
        setupFaqToggle(
            questionLayout = binding.faq7QuestionLayout,
            arrowView = binding.faq7ArrowIv,
            answerView = binding.faq7AnswerTv
        )
    }

    // FAQ 한 카드에 대한 펼치기/접기 공통 함수
    private fun setupFaqToggle(
        questionLayout: View,
        arrowView: ImageView,
        answerView: View
    ) {
        var expanded = false

        val toggleListener = View.OnClickListener {
            expanded = !expanded

            // 답변 영역 show/hide
            answerView.visibility = if (expanded) View.VISIBLE else View.GONE

            // 화살표 회전 애니메이션
            arrowView.animate()
                .rotation(if (expanded) 180f else 0f)
                .setDuration(150L)
                .start()
        }

        // 질문 영역/화살표 둘 다 눌렀을 때 동작
        questionLayout.setOnClickListener(toggleListener)
        arrowView.setOnClickListener(toggleListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}