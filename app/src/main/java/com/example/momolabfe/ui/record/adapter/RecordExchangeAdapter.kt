package com.example.momolabfe.ui.record.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.R
import com.example.momolabfe.remote.record.model.OcrRecordExchangeData
import com.example.momolabfe.databinding.ItemRecordExchangeBinding
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class RecordExchangeAdapter :
    RecyclerView.Adapter<RecordExchangeAdapter.ExchangeViewHolder>() {

    // 항목의 확장/축소 상태를 저장하는 리스트
    private var isExpandedList: MutableList<Boolean> = mutableListOf()

    var items: MutableList<OcrRecordExchangeData> = mutableListOf()
        set(value) {
            field = value.toMutableList()
            isExpandedList = MutableList(value.size) { true } // 항목 리스트 크기에 맞춰 확장 상태 리스트 업데이트
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

        // 프로그램적으로 setText() 할 때 TextWatcher가 반응하지 않도록 막는 플래그
        private var isBinding = false

        init {
            val clickListener = View.OnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTimePickerClickListener?.onTimePickerClick(position, binding.exchangeTimeEt)
                }
            }

            binding.exchangeTimeCv.setOnClickListener(clickListener)
            binding.exchangeTimeEt.setOnClickListener(clickListener)

            // 확장/축소 클릭 리스너
            binding.dropdownIv.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // 상태 토글
                    val isExpanded = isExpandedList[position]
                    isExpandedList[position] = !isExpanded

                    // 해당 항목만 갱신 (애니메이션 없이 뷰 변경)
                    notifyItemChanged(position)
                }
            }

            // TextWatcher 들은 init 블록에서 한 번만 등록
            binding.drainVolumeEt.doAfterTextChanged { text ->
                if (isBinding) return@doAfterTextChanged
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@doAfterTextChanged

                val value = text?.toString()?.toIntOrNull() ?: 0
                items[pos] = items[pos].copy(drainVolume = value)
            }

            binding.fillVolumeEt.doAfterTextChanged { text ->
                if (isBinding) return@doAfterTextChanged
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@doAfterTextChanged

                val value = text?.toString()?.toIntOrNull() ?: 0
                items[pos] = items[pos].copy(fillVolume = value)
            }

            binding.fillConcentrationEt.doAfterTextChanged { text ->
                if (isBinding) return@doAfterTextChanged
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@doAfterTextChanged

                val value = text?.toString()?.toDoubleOrNull() ?: 0.0
                items[pos] = items[pos].copy(fillConcentration = value)
            }

            binding.ufEt.doAfterTextChanged { text ->
                if (isBinding) return@doAfterTextChanged
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@doAfterTextChanged

                val value = text?.toString()?.toIntOrNull() ?: 0
                items[pos] = items[pos].copy(uf = value)
            }
        }

        fun bind(item: OcrRecordExchangeData) {

            // TextWatcher 트리거를 막기 위해 true
            isBinding = true

            binding.exchangeNoTv.text = "${item.exchangeNo}회차"

            val timeText = formatTime(item.exchangeTime)
            binding.exchangeTimeEt.setText(timeText)

            // 값이 0일 경우 빈 문자열("")로 표시
            binding.drainVolumeEt.setText(if (item.drainVolume == 0) "" else item.drainVolume.toString())
            binding.fillConcentrationEt.setText(if (item.fillConcentration == 0.0) "" else item.fillConcentration.toString())
            binding.fillVolumeEt.setText(if (item.fillVolume == 0) "" else item.fillVolume.toString())
            binding.ufEt.setText(if (item.uf == 0) "" else item.uf.toString())

            updateExpansionState(absoluteAdapterPosition)

            // 바인딩 끝났으니 다시 false
            isBinding = false
        }

        private fun updateExpansionState(position: Int) {
            if (position == RecyclerView.NO_POSITION || position >= isExpandedList.size) return

            val isExpanded = isExpandedList[position]

            // 상세 정보 컨테이너의 가시성 변경
            val detailsContainer = binding.root.findViewById<View>(R.id.details_container)
            detailsContainer?.visibility = if (isExpanded) View.VISIBLE else View.GONE

            binding.dropdownIv.setImageResource(
                if (isExpanded) R.drawable.ic_arrow_up_sv else R.drawable.ic_arrow_down_sv
            )
        }

        private fun formatTime(time: LocalTime): String {
            return time.format(TIME_FORMATTER)
        }
    }

    // 새로운 항목을 추가하고 확장 상태를 동기화
    fun addExchangeItem(item: OcrRecordExchangeData) {
        val newPosition = items.size

        items.add(item)
        isExpandedList.add(true)
        notifyItemInserted(newPosition)
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