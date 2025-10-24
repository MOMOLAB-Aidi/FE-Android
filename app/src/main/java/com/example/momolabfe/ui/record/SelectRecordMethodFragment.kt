package com.example.momolabfe.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordSelectMethodBinding

class SelectRecordMethodFragment : Fragment() {

    private var _binding: FragmentRecordSelectMethodBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordSelectMethodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.manualEntryCv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, SelectRecordDateFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}