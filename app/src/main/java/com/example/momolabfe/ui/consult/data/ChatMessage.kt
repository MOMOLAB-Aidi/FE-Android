package com.example.momolabfe.ui.consult.data

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean, // true면 오른쪽(환자), false면 왼쪽(에이전트)
    val isTyping: Boolean = false, // 에이전트 입력 중 상태를 위한 플래그
    val isTokenWarning: Boolean = false, // 토큰 경고 말풍선인지
    val isSessionEnd: Boolean = false, // 세션 종료 말풍선인지
)