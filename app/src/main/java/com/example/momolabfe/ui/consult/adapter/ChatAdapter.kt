package com.example.momolabfe.ui.consult.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
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

class ChatAdapter(
    private val onAgentSpeakerToggle: ((text: String, turnOn: Boolean) -> Unit)? = null,
    private val onUserSpeakToggle: ((text: String, turnOn: Boolean) -> Unit)? = null
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback) {

    private var typingAnimation: Animation? = null

    // 현재 "재생 중"인 에이전트 메시지 ID
    private var playingAgentMessageId: Long? = null // 현재 재생 중인 에이전트 메시지 ID
    private var playingUserMessageId: Long? = null // 현재 재생 중인 사용자 메시지 ID

    private fun getTypingAnimation(context: Context): Animation {
        return typingAnimation ?: AnimationUtils.loadAnimation(context, R.anim.typing_blink).also {
            typingAnimation = it
        }
    }

    fun onTtsFinished() {
        playingAgentMessageId = null
        playingUserMessageId = null
        notifyDataSetChanged()
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

            val context = binding.root.context

            if (item.isTyping) {
                // 타이핑 인디케이터 + 애니메이션 효과
                binding.agentMsgTv.text = binding.root.context.getString(R.string.typing_indicator)
                binding.agentMsgTv.startAnimation(getTypingAnimation(binding.root.context))
                return
            }

            binding.agentMsgTv.clearAnimation()

            when {
                // 세션 경고
                item.isTokenWarning -> {
                    binding.agentMsgTv.setTextColor(context.getColor(R.color.warning_yellow))
                    binding.agentMsgTv.setTypeface(null, Typeface.BOLD)
                    binding.agentMsgTv.text = item.text
                }

                // 세션 종료
                item.isSessionEnd -> {
                    binding.agentMsgTv.setTextColor(context.getColor(R.color.red))
                    binding.agentMsgTv.setTypeface(null, Typeface.BOLD)
                    binding.agentMsgTv.text = item.text
                }

                // 일반 에이전트 답변
                else -> {
                    binding.agentMsgTv.setTextColor(context.getColor(R.color.text_primary))
                    binding.agentMsgTv.setTypeface(null, Typeface.NORMAL)
                    setMarkdownBold(binding.agentMsgTv, item.text)
                }
            }

            // 스피커 토글 처리
            val hasText = item.text.isNotBlank()
            val isPlaying = (item.id == playingAgentMessageId)

            binding.agentSpeakerIv.setImageResource(
                if (isPlaying) R.drawable.ic_speaker_on_sv
                else R.drawable.ic_speaker_off_sv
            )

            // 콜백이 없거나 텍스트가 없으면 숨김
            val canUseSpeaker = hasText && onAgentSpeakerToggle != null
            binding.agentSpeakerIv.visibility =
                if (canUseSpeaker) View.VISIBLE else View.GONE

            if (!canUseSpeaker) {
                binding.agentSpeakerIv.setOnClickListener(null)
                return
            }

            binding.agentSpeakerIv.setOnClickListener {
                val speakText = binding.agentMsgTv.text.toString() // 화면에 실제로 보이는 텍스트 기준으로 읽기

                if (playingAgentMessageId == item.id) {
                    playingAgentMessageId = null
                    onAgentSpeakerToggle?.invoke(speakText, false) // 음성 중지 요청
                } else {
                    playingAgentMessageId = item.id
                    playingUserMessageId = null // 사용자 쪽 재생 중이면 끄기
                    onAgentSpeakerToggle?.invoke(speakText, true) // 음성 재생 요청
                }

                notifyDataSetChanged()
            }
        }
    }

    inner class UserViewHolder(
        private val binding: ItemChatUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatMessage) {
            binding.userMsgTv.text = item.text

            val hasText = item.text.isNotBlank()
            val isPlaying = (item.id == playingUserMessageId)

            binding.userSpeakerIv.setImageResource(
                if (isPlaying) R.drawable.ic_speaker_on_sv
                else R.drawable.ic_speaker_off_sv
            )

            val canUseSpeaker = hasText && onUserSpeakToggle != null
            binding.userSpeakerIv.visibility =
                if (canUseSpeaker) View.VISIBLE else View.GONE

            if (!canUseSpeaker) {
                binding.userSpeakerIv.setOnClickListener(null)
                return
            }

            binding.userSpeakerIv.setOnClickListener {

                if (playingUserMessageId == item.id) {
                    playingUserMessageId = null
                    onUserSpeakToggle?.invoke(item.text, false)
                } else {
                    playingUserMessageId = item.id
                    playingAgentMessageId = null
                    onUserSpeakToggle?.invoke(item.text, true)
                }

                notifyDataSetChanged()
            }
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