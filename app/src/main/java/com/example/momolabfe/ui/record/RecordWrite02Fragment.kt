package com.example.momolabfe.ui.record

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.NumberPicker
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.momolabfe.R
import com.example.momolabfe.databinding.DialogTimePickerBinding
import com.example.momolabfe.databinding.FragmentRecordWrite02Binding
import com.example.momolabfe.ui.record.adapter.RecordExchangeAdapter
import com.example.momolabfe.ui.record.viewModel.RecordViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class RecordWrite02Fragment : Fragment() {

    private var _binding: FragmentRecordWrite02Binding? = null
    private val binding get() = _binding!!

    private val viewModel: RecordViewModel by activityViewModels()
    private lateinit var exchangeAdapter: RecordExchangeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordWrite02Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        setupRecyclerView()
        fillExchangesFromOcr()

        binding.exchangeTimeCv.setOnClickListener {
            showTimePickerDialog()
        }

        binding.exchangeTimeEt.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun showTimePickerDialog() {
        val dialogBinding = DialogTimePickerBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
            .setView(dialogBinding.root)
            .create()

        setupPickersInDialog(dialogBinding)

        dialogBinding.applyTv.setOnClickListener {
            applySelectedTime(dialogBinding)
            dialog.dismiss()
        }

        dialogBinding.cancelTv.setOnClickListener {
            dialog.dismiss()
        }

        applyDialogWindow(dialog)
        dialog.show()
    }

    private fun setupPickersInDialog(dialogBinding: DialogTimePickerBinding) {
        dialogBinding.ampmPickerNp.apply {
            minValue = 0
            maxValue = 1
            displayedValues = arrayOf("오전", "오후")
            wrapSelectorWheel = false
            post { applyTextStyleToNumberPicker(this, requireContext()) }
        }

        dialogBinding.hourPickerNp.apply {
            minValue = 1
            maxValue = 12
            wrapSelectorWheel = true
            value = 12
            post { applyTextStyleToNumberPicker(this, requireContext()) }
        }

        dialogBinding.minutePickerNp.apply {
            minValue = 0
            maxValue = 5
            displayedValues = arrayOf("00", "10", "20", "30", "40", "50")
            wrapSelectorWheel = true
            post { applyTextStyleToNumberPicker(this, requireContext()) }
        }
    }

    private fun applySelectedTime(dialogBinding: DialogTimePickerBinding) {
        val ampmPicker = dialogBinding.ampmPickerNp
        val hourPicker = dialogBinding.hourPickerNp
        val minutePicker = dialogBinding.minutePickerNp

        val ampm = ampmPicker.value
        val hour12 = hourPicker.value
        val minute = arrayOf("00", "10", "20", "30", "40", "50")[minutePicker.value]

        // 화면 표시용 (12시간 형식)
        val displayTimeText = "${if (ampm == 0) "오전" else "오후"} $hour12:$minute"
        binding.exchangeTimeEt.setText(displayTimeText)

        // 서버 전송용 24시간 형식으로 변환
        val hour24 = convertTo24Hour(ampm, hour12)
        val serverTimeText = String.format(Locale.KOREA, "%02d:%s", hour24, minute)

        // ViewModel에 24시간 형식 저장 (서버 전송용)
        viewModel.setExchangeTime(serverTimeText)
    }

    private fun convertTo24Hour(ampm: Int, hour12: Int): Int {
        return when {
            ampm == 0 && hour12 == 12 -> 0  // 오전 12시 -> 00시
            ampm == 0 -> hour12              // 오전 1~11시 -> 1~11시
            ampm == 1 && hour12 == 12 -> 12 // 오후 12시 -> 12시
            else -> hour12 + 12              // 오후 1~11시 -> 13~23시
        }
    }

    private fun applyDialogWindow(dialog: AlertDialog) {
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnShowListener {
            dialog.window?.let { window ->
                val layoutParams = window.attributes
                layoutParams.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                layoutParams.gravity = Gravity.CENTER
                layoutParams.dimAmount = 0.5f
                window.attributes = layoutParams
                window.setDimAmount(0.5f)
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }
    }

    private fun applyTextStyleToNumberPicker(picker: NumberPicker, context: Context) {
        try {
            val count = picker.childCount
            for (i in 0 until count) {
                val child = picker.getChildAt(i)
                if (child is EditText) {
                    child.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    child.textSize = 15f
                    child.typeface = ResourcesCompat.getFont(context, R.font.noto_sans_kr_regular)
                    child.includeFontPadding = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        exchangeAdapter = RecordExchangeAdapter()
        binding.exchangeRv.apply {
            adapter = exchangeAdapter
        }
    }

    private fun fillExchangesFromOcr() {
        val ocr = viewModel.ocrRecordResult.value ?: return

        val exchanges = ocr.exchanges
        if (exchanges.isEmpty()) return

        // 1회차 → contentCv 내 EditText에 반영
        val first = exchanges.first()
        binding.apply {
            exchangeTimeEt.setText(first.exchangeTime.toString())
            drainVolumeEt.setText(first.drainVolume.toString())
            fillConcentrationEt.setText(first.fillConcentration.toString())
            fillVolumeEt.setText(first.fillVolume.toString())
            ufEt.setText(first.uf.toString())
        }

        // 2회차 이후 → RecyclerView에 반영
        val rest = exchanges.drop(1)
        exchangeAdapter.submitList(rest)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearOcr()
        _binding = null
    }
}