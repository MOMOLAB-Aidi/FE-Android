package com.example.momolabfe.ui.record

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordOcrLoadingBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import com.example.momolabfe.ui.record.data.LoadingStep

class RecordOcrLoadingFragment : Fragment() {

    private var _binding: FragmentRecordOcrLoadingBinding? = null
    private val binding get() = _binding!!

    private var imageUri: String? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val loadingSteps = listOf(
        LoadingStep(0, "AI가 기록을 읽고 있어요", "이미지 분석 중.."),
        LoadingStep(20, "AI가 기록을 읽고 있어요", "이미지 전처리 중.."),
        LoadingStep(40, "AI가 기록을 읽고 있어요", "텍스트 인식 중.."),
        LoadingStep(60, "AI가 기록을 읽고 있어요", "숫자 추출 중.."),
        LoadingStep(80, "AI가 기록을 읽고 있어요", "데이터 정리 중.."),
        LoadingStep(100, "인식 완료!", "데이터를 불러오는 중..")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageUri = it.getString(ARG_IMAGE_URI)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordOcrLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        // 이미지 로드
        imageUri?.let { uri ->
            try {
                Glide.with(this)
                    .load(uri.toUri())
                    .centerCrop()
                    .into(binding.previewImageIv)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        startLoadingAnimation()
        performOcrProcess()
    }

    private fun startLoadingAnimation() {
        scope.launch {
            for ((index, step) in loadingSteps.withIndex()) {
                updateLoadingUI(step)

                // 각 단계별 대기 시간 (총 8초)
                val delayTime = if (index < loadingSteps.size - 1) {
                    1600L // 0->20->40->60->80% 각각 1.6초
                } else {
                    800L // 80->100% 0.8초
                }

                delay(delayTime)
            }

            // 로딩 완료 후 다음 화면으로 이동
            delay(500)
            navigateToNextScreen()
        }
    }

    private fun updateLoadingUI(step: LoadingStep) {
        // 텍스트 업데이트
        binding.loadingTitleTv.text = step.title
        binding.loadingSubtitleTv.text = step.subtitle
        binding.progressPercentTv.text = "${step.progress}%"

        // 프로그레스바 애니메이션
        ObjectAnimator.ofInt(
            binding.progressBar,
            "progress",
            binding.progressBar.progress,
            step.progress
        ).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            start()
        }

        // 100% 완료 시 아이콘 변경
        if (step.progress == 100) {
            binding.loadingIconIv.setImageResource(R.drawable.ic_check_circle)
        }
    }

    private fun performOcrProcess() {
        // 실제 OCR API 호출은 여기서 수행
        scope.launch {
            try {
                // TODO: 실제 OCR API 호출
                // val result = ocrRepository.processImage(imageUri)

                // 8초 후 완료 (실제로는 API 응답을 받으면 완료)
                delay(8500)

            } catch (e: Exception) {
                // 에러 처리
                withContext(Dispatchers.Main) {
                    // Toast 또는 에러 화면 표시
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun navigateToNextScreen() {
        val bundle = Bundle().apply {
            putString("imageUri", imageUri)
            // TODO: OCR 결과 데이터도 함께 전달
        }

        val fragment = RecordWrite01Fragment()
        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frm, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
    }

    companion object {
        private const val ARG_IMAGE_URI = "imageUri"

        fun newInstance(imageUri: String) = RecordOcrLoadingFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_IMAGE_URI, imageUri)
            }
        }
    }
}