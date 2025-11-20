package com.example.momolabfe.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentHomeBinding
import com.example.momolabfe.ui.record.RecordFragment
import com.example.momolabfe.ui.record.RecordListFragment
import com.example.momolabfe.ui.record.adapter.RecentRecordAdapter
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.example.momolabfe.ui.setting.SettingFragment
import com.example.momolabfe.ui.statistics.StatisticsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RecentRecordAdapter
    private val viewModel: RecordViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.main_bnv)
        bottomNav?.visibility = View.VISIBLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RecentRecordAdapter(parentFragmentManager, emptyList())
        binding.recentRecordRv.adapter = adapter

        setupObservers()
        viewModel.getRecentRecords()

        try {
            // 날짜 포맷
            val now = Date()
            val format = SimpleDateFormat("yyyy년 MM월 dd일 EEEE", Locale.KOREA)

            val formattedDate = format.format(now)
            binding.contentTv.text = formattedDate

        } catch (e: Exception) {
            // 날짜 포맷팅 중 오류 발생 시 로그 출력
            e.printStackTrace()
        }

        binding.settingIv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, SettingFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.recordCv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, RecordFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.detailStatisticsBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, StatisticsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.totalTv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, RecordListFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupObservers() {
        viewModel.recordlistItems.observe(viewLifecycleOwner) { itemList ->
            adapter.updateList(itemList)

            if (itemList.isEmpty()) {
                binding.recentRecordRv.visibility = View.GONE
            } else {
                binding.recentRecordRv.visibility = View.VISIBLE
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            Log.e("HOME_FRAGMENT", errorMsg.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}