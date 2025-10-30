package com.example.momolabfe.ui.record.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.R
import com.example.momolabfe.ui.record.data.RecordExchangeInfo

class ExchangeInfoAdapter(
    private val items: List<RecordExchangeInfo>
) : RecyclerView.Adapter<ExchangeInfoAdapter.ExchangeInfoVH>() {
    inner class ExchangeInfoVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val exchangeNoTv: TextView = itemView.findViewById(R.id.exchange_no_tv)
        private val exchangeTimeTv: TextView = itemView.findViewById(R.id.exchange_time_tv)
        private val addIv: ImageView = itemView.findViewById(R.id.add_iv)

        fun bind(position: Int) {
            val isAddCell = position == items.size
            if (isAddCell) {
                exchangeNoTv.visibility = View.GONE
                exchangeTimeTv.visibility = View.GONE
                addIv.visibility = View.VISIBLE
            } else {
                val item = items[position]
                exchangeNoTv.text = "${item.exchangeNo}회차"
                exchangeTimeTv.text = item.exchangeTimeText
                exchangeNoTv.visibility = View.VISIBLE
                exchangeTimeTv.visibility = View.VISIBLE
                addIv.visibility = View.GONE
                itemView.setOnClickListener(null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeInfoVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record_exchange_info, parent, false)
        return ExchangeInfoVH(view)
    }

    override fun onBindViewHolder(holder: ExchangeInfoVH, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = items.size + 1
}