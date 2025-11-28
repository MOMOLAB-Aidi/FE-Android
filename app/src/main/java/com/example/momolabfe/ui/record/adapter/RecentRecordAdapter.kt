package com.example.momolabfe.ui.record.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.R
import com.example.momolabfe.databinding.ItemRecentRecordBinding
import com.example.momolabfe.remote.record.model.RecordGetResponse
import com.example.momolabfe.ui.record.RecordInfoFragment
import java.util.Locale

class RecentRecordAdapter(
    private val fragmentManager: FragmentManager,
    private var recordList: List<RecordGetResponse>
) : RecyclerView.Adapter<RecentRecordAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRecentRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemRecentRecordBinding =
            ItemRecentRecordBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = recordList[position]
        val b = holder.binding
        b.dateTv.text = item.recordDate.toString()
        b.weightTv.text = item.weight.let { weight ->
            String.format(Locale.KOREA, "%.1fkg", weight)
        }

        b.totalUfTv.text = item.totalUf.let { uf ->
            String.format(Locale.KOREA, "%dg", uf)
        }

        val exchangeCount = item.exchanges.size
        b.completeTv.text = "${exchangeCount}회차 완료"

        b.root.setOnClickListener {
            val args = Bundle().apply {
                putLong("record_id", item.id)
            }

            val recordInfoFragment = RecordInfoFragment().apply {
                arguments = args
            }

            fragmentManager.beginTransaction()
                .replace(R.id.main_frm, recordInfoFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun getItemCount(): Int = recordList.size

    fun updateList(newList: List<RecordGetResponse>) {
        recordList = newList
        notifyDataSetChanged()
    }
}