package com.ekhtiari.smsorganizer.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.Telephony
import com.ekhtiari.smsorganizer.data.local.MessageMetaDao
import com.ekhtiari.smsorganizer.data.local.MessageMetaEntity
import com.ekhtiari.smsorganizer.data.local.ThreadMetaDao
import com.ekhtiari.smsorganizer.util.SmsPermissions

/**
 * Default-SMS-app access to Telephony.Sms and Telephony.Threads.
 * Deletes try the item URI first (content://sms/{id} and
 * content://mms-sms/conversations/{id}) and fall back to a selection.
 */
class TelephonySmsContentSource(private val context: Context) : SmsContentSource {

    override fun canDeleteSms(): Boolean =
        SmsPermissions.isDefaultSmsApp(context) || SmsPermissions.isSmsRoleHeld(context)

    override fun findMessage(messageId: Long): StoredSms? =
        query(selection = "${Telephony.Sms._ID}=?", args = arrayOf(messageId.toString())).firstOrNull()

    override fun findThread(threadId: Long): List<StoredSms> =
        query(selection = "${Telephony.Sms.THREAD_ID}=?", args = arrayOf(threadId.toString()))

    override fun deleteMessage(messageId: Long): Int =
        executeDeletePlan(messageDeletePlan(messageId), ::deleteAttempt)

    override fun deleteThread(threadId: Long): Int =
        executeDeletePlan(threadDeletePlan(threadId), ::deleteAttempt)

    override fun insertMessage(message: StoredSms): Long? {
        val withThread = insert(message, includeThread = true)
        if (withThread != null) return withThread
        return insert(message, includeThread = false)
    }

    override fun notifyChanged() {
        val resolver = context.contentResolver
        runCatching { resolver.notifyChange(Telephony.Sms.CONTENT_URI, null) }
        runCatching { resolver.notifyChange(Telephony.Threads.CONTENT_URI, null) }
    }

    private fun deleteAttempt(attempt: DeleteAttempt): Int {
        val resolver = context.contentResolver
        // One OEM URI can throw while the next one deletes. A zero count lets the
        // plan continue; the caller checks whether the row is actually gone.
        return try {
            deleteAt(resolver, attempt)
        } catch (_: Exception) {
            0
        }
    }

    private fun deleteAt(resolver: ContentResolver, attempt: DeleteAttempt): Int =
        when (attempt.target) {
            SmsDeleteTarget.MessageItem -> resolver.delete(
                ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, attempt.id),
                null,
                null,
            )
            SmsDeleteTarget.MessageSelection -> resolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms._ID}=?",
                arrayOf(attempt.id.toString()),
            )
            SmsDeleteTarget.ConversationItem -> resolver.delete(
                ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, attempt.id),
                null,
                null,
            )
            SmsDeleteTarget.SmsConversationItem -> resolver.delete(
                ContentUris.withAppendedId(Telephony.Sms.Conversations.CONTENT_URI, attempt.id),
                null,
                null,
            )
            SmsDeleteTarget.ConversationMessages -> resolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(attempt.id.toString()),
            )
        }

    private fun insert(message: StoredSms, includeThread: Boolean): Long? {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, message.address)
            put(Telephony.Sms.BODY, message.body)
            put(Telephony.Sms.DATE, message.dateMillis)
            if (message.dateSentMillis > 0L) put(Telephony.Sms.DATE_SENT, message.dateSentMillis)
            put(Telephony.Sms.READ, if (message.read) 1 else 0)
            put(Telephony.Sms.SEEN, if (message.seen) 1 else 0)
            put(Telephony.Sms.TYPE, message.type)
            if (includeThread && message.threadId > 0L) {
                put(Telephony.Sms.THREAD_ID, message.threadId)
            }
            if (message.subscriptionId >= 0) {
                put(Telephony.Sms.SUBSCRIPTION_ID, message.subscriptionId)
            }
        }
        val uri = try {
            context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        } catch (_: Exception) {
            null
        } ?: return null
        return try {
            ContentUris.parseId(uri).takeIf { it > 0L }
        } catch (_: Exception) {
            null
        }
    }

    private fun query(selection: String, args: Array<String>): List<StoredSms> {
        val resolver = context.contentResolver
        val cursor = resolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            args,
            "${Telephony.Sms.DATE} ASC",
        ) ?: return emptyList()
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdx = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val dateSentIdx = it.getColumnIndex(Telephony.Sms.DATE_SENT)
            val readIdx = it.getColumnIndexOrThrow(Telephony.Sms.READ)
            val seenIdx = it.getColumnIndex(Telephony.Sms.SEEN)
            val typeIdx = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val subIdx = it.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)
            val rows = mutableListOf<StoredSms>()
            while (it.moveToNext()) {
                rows += StoredSms(
                    id = it.getLong(idIdx),
                    threadId = it.getLong(threadIdx),
                    address = it.getString(addressIdx).orEmpty(),
                    body = it.getString(bodyIdx).orEmpty(),
                    dateMillis = it.getLong(dateIdx),
                    dateSentMillis = if (dateSentIdx >= 0 && !it.isNull(dateSentIdx)) {
                        it.getLong(dateSentIdx)
                    } else {
                        0L
                    },
                    type = it.getInt(typeIdx),
                    read = it.getInt(readIdx) == 1,
                    seen = seenIdx < 0 || it.isNull(seenIdx) || it.getInt(seenIdx) == 1,
                    subscriptionId = if (subIdx >= 0 && !it.isNull(subIdx)) it.getInt(subIdx) else -1,
                )
            }
            return rows
        }
    }

    private companion object {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.DATE_SENT,
            Telephony.Sms.READ,
            Telephony.Sms.SEEN,
            Telephony.Sms.TYPE,
            Telephony.Sms.SUBSCRIPTION_ID,
        )
    }
}

class RoomMessageMetaStore(
    private val messages: MessageMetaDao,
    private val threads: ThreadMetaDao,
) : MessageMetaStore {
    override suspend fun metaFor(messageId: Long): Pair<String, String?>? {
        val row = messages.get(messageId) ?: return null
        return row.category to row.otpCode
    }

    override suspend fun deleteMessages(messageIds: List<Long>) {
        if (messageIds.isEmpty()) return
        if (messageIds.size == 1) messages.delete(messageIds.first())
        else messages.deleteAll(messageIds)
    }

    override suspend fun deleteThread(threadId: Long) {
        threads.delete(threadId)
    }

    override suspend fun restoreMessage(messageId: Long, category: String, otpCode: String?) {
        messages.upsert(
            MessageMetaEntity(
                messageId = messageId,
                category = category,
                otpCode = otpCode,
            ),
        )
    }
}
