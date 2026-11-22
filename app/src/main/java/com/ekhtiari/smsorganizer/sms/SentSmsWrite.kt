package com.ekhtiari.smsorganizer.sms

import android.content.ContentValues
import android.provider.Telephony

/**
 * Values written to the SMS provider after a successful send.
 * [threadId] is required so the reply appears in the open conversation
 * (queries filter by THREAD_ID).
 */
data class SentSmsWrite(
    val address: String,
    val body: String,
    val dateMillis: Long,
    val subscriptionId: Int,
    val threadId: Long,
    val read: Int = 1,
    val seen: Int = 1,
    val type: Int = Telephony.Sms.MESSAGE_TYPE_SENT,
) {
    fun toContentValues(): ContentValues = ContentValues().apply {
        put(Telephony.Sms.ADDRESS, address)
        put(Telephony.Sms.BODY, body)
        put(Telephony.Sms.DATE, dateMillis)
        put(Telephony.Sms.READ, read)
        put(Telephony.Sms.SEEN, seen)
        put(Telephony.Sms.TYPE, type)
        put(Telephony.Sms.SUBSCRIPTION_ID, subscriptionId)
        put(Telephony.Sms.THREAD_ID, threadId)
    }

    companion object {
        fun build(
            address: String,
            body: String,
            dateMillis: Long,
            subscriptionId: Int,
            threadId: Long,
        ): SentSmsWrite {
            require(threadId > 0L) { "threadId is required so sent replies show in the conversation" }
            return SentSmsWrite(
                address = address,
                body = body,
                dateMillis = dateMillis,
                subscriptionId = subscriptionId,
                threadId = threadId,
            )
        }
    }
}
