package com.example.momolabfe.ui.main

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.momolabfe.R
import com.example.momolabfe.databinding.ActivityMainBinding
import com.example.momolabfe.ui.auth.LoginFragment
import com.example.momolabfe.data.remote.auth.LogoutManager
import com.example.momolabfe.ui.consult.ConsultFragment
import com.example.momolabfe.ui.record.RecordFragment
import com.example.momolabfe.ui.statistics.StatisticsFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var logoutManager: LogoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initBottomNavigation()

        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainBnv) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bottom)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_frm, LoginFragment())
                .commit()
        }

        observeLogoutEvent()
    }

    private fun initBottomNavigation() {

        binding.mainBnv.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.fragment_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_frm, HomeFragment())
                        .commitAllowingStateLoss()
                    return@setOnItemSelectedListener true
                }

                R.id.fragment_record -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_frm, RecordFragment())
                        .commitAllowingStateLoss()
                    return@setOnItemSelectedListener true
                }

                R.id.fragment_consult -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_frm, ConsultFragment())
                        .commitAllowingStateLoss()
                    return@setOnItemSelectedListener true
                }

                R.id.fragment_statistics -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_frm, StatisticsFragment())
                        .commitAllowingStateLoss()
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
    }

    private fun observeLogoutEvent() {
        // LifecycleScope를 사용하여 액티비티 라이프사이클에 맞춰 관찰
        lifecycleScope.launch {
            logoutManager.logoutEvent.collect {
                // 로그아웃 이벤트 발생 시 LoginFragment로 이동
                navigateToLoginFragment()
            }
        }
    }

    private fun navigateToLoginFragment() {
        // 바텀 네비게이션 숨김 처리 (LoginFragment에서 할 수도 있지만 여기서 명확히 처리)
        binding.mainBnv.visibility = View.GONE

        // LoginFragment로 교체
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frm, LoginFragment())
            // Back Stack의 모든 내용을 제거하여 뒤로가기 버튼으로 홈 화면에 돌아가지 못하게 함
            .commitNow()
    }
}