package com.example.momolabfe.ui.consult.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.momolabfe.R
import com.example.momolabfe.databinding.ItemChatAgentBinding
import com.example.momolabfe.databinding.ItemChatUserBinding
import com.example.momolabfe.ui.consult.data.ChatMessage

class ChatAdapter :
    ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback) {

    private var typingAnimation: Animation? = null
    private fun getTypingAnimation(context: Context): Animation {
        return typingAnimation ?: AnimationUtils.loadAnimation(context, R.anim.typing_blink).also {
            typingAnimation = it
        }
    }

    companion object {
        private const val TYPE_AGENT = 0
        private const val TYPE_USER = 1

        object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
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

    // 애니메이션 정리
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is AgentViewHolder) {
            holder.itemView.findViewById<TextView>(R.id.agent_msg_tv)?.clearAnimation()
        }
    }

    inner class AgentViewHolder(
        private val binding: ItemChatAgentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatMessage) {
            if (item.isTyping) {
                // 타이핑 인디케이터 + 애니메이션 효과
                binding.agentMsgTv.text = binding.root.context.getString(R.string.typing_indicator)
                binding.agentMsgTv.startAnimation(getTypingAnimation(binding.root.context))
                return
            }

            binding.agentMsgTv.clearAnimation()

            if (item.isTokenWarning) {
                // 토큰 경고 말풍선 스타일
                binding.agentMsgTv.setTextColor(binding.root.context.getColor(R.color.red))
                binding.agentMsgTv.setTypeface(null, Typeface.BOLD)

                // 경고 문구는 마크다운 파싱 필요 없으면 그냥 text
                binding.agentMsgTv.text = item.text
            } else {
                binding.agentMsgTv.setTypeface(null, Typeface.NORMAL)

                // 마크다운 처리
                setMarkdownBold(binding.agentMsgTv, item.text)
            }
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
        // HTML 특수문자 이스케이프
        var text = raw
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        // **강조** → <b>강조</b>
        text = text.replace(Regex("\\*\\*(.+?)\\*\\*")) { match ->
            "<b>${match.groupValues[1]}</b>"
        }

        // *강조* → <b>강조</b>
        // 앞뒤에 * 이 하나 더 붙어 있는 경우(즉 **강조**)는 제외
        text = text.replace(
            Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
        ) { match ->
            "<b>${match.groupValues[1]}</b>"
        }

        // 줄바꿈 처리
        val html = text.replace("\n", "<br>")
        textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}