package com.example.momolabfe.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.momolabfe.databinding.FragmentRecordExchangeListBinding
import com.example.momolabfe.ui.record.adapter.ExchangeInfoAdapter
import com.example.momolabfe.ui.record.data.RecordExchangeInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RecordExchangeListFragment : Fragment() {

    private var _binding: FragmentRecordExchangeListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordExchangeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 전달된 날짜 문자열 꺼내기 (기본값: 오늘 날짜 포맷 or 빈 문자열)
        val dateText = arguments?.getString("selected_date_text")
            ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))

        binding.dateTv.text = dateText

        setupRecycler()
    }

    private fun setupRecycler() {
        binding.exchangeRv.layoutManager = GridLayoutManager(requireContext(), 2)

        val dummy = createDummyExchangeList()
        binding.exchangeRv.adapter = ExchangeInfoAdapter(dummy)
    }

    private fun createDummyExchangeList(): List<RecordExchangeInfo> {
        return listOf(
            RecordExchangeInfo(exchangeNo = 1, exchangeTimeText = "오전 6시"),
            RecordExchangeInfo(exchangeNo = 2, exchangeTimeText = "오전 11시 30분"),
            RecordExchangeInfo(exchangeNo = 3, exchangeTimeText = "오후 5시"),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}