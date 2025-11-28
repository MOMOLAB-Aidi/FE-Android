package com.example.momolabfe.ui.record.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.databinding.ItemExchangeDetailBinding
import com.example.momolabfe.remote.record.model.RecordExchangeGetResponse
import java.time.format.DateTimeFormatter
import java.util.Locale

class RecordExchangeDetailAdapter(
    private var exchangeList: List<RecordExchangeGetResponse>
) : RecyclerView.Adapter<RecordExchangeDetailAdapter.ViewHolder>() {

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)

    inner class ViewHolder(val binding: ItemExchangeDetailBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemExchangeDetailBinding =
            ItemExchangeDetailBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = exchangeList[position]
        val b = holder.binding

        b.exchangeNumberBadgeTv.text = item.exchangeNo.toString()
        b.exchangeTimeTv.text = item.exchangeTime.format(timeFormatter)
        b.ufValueTv.text = item.uf.let { uf -> "${uf}g" }

        val drainText = item.drainVolume.let { dv -> "배액량: ${dv}g" }
        val fillText = item.fillVolume.let { fv -> "주입량: ${fv}g" }
        b.volumeTv.text = "$drainText | $fillText"
    }

    override fun getItemCount(): Int = exchangeList.size

    fun updateList(newList: List<RecordExchangeGetResponse>) {
        exchangeList = newList
        notifyDataSetChanged()
    }
}