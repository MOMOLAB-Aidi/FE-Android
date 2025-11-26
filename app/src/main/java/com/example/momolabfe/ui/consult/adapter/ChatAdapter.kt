package com.example.momolabfe.ui.consult.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.databinding.ItemChatAgentBinding
import com.example.momolabfe.databinding.ItemChatUserBinding
import com.example.momolabfe.ui.consult.data.ChatMessage

class ChatAdapter :
    ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val TYPE_AGENT = 0
        private const val TYPE_USER = 1

        object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                // 간단하게는 equals 기준으로
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUser) TYPE_USER else TYPE_AGENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> {
                val binding = ItemChatUserBinding.inflate(inflater, parent, false)
                UserViewHolder(binding)
            }
            else -> {
                val binding = ItemChatAgentBinding.inflate(inflater, parent, false)
                AgentViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is UserViewHolder -> holder.bind(item)
            is AgentViewHolder -> holder.bind(item)
        }
    }

    inner class AgentViewHolder(
        private val binding: ItemChatAgentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatMessage) {
            setMarkdownBold(binding.agentMsgTv, item.text)
        }
    }

    inner class UserViewHolder(
        private val binding: ItemChatUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatMessage) {
            binding.userMsgTv.text = item.text
        }
    }

    private fun setMarkdownBold(textView: TextView, raw: String) {
        // 볼드체 변환
        val withBold = raw.replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")

        // 줄바꿈도 HTML로 변환
        val html = withBold.replace("\n", "<br>")

        textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}