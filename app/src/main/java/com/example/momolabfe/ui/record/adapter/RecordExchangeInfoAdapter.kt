package com.example.momolabfe.ui.record.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.databinding.ItemExchangeInfoBinding
import com.example.momolabfe.remote.record.data.RecordExchangeGetResponse

class RecordExchangeInfoAdapter(
    private var exchangeList: List<RecordExchangeGetResponse>
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
        b.ufValueTv.text = item.uf.toString()
    }

    override fun getItemCount(): Int = exchangeList.size

    fun getItems(): List<RecordExchangeGetResponse> = exchangeList

    fun updateList(newList: List<RecordExchangeGetResponse>) {
        exchangeList = newList
        notifyDataSetChanged()
    }
}