package com.example.momolabfe.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.momolabfe.databinding.FragmentCustomerServiceBinding
import com.example.momolabfe.ui.setting.adapter.CustomerServicePagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class CustomerServiceFragment : Fragment() {

    private var _binding: FragmentCustomerServiceBinding? = null
    private val binding get() = _binding!!

    private lateinit var pagerAdapter: CustomerServicePagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerServiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pagerAdapter = CustomerServicePagerAdapter(this)
        binding.customerServiceVp.adapter = pagerAdapter
        binding.customerServiceVp.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.customerServiceVp.offscreenPageLimit = 3

        val titles = listOf("문의하기", "자주 묻는 질문", "긴급 연락처")

        TabLayoutMediator(
            binding.customerServiceTabLayout,
            binding.customerServiceVp
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = titles[position]
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}