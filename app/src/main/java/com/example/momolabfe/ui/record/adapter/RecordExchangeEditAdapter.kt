package com.example.momolabfe.ui.record.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.databinding.ItemExchangeEditBinding
import com.example.momolabfe.remote.record.model.RecordExchangeGetResponse
import java.time.format.DateTimeFormatter
import java.util.Locale

class RecordExchangeEditAdapter(
    var items: MutableList<RecordExchangeGetResponse> = mutableListOf()
) : RecyclerView.Adapter<RecordExchangeEditAdapter.ViewHolder>() {

    interface OnTimePickerClickListener {
        fun onTimePickerClick(position: Int, targetEditText: EditText)
    }

    var onTimePickerClickListener: OnTimePickerClickListener? = null

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        val binding: ItemExchangeEditBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var isBinding = false

        init {
            // 시간 클릭 시 타임피커 호출
            binding.exchangeTimeValueEt.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onTimePickerClickListener?.onTimePickerClick(pos, binding.exchangeTimeValueEt)
                }
            }

            binding.drainVolumeValueEt.doAfterTextChanged { text ->
                if (isBinding) return@doAfterTextChanged
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@doAfterTextChanged

                val value = text?.toString()?.toIntOrNull() ?: 0
                items[pos] = items[pos].copy(drainVolume = value)
            }

            binding.fillVolumeValueEt.doAfterTextChanged { text ->
                if (isBinding) return@doAfterTextChanged
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@doAfterTextChanged

                val value = text?.toString()?.toIntOrNull() ?: 0
                items[pos] = items[pos].copy(fillVolume = value)
            }

            binding.fillConcentrationValueEt.doAfterTextChanged { text ->
                if (isBinding) return@doAfterTextChanged
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@doAfterTextChanged

                val value = text?.toString()?.toDoubleOrNull() ?: 0.0
                items[pos] = items[pos].copy(fillConcentration = value)
            }

            binding.ufValueEt.doAfterTextChanged { text ->
                if (isBinding) return@doAfterTextChanged
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@doAfterTextChanged

                val value = text?.toString()?.toIntOrNull() ?: 0
                items[pos] = items[pos].copy(uf = value)
            }
        }

        fun bind(item: RecordExchangeGetResponse) {
            isBinding = true

            binding.exchangeTitleTv.text = "${item.exchangeNo}회차"

            binding.exchangeTimeValueEt.setText(
                item.exchangeTime.format(TIME_FORMATTER)
            )

            // 0이면 빈칸으로 표시
            binding.drainVolumeValueEt.setText(
                if (item.drainVolume == 0) "" else item.drainVolume.toString()
            )
            binding.fillVolumeValueEt.setText(
                if (item.fillVolume == 0) "" else item.fillVolume.toString()
            )
            binding.fillConcentrationValueEt.setText(
                if (item.fillConcentration == 0.0) "" else item.fillConcentration.toString()
            )
            binding.ufValueEt.setText(
                if (item.uf == 0) "" else item.uf.toString()
            )

            isBinding = false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExchangeEditBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun updateList(newList: List<RecordExchangeGetResponse>) {
        items = newList.toMutableList()
        notifyDataSetChanged()
    }

    companion object {
        val TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)
    }
}