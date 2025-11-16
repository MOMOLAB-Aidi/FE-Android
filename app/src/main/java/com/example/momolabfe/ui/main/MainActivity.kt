package com.example.momolabfe.ui.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.momolabfe.R
import com.example.momolabfe.databinding.ActivityMainBinding
import com.example.momolabfe.ui.auth.LoginFragment
import com.example.momolabfe.ui.consult.ConsultFragment
import com.example.momolabfe.ui.record.RecordFragment
import com.example.momolabfe.ui.statistics.StatisticsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

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
}