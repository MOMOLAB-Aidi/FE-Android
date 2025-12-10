package com.example.momolabfe.ui.record

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordOcrLoadingBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.momolabfe.ui.record.data.LoadingStep
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import kotlinx.coroutines.isActive

class RecordOcrLoadingFragment : Fragment() {

    private var _binding: FragmentRecordOcrLoadingBinding? = null
    private val binding get() = _binding!!

    private var imageUri: String? = null

    private val loadingSteps = listOf(
        LoadingStep(0, "AI가 기록을 읽고 있어요", "이미지 분석 중.."),
        LoadingStep(20, "AI가 기록을 읽고 있어요", "이미지 전처리 중.."),
        LoadingStep(40, "AI가 기록을 읽고 있어요", "텍스트 인식 중.."),
        LoadingStep(60, "AI가 기록을 읽고 있어요", "숫자 추출 중.."),
        LoadingStep(80, "AI가 기록을 읽고 있어요", "데이터 정리 중.."),
        LoadingStep(100, "인식 완료!", "데이터를 불러오는 중..")
    )

    private val viewModel: RecordViewModel by activityViewModels()

    private var loadingJob: Job? = null
    private var navigated = false

    private var startTime: Long = 0L
    @Volatile private var isOcrFinished: Boolean = false

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

        // 로딩 시작 시각 기록
        startTime = System.currentTimeMillis()
        isOcrFinished = false
        navigated = false

        startLoadingAnimation()
        startOcrRequest()
        observeOcrResult()
    }

    private fun startLoadingAnimation() {
        loadingJob?.cancel()
        loadingJob = viewLifecycleOwner.lifecycleScope.launch {
            val maxIdxBeforeDone = loadingSteps.size - 3

            var idx = 0

            // api 호출 끝날때까지 대기
            while (isActive && !isOcrFinished) {
                val step = loadingSteps[idx]
                updateLoadingUI(step)

                delay(900L)

                if (idx < maxIdxBeforeDone) {
                    idx++
                } else {
                    idx = maxIdxBeforeDone
                }
            }

            if (!isActive) return@launch

            val elapsed = System.currentTimeMillis() - startTime

            // 전체 최소 노출 시간
            val minTotalDisplay = 5000L

            // 현재 progress 기준으로 아직 안 보여준 단계들 계산
            val currentProgress = binding.progressBar.progress
            val currentIdx = loadingSteps.indexOfFirst { it.progress == currentProgress }

            val startIdx = if (currentIdx == -1) 0 else currentIdx + 1
            val remainingSteps = if (startIdx < loadingSteps.size) {
                loadingSteps.subList(startIdx, loadingSteps.size)
            } else {
                emptyList()
            }

            if (remainingSteps.isEmpty()) return@launch

            // elapsed를 고려하여 남은 시간 계산
            val remainingDuration = maxOf(minTotalDisplay - elapsed, 800L)
            val perStepDelay = remainingDuration / remainingSteps.size

            for (step in remainingSteps) {
                if (!isActive) return@launch
                updateLoadingUI(step)
                delay(perStepDelay)
            }
        }
    }

    private fun updateLoadingUI(step: LoadingStep) {
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

    private fun startOcrRequest() {
        val uriStr = imageUri
        if (uriStr == null) {
            Toast.makeText(requireContext(), "이미지 경로가 없습니다.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        viewModel.recordByOcr(uriStr.toUri())
    }

    private fun observeOcrResult() {
        viewModel.ocrRecordResult.observe(viewLifecycleOwner) { result ->
            if (result != null && !navigated) {
                isOcrFinished = true  // ← 로딩 코루틴에 "끝났다" 신호

                viewLifecycleOwner.lifecycleScope.launch {
                    loadingJob?.join()

                    if (!navigated) {
                        navigated = true
                        navigateToNextScreen()
                    }
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank() && !navigated) {
                isOcrFinished = true
                navigated = true
                loadingJob?.cancel()

                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
                viewModel.clearError()
            }
        }
    }

    private fun navigateToNextScreen() {
        val bundle = Bundle().apply {
            putString("imageUri", imageUri)
            putBoolean("fromOcr", true)
        }

        val fragment = RecordWrite01Fragment()
        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frm, fragment)
            .commit()
    }

    companion object {
        private const val ARG_IMAGE_URI = "imageUri"

        fun newInstance(imageUri: String) = RecordOcrLoadingFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_IMAGE_URI, imageUri)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}