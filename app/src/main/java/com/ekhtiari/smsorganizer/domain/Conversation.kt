package com.ekhtiari.smsorganizer.domain

data class Conversation(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val snippet: String,
    val date: Long,
    val unreadCount: Int,
    val subscriptionId: Int,
    val category: MessageCategory,
    val otpCode: String? = null,
) {
    val displayName: String get() = contactName?.takeIf { it.isNotBlank() } ?: address
}
