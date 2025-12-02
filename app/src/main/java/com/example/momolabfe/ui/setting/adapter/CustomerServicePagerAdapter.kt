package com.example.momolabfe.ui.setting.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.momolabfe.ui.setting.CustomerEmergencyFragment
import com.example.momolabfe.ui.setting.CustomerFaqFragment
import com.example.momolabfe.ui.setting.CustomerInquiryFragment

class CustomerServicePagerAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CustomerInquiryFragment()
            1 -> CustomerFaqFragment()
            else -> CustomerEmergencyFragment()
        }
    }
}