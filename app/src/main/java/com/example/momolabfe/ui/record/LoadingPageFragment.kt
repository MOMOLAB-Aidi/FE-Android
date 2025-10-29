package com.example.momolabfe.ui.record

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.momolabfe.databinding.FragmentLoadingPageBinding
import androidx.core.net.toUri

class LoadingPageFragment : Fragment() {

    private var _binding: FragmentLoadingPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoadingPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uriStr = arguments?.getString("image_uri")
        val uri = uriStr?.toUri()

        if (uri != null) {
            Glide.with(this)
                .load(uri)
                .fitCenter()
                .into(binding.previewIv)
        } else {
            // 전달 실패 시 플레이스홀더
            binding.previewIv.setImageResource(android.R.color.darker_gray)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}