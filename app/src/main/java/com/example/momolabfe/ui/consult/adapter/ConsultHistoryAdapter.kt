package com.example.momolabfe.ui.consult.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.databinding.ItemConsultHistoryBinding
import com.example.momolabfe.remote.consult.data.GetConsultResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ConsultHistoryAdapter(
    private val onClick: (GetConsultResponse) -> Unit = {},
    private val onDelete: (GetConsultResponse) -> Unit = {}
) : ListAdapter<GetConsultResponse, ConsultHistoryAdapter.HistoryViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<GetConsultResponse>() {
            override fun areItemsTheSame(oldItem: GetConsultResponse, newItem: GetConsultResponse): Boolean =
                oldItem.sessionId == newItem.sessionId

            override fun areContentsTheSame(oldItem: GetConsultResponse, newItem: GetConsultResponse): Boolean =
                oldItem == newItem
        }

        private val INPUT_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME  // "2025-11-19T17:22:00" 형태 가정

        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy년 MM월 dd일", Locale.KOREA)

        private val TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("a hh:mm", Locale.KOREA)
    }

    inner class HistoryViewHolder(
        private val binding: ItemConsultHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GetConsultResponse) {
            // 제목 (지금은 고정 문구, 원하면 "상담 #1" 같은 걸로 바꿀 수 있음)
            binding.titleTv.text = "새로운 상담"

            // 날짜/시간 표시
            binding.dateTv.text = formatStartedAt(item.startedAt)

            // 메시지 개수 표시: "• 1개 메시지"
            binding.messageCountTv.text = "• ${item.messageCount}개 메시지"

            // 카드 전체 클릭 → 해당 세션 열기 등
            binding.root.setOnClickListener {
                onClick(item)
            }

            // 삭제 아이콘 클릭
            binding.deleteIv.setOnClickListener {
                onDelete(item)
            }
        }

        private fun formatStartedAt(raw: String): String {
            return try {
                val dt = LocalDateTime.parse(raw, INPUT_FORMATTER)
                val datePart = dt.format(DATE_FORMATTER)
                val timePart = dt.format(TIME_FORMATTER)
                "$datePart\n$timePart"
            } catch (e: Exception) {
                // 파싱 실패 시 raw 그대로 보여줌 (최소 망가지진 않게)
                raw
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemConsultHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
