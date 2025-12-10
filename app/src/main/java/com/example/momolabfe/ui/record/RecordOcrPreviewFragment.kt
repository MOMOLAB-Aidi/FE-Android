package com.example.momolabfe.ui.record

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordOcrPreviewBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class RecordOcrPreviewFragment : Fragment() {

    private var _binding: FragmentRecordOcrPreviewBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_IMAGE_URI)?.let { uriString ->
            imageUri = Uri.parse(uriString)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordOcrPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        // 이미지 표시
        imageUri?.let { uri ->
            Glide.with(this)
                .load(uri)
                .into(binding.previewIv)
        }

        // 닫기 버튼
        binding.closeIv.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 다시 찍기 버튼
        binding.retakeBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 사용하기 버튼
        binding.useBtn.setOnClickListener {
            imageUri?.let { uri ->
                val fragment = RecordOcrLoadingFragment.newInstance(uri.toString())
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_frm, fragment)
                    .commit()
            }
        }
    }

    companion object {
        private const val ARG_IMAGE_URI = "imageUri"

        fun newInstance(imageUri: String) = RecordOcrPreviewFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_IMAGE_URI, imageUri)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}