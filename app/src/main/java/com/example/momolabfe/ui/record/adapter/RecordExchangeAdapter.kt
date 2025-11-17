package com.example.momolabfe.ui.record.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.data.remote.record.model.RecordExchangeOcrResponse
import com.example.momolabfe.databinding.ItemRecordExchangeBinding
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class RecordExchangeAdapter :
    ListAdapter<RecordExchangeOcrResponse, RecordExchangeAdapter.ExchangeViewHolder>(DIFF_CALLBACK) {

    inner class ExchangeViewHolder(
        private val binding: ItemRecordExchangeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecordExchangeOcrResponse) {
            binding.exchangeNoTv.text = "${item.exchangeNo}회차"

            // LocalTime → "HH:mm" 표시
            binding.exchangeTimeEt.setText(formatTime(item.exchangeTime))

            binding.drainVolumeEt.setText(item.drainVolume.toString())
            binding.fillConcentrationEt.setText(item.fillConcentration.toString())
            binding.fillVolumeEt.setText(item.fillVolume.toString())
            binding.ufEt.setText(item.uf.toString())
        }

        private fun formatTime(time: LocalTime): String {
            val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)
            return time.format(formatter)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRecordExchangeBinding.inflate(inflater, parent, false)
        return ExchangeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExchangeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RecordExchangeOcrResponse>() {
            override fun areItemsTheSame(oldItem: RecordExchangeOcrResponse, newItem: RecordExchangeOcrResponse) =
                oldItem.exchangeNo == newItem.exchangeNo

            override fun areContentsTheSame(oldItem: RecordExchangeOcrResponse, newItem: RecordExchangeOcrResponse) =
                oldItem == newItem
        }
    }
}