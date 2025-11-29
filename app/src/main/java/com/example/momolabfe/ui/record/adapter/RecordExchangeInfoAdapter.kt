package com.example.momolabfe.ui.record.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.R
import com.example.momolabfe.databinding.ItemExchangeInfoBinding
import com.example.momolabfe.remote.record.model.RecordExchangeGetResponse

class RecordExchangeInfoAdapter(
    private var exchangeList: List<RecordExchangeGetResponse>,
    private val highlightMismatch: Boolean = false // 색상 강조 여부
) : RecyclerView.Adapter<RecordExchangeInfoAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemExchangeInfoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemExchangeInfoBinding =
            ItemExchangeInfoBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = exchangeList[position]
        val b = holder.binding
        b.exchangeTitleTv.text = item.exchangeNo.toString()
        b.exchangeTimeValueTv.text = item.exchangeTime.toString()
        b.fillConcentrationValueTv.text = item.fillConcentration.toString()
        b.drainVolumeValueTv.text = item.drainVolume.toString()
        b.fillVolumeValueTv.text = item.fillVolume.toString()

        // 제수량 색상 설정
        val context = b.root.context
        val normalColor = ContextCompat.getColor(context, R.color.text_primary)
        val errorColor = ContextCompat.getColor(context, R.color.red)

        val uf = item.uf // 서버에서 온 제수량
        val calculatedUf = item.drainVolume - item.fillVolume // (배액량 - 주입량)

        b.ufValueTv.text = uf.toString()

        // 상세 조회일 때만 빨간색 강조
        val color =
            if (highlightMismatch && uf != null && uf != calculatedUf) errorColor
            else normalColor
        b.ufValueTv.setTextColor(color)
    }

    override fun getItemCount(): Int = exchangeList.size

    fun getItems(): List<RecordExchangeGetResponse> = exchangeList

    fun updateList(newList: List<RecordExchangeGetResponse>) {
        exchangeList = newList
        notifyDataSetChanged()
    }
}