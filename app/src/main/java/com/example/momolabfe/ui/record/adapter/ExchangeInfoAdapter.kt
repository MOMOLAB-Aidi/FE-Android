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
    private val items: MutableList<RecordExchangeInfo> = mutableListOf()
) : RecyclerView.Adapter<ExchangeInfoAdapter.ExchangeInfoVH>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_ADD = 1
    }

    inner class ExchangeInfoVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val exchangeNoTv: TextView = itemView.findViewById(R.id.exchange_no_tv)
        private val exchangeTimeTv: TextView = itemView.findViewById(R.id.exchange_time_tv)
        private val addIv: ImageView = itemView.findViewById(R.id.add_iv)

        fun bindItem(item: RecordExchangeInfo) {
            exchangeNoTv.text = "${item.exchangeNo}회차"
            exchangeTimeTv.text = item.exchangeTimeText
            exchangeNoTv.visibility = View.VISIBLE
            exchangeTimeTv.visibility = View.VISIBLE
            addIv.visibility = View.GONE
        }

        fun bindAdd() {
            exchangeNoTv.visibility = View.GONE
            exchangeTimeTv.visibility = View.GONE
            addIv.visibility = View.VISIBLE
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == items.size) VIEW_TYPE_ADD else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeInfoVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record_exchange_info, parent, false)
        return ExchangeInfoVH(view)
    }

    override fun onBindViewHolder(holder: ExchangeInfoVH, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_ADD) {
            holder.bindAdd()
        } else {
            holder.bindItem(items[position])
        }
    }

    override fun getItemCount(): Int = items.size + 1

    // 리스트 갱신용
    fun setItems(newItems: List<RecordExchangeInfo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}