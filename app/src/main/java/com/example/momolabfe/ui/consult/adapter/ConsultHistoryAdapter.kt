package com.example.momolabfe.ui.consult.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.databinding.ItemConsultHistoryBinding
import com.example.momolabfe.remote.consult.model.GetConsultResponse
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
            DateTimeFormatter.ISO_LOCAL_DATE_TIME

        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy년 MM월 dd일", Locale.KOREA)

        private val TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("a hh:mm", Locale.KOREA)

        private const val MAX_TITLE_LENGTH = 20
    }

    inner class HistoryViewHolder(
        private val binding: ItemConsultHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GetConsultResponse) {
            val rawTitle = item.firstUserQuestion
                ?.takeIf { it.isNotBlank() }
                ?: "새로운 상담"

            val displayTitle = if (rawTitle.length > MAX_TITLE_LENGTH) {
                rawTitle.take(MAX_TITLE_LENGTH) + "…" // 20자 + …
            } else {
                rawTitle
            }

            binding.titleTv.apply {
                text = displayTitle

                paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            }

            binding.dateTv.text = formatEndedAt(item.endedAt)

            binding.summaryTv.text = item.summary
                ?.takeIf { it.isNotBlank() }
                ?: "요약 생성 중..."

            binding.titleTv.setOnClickListener {
                onClick(item)
            }

            binding.deleteIv.setOnClickListener {
                onDelete(item)
            }
        }

        private fun formatEndedAt(raw: String): String {
            return try {
                val dt = LocalDateTime.parse(raw, INPUT_FORMATTER)
                val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 a h:mm", Locale.KOREA)
                dt.format(formatter)
            } catch (e: Exception) {
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
