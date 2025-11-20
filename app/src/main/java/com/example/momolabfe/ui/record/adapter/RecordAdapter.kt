package com.example.momolabfe.ui.record.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.R
import com.example.momolabfe.databinding.ItemRecordListBinding
import com.example.momolabfe.remote.record.model.RecordGetResponse
import com.example.momolabfe.ui.record.RecordInfoFragment

class RecordAdapter(
    private val fragmentManager: FragmentManager,
    private var recordList: List<RecordGetResponse>
) : RecyclerView.Adapter<RecordAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRecordListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemRecordListBinding =
            ItemRecordListBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = recordList[position]
        val b = holder.binding
        b.dateTv.text = item.recordDate.toString()
        b.weightContentTv.text = item.weight.toString()
        b.systolicTv.text = item.systolic.toString()
        b.diastolicTv.text = item.diastolic.toString()
        b.totalUfContentTv.text = item.totalUf.toString()

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