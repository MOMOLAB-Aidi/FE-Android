package com.example.momolabfe.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.momolabfe.R
import com.example.momolabfe.databinding.BottomSheetSelectMethodBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetSelectMethodFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSelectMethodBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSelectMethodBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cameraBtn.setOnClickListener {
            dismiss()
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, CameraFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.albumBtn.setOnClickListener {
            dismiss()
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, AlbumFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}