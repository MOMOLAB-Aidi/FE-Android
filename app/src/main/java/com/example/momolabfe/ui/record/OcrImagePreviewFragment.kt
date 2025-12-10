package com.example.momolabfe.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentOcrImagePreviewBinding

class OcrImagePreviewFragment : Fragment() {

    private var _binding: FragmentOcrImagePreviewBinding? = null
    private val binding get() = _binding!!

    private val imageUrl: String? by lazy {
        arguments?.getString(ARG_IMAGE_URL)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOcrImagePreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (imageUrl.isNullOrBlank()) {
            // URL 없으면 그냥 뒤로
            parentFragmentManager.popBackStack()
            return
        }

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_thumbnail_sv)
            .error(R.drawable.ic_thumbnail_sv)
            .into(binding.ocrPhotoView)

        binding.closeIv.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    companion object {
        private const val ARG_IMAGE_URL = "image_url"

        fun newInstance(url: String): OcrImagePreviewFragment {
            return OcrImagePreviewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_URL, url)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}