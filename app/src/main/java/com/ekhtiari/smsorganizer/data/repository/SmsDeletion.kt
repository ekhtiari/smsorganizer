package com.ekhtiari.smsorganizer.data.repository

/**
 * Where a delete is applied. Order matters: item URIs first, then a selection,
 * because some OEM providers ignore a selection on content://sms.
 */
enum class SmsDeleteTarget {
    /** content://sms/{id} */
    MessageItem,

    /** content://sms with _ID = ? */
    MessageSelection,

    /** content://mms-sms/conversations/{threadId} */
    ConversationItem,

    /** content://sms/conversations/{threadId} */
    SmsConversationItem,

    /** content://sms with THREAD_ID = ? */
    ConversationMessages,
}

data class DeleteAttempt(val target: SmsDeleteTarget, val id: Long)

fun messageDeletePlan(messageId: Long): List<DeleteAttempt> = listOf(
    DeleteAttempt(SmsDeleteTarget.MessageItem, messageId),
    DeleteAttempt(SmsDeleteTarget.MessageSelection, messageId),
)

fun threadDeletePlan(threadId: Long): List<DeleteAttempt> = listOf(
    DeleteAttempt(SmsDeleteTarget.ConversationItem, threadId),
    DeleteAttempt(SmsDeleteTarget.SmsConversationItem, threadId),
    DeleteAttempt(SmsDeleteTarget.ConversationMessages, threadId),
)

/**
 * Runs [plan] in order and returns the first positive row count.
 * A zero count tries the next URI. Exceptions propagate so the caller can
 * surface a failure instead of treating a rejected delete as success.
 */
fun executeDeletePlan(plan: List<DeleteAttempt>, delete: (DeleteAttempt) -> Int): Int {
    for (attempt in plan) {
        val count = delete(attempt)
        if (count > 0) return count
    }
    return 0
}

/** Snapshot of one SMS row, including Room classification so undo can restore it. */
data class StoredSms(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val dateMillis: Long,
    val dateSentMillis: Long = 0L,
    val type: Int,
    val read: Boolean,
    val seen: Boolean = true,
    val subscriptionId: Int = -1,
    val category: String? = null,
    val otpCode: String? = null,
)

data class DeleteUndo(val messages: List<StoredSms>)

sealed class DeleteOutcome {
    data class Success(val deletedCount: Int, val undo: DeleteUndo) : DeleteOutcome()
    data object RequiresDefaultSms : DeleteOutcome()
    data class Failed(val reason: String) : DeleteOutcome()
}

/** Operations the inbox and conversation screens need from [SmsRepository]. */
interface SmsDeleter {
    suspend fun deleteMessageNow(messageId: Long): DeleteOutcome
    suspend fun deleteThreadNow(threadId: Long): DeleteOutcome
    suspend fun undoDelete(undo: DeleteUndo): Boolean
}

/** Telephony provider access. Tests use a fake; the app uses [TelephonySmsContentSource]. */
interface SmsContentSource {
    /** True when this app holds the default SMS role and may mutate the provider. */
    fun canDeleteSms(): Boolean

    fun findMessage(messageId: Long): StoredSms?

    fun findThread(threadId: Long): List<StoredSms>

    /** @return provider rows removed. Zero means that attempt removed nothing. */
    fun deleteMessage(messageId: Long): Int

    /** @return provider rows removed. Zero means that attempt removed nothing. */
    fun deleteThread(threadId: Long): Int

    /** Re-insert a deleted message. Returns the new row id. */
    fun insertMessage(message: StoredSms): Long?

    /** Ask the provider to notify observers on sms and conversation URIs. */
    fun notifyChanged()
}

interface MessageMetaStore {
    suspend fun metaFor(messageId: Long): Pair<String, String?>?

    suspend fun deleteMessages(messageIds: List<Long>)

    suspend fun deleteThread(threadId: Long)

    suspend fun restoreMessage(messageId: Long, category: String, otpCode: String?)
}

/**
 * Deletes SMS through the telephony provider, then the local Room cache, and
 * always pokes [refresh] after the provider view of the data changes.
 */
class SmsDeletion(
    private val content: SmsContentSource,
    private val meta: MessageMetaStore,
    private val refresh: SmsRefreshBus,
) {
    suspend fun deleteMessage(messageId: Long): DeleteOutcome {
        if (!content.canDeleteSms()) return DeleteOutcome.RequiresDefaultSms
        val before = try {
            content.findMessage(messageId)?.let { withMeta(it) }
        } catch (e: SecurityException) {
            return DeleteOutcome.Failed("SMS permission denied")
        } catch (e: Exception) {
            return DeleteOutcome.Failed(e.message ?: "Couldn't read the message")
        }
        if (before == null) {
            meta.deleteMessages(listOf(messageId))
            publishChange()
            return DeleteOutcome.Success(deletedCount = 0, undo = DeleteUndo(emptyList()))
        }
        return try {
            content.deleteMessage(messageId)
            if (content.findMessage(messageId) != null) {
                DeleteOutcome.Failed("The message is still in the SMS provider")
            } else {
                meta.deleteMessages(listOf(messageId))
                publishChange()
                DeleteOutcome.Success(deletedCount = 1, undo = DeleteUndo(listOf(before)))
            }
        } catch (e: SecurityException) {
            DeleteOutcome.Failed("SMS provider rejected the delete")
        } catch (e: Exception) {
            DeleteOutcome.Failed(e.message ?: "Delete failed")
        }
    }

    suspend fun deleteThread(threadId: Long): DeleteOutcome {
        if (!content.canDeleteSms()) return DeleteOutcome.RequiresDefaultSms
        val before = try {
            content.findThread(threadId).map { withMeta(it) }
        } catch (e: SecurityException) {
            return DeleteOutcome.Failed("SMS permission denied")
        } catch (e: Exception) {
            return DeleteOutcome.Failed(e.message ?: "Couldn't read the conversation")
        }
        if (before.isEmpty()) {
            meta.deleteThread(threadId)
            publishChange()
            return DeleteOutcome.Success(deletedCount = 0, undo = DeleteUndo(emptyList()))
        }
        return try {
            content.deleteThread(threadId)
            var remaining = content.findThread(threadId)
            if (remaining.isNotEmpty()) {
                remaining.forEach { message -> content.deleteMessage(message.id) }
                remaining = content.findThread(threadId)
            }
            val remainingIds = remaining.map { it.id }.toSet()
            val removed = before.filter { it.id !in remainingIds }
            if (remaining.isNotEmpty()) {
                if (removed.isNotEmpty()) {
                    meta.deleteMessages(removed.map { it.id })
                    publishChange()
                }
                DeleteOutcome.Failed("The conversation is still in the SMS provider")
            } else {
                meta.deleteMessages(before.map { it.id })
                meta.deleteThread(threadId)
                publishChange()
                DeleteOutcome.Success(deletedCount = before.size, undo = DeleteUndo(before))
            }
        } catch (e: SecurityException) {
            DeleteOutcome.Failed("SMS provider rejected the delete")
        } catch (e: Exception) {
            DeleteOutcome.Failed(e.message ?: "Delete failed")
        }
    }

    suspend fun undo(undo: DeleteUndo): Boolean {
        if (undo.messages.isEmpty() || !content.canDeleteSms()) return false
        var restored = 0
        for (message in undo.messages) {
            val newId = try {
                content.insertMessage(message)
            } catch (_: Exception) {
                null
            } ?: continue
            restored++
            val category = message.category
            if (category != null) {
                meta.restoreMessage(newId, category, message.otpCode)
            }
        }
        publishChange()
        return restored == undo.messages.size
    }

    private suspend fun withMeta(message: StoredSms): StoredSms {
        val metaRow = meta.metaFor(message.id) ?: return message
        return message.copy(category = metaRow.first, otpCode = metaRow.second)
    }

    private fun publishChange() {
        content.notifyChanged()
        refresh.notifyChanged()
    }
}
