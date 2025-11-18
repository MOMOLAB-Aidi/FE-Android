package com.example.momolabfe.ui.record.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.data.remote.record.model.RecordExchangeOcrResponse
import com.example.momolabfe.databinding.ItemRecordExchangeBinding
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class RecordExchangeAdapter :
    RecyclerView.Adapter<RecordExchangeAdapter.ExchangeViewHolder>() {

    var items: MutableList<RecordExchangeOcrResponse> = mutableListOf()
        set(value) {
            field = value.toMutableList()
        }

    // TimePicker 클릭 이벤트 처리를 위한 인터페이스 정의
    interface OnTimePickerClickListener {
        fun onTimePickerClick(position: Int, targetEditText: EditText)
    }

    // 외부에서 리스너를 설정할 수 있는 속성
    var onTimePickerClickListener: OnTimePickerClickListener? = null

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ExchangeViewHolder(
        private val binding: ItemRecordExchangeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            val clickListener = View.OnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTimePickerClickListener?.onTimePickerClick(position, binding.exchangeTimeEt)
                }
            }

            binding.exchangeTimeCv.setOnClickListener(clickListener)
            binding.exchangeTimeEt.setOnClickListener(clickListener)
        }

        fun bind(item: RecordExchangeOcrResponse) {
            binding.exchangeNoTv.text = "${item.exchangeNo}회차"

            // LocalTime이 00:00이 아닐 때만 표시
            val timeText =
                if (item.exchangeTime == LocalTime.of(0, 0)) "" else formatTime(item.exchangeTime)
            binding.exchangeTimeEt.setText(timeText)

            // 값이 0일 경우 빈 문자열("")로 표시
            binding.drainVolumeEt.setText(if (item.drainVolume == 0) "" else item.drainVolume.toString())
            binding.fillConcentrationEt.setText(if (item.fillConcentration == 0.0) "" else item.fillConcentration.toString())
            binding.fillVolumeEt.setText(if (item.fillVolume == 0) "" else item.fillVolume.toString())
            binding.ufEt.setText(if (item.uf == 0) "" else item.uf.toString())
        }

        private fun formatTime(time: LocalTime): String {
            return time.format(TIME_FORMATTER)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRecordExchangeBinding.inflate(inflater, parent, false)
        return ExchangeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExchangeViewHolder, position: Int) {
        holder.bind(items[position])
    }

    companion object {
        val TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)

        const val MAX_EXCHANGES = 5
    }
}