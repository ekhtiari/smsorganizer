package com.ekhtiari.smsorganizer.domain

data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
    val read: Boolean,
    val subscriptionId: Int,
    val category: MessageCategory = MessageCategory.PERSONAL,
    val otpCode: String? = null,
) {
    val isIncoming: Boolean get() = type == TYPE_INBOX || type == TYPE_ALL
    val isOutgoing: Boolean get() = type == TYPE_SENT || type == TYPE_OUTBOX || type == TYPE_FAILED || type == TYPE_QUEUED

    companion object {
        const val TYPE_ALL = 0
        const val TYPE_INBOX = 1
        const val TYPE_SENT = 2
        const val TYPE_DRAFT = 3
        const val TYPE_OUTBOX = 4
        const val TYPE_FAILED = 5
        const val TYPE_QUEUED = 6
    }
}
