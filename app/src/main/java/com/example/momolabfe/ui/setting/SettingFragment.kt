package com.example.momolabfe.ui.setting

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentSettingBinding
import com.example.momolabfe.remote.auth.LogoutManager
import com.example.momolabfe.ui.setting.viewModel.SettingViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
    private val LAST_LOGIN_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm:ss")

    private val viewModel: SettingViewModel by activityViewModels()

    @Inject
    lateinit var logoutManager: LogoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        viewModel.getMyPage()
        observeMyPageResult()

        binding.recordCreateTitleTv.setOnClickListener { navigateToAlarmSetting() }
        binding.recordCreateContentTv.setOnClickListener { navigateToAlarmSetting() }
        binding.recordTimeTitleTv.setOnClickListener { navigateToAlarmSetting() }
        binding.recordTimeContentTv.setOnClickListener { navigateToAlarmSetting() }

        binding.changePasswordTitleTv.setOnClickListener { navigateToPassword() }
        binding.changePasswordContentTv.setOnClickListener { navigateToPassword() }

        binding.controlDataTitleTv.setOnClickListener { navigateToPersonalInformationPolicy() }
        binding.controlDataContentTv.setOnClickListener { navigateToPersonalInformationPolicy() }

        binding.agreementsTitleTv.setOnClickListener { navigateToTermsOfService() }
        binding.agreementsContentTv.setOnClickListener { navigateToTermsOfService() }

        binding.contactTitleTv.setOnClickListener { navigateToCustomerService() }
        binding.contactContentTv.setOnClickListener { navigateToCustomerService() }

        binding.logoutBtn.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                logoutManager.logout()
            }
        }
    }

    private fun navigateToAlarmSetting() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frm, AlarmSettingFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToPassword() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frm, PasswordFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToPersonalInformationPolicy() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frm, PersonalInformationPolicyFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToTermsOfService() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frm, TermsOfServiceFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToCustomerService() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frm, CustomerServiceFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun observeMyPageResult() {
        viewModel.getPageResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                binding.loginIdTv.text = it.loginId
                binding.recordStartDateTv.text = it.recordStartDate.format(DATE_FORMATTER)
                binding.recordPeriodTv.text = "(${it.recordPeriod})"

                val formattedLastLogin = it.lastLoginAt.let { raw ->
                    runCatching {
                        val trimmed = raw.substringBefore('.')
                        val ldt = LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        ldt.format(LAST_LOGIN_FORMATTER)
                    }.getOrElse { _ ->
                        raw // 혹은 null 반환 후 "-" 등 기본 문구 사용
                    }
                }
                binding.lastLoginDataTv.text = formattedLastLogin ?: "-"
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Log.e("MyPage", "마이페이지 조회 실패: $it")
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}