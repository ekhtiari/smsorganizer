package com.ekhtiari.smsorganizer.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import com.ekhtiari.smsorganizer.data.local.AppDatabase
import com.ekhtiari.smsorganizer.data.local.MessageMetaEntity
import com.ekhtiari.smsorganizer.data.local.ThreadMetaEntity
import com.ekhtiari.smsorganizer.domain.Conversation
import com.ekhtiari.smsorganizer.domain.MessageCategory
import com.ekhtiari.smsorganizer.domain.MessageClassifier
import com.ekhtiari.smsorganizer.domain.SmsMessage
import com.ekhtiari.smsorganizer.util.ContactHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsRepository(private val context: Context) {

    private val db = AppDatabase.get(context)
    private val metaDao = db.messageMetaDao()
    private val threadMetaDao = db.threadMetaDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observeConversations(category: MessageCategory?): Flow<List<Conversation>> {
        val smsFlow = observeSmsChanges()
        val metaFlow = metaDao.observeAll()
        val archivedFlow = threadMetaDao.observeArchivedThreadIds()
        return combine(smsFlow, metaFlow, archivedFlow) { _, metas, archivedIds ->
            val metaMap = metas.associateBy { it.messageId }
            val archived = archivedIds.toHashSet()
            loadConversations(category, metaMap).filter { it.threadId !in archived }
        }
    }

    fun observeThread(threadId: Long): Flow<List<SmsMessage>> {
        val smsFlow = observeSmsChanges()
        val metaFlow = metaDao.observeAll()
        return combine(smsFlow, metaFlow) { _, metas ->
            val metaMap = metas.associateBy { it.messageId }
            loadThreadMessages(threadId, metaMap)
        }
    }

    fun search(query: String): Flow<List<Conversation>> =
        observeConversations(null).map { list ->
            val q = query.trim().lowercase()
            if (q.isEmpty()) list
            else list.filter {
                it.displayName.lowercase().contains(q) ||
                    it.address.lowercase().contains(q) ||
                    it.snippet.lowercase().contains(q)
            }
        }

    suspend fun saveClassification(
        messageId: Long,
        category: MessageCategory,
        otpCode: String?,
    ) {
        metaDao.upsert(
            MessageMetaEntity(
                messageId = messageId,
                category = category.name,
                otpCode = otpCode,
            ),
        )
    }

    suspend fun classifyMissingMessages(limit: Int = 500) = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
        )
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC LIMIT $limit",
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val batch = mutableListOf<MessageMetaEntity>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                if (metaDao.get(id) != null) continue
                val address = cursor.getString(addrIdx).orEmpty()
                val body = cursor.getString(bodyIdx).orEmpty()
                val result = MessageClassifier.classify(body, address)
                batch += MessageMetaEntity(
                    messageId = id,
                    category = result.category.name,
                    otpCode = result.otpCode,
                )
            }
            if (batch.isNotEmpty()) metaDao.upsertAll(batch)
        }
    }

    fun markThreadRead(threadId: Long) {
        val values = ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        try {
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID}=? AND ${Telephony.Sms.READ}=0",
                arrayOf(threadId.toString()),
            )
        } catch (_: Exception) {
        }
    }

    fun deleteMessage(messageId: Long) {
        scope.launch { deleteMessageNow(messageId) }
    }

    suspend fun deleteMessageNow(messageId: Long) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms._ID}=?",
                arrayOf(messageId.toString()),
            )
            metaDao.delete(messageId)
        } catch (_: Exception) {
        }
    }

    fun deleteThread(threadId: Long) {
        scope.launch { deleteThreadNow(threadId) }
    }

    suspend fun deleteThreadNow(threadId: Long) = withContext(Dispatchers.IO) {
        try {
            val ids = mutableListOf<Long>()
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID),
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString()),
                null,
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                while (cursor.moveToNext()) ids += cursor.getLong(idIdx)
            }
            context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString()),
            )
            if (ids.isNotEmpty()) metaDao.deleteAll(ids)
            threadMetaDao.delete(threadId)
        } catch (_: Exception) {
        }
    }

    suspend fun archiveThread(threadId: Long) = withContext(Dispatchers.IO) {
        if (threadId <= 0L) return@withContext
        val existing = threadMetaDao.get(threadId)
        threadMetaDao.upsert(
            (existing ?: ThreadMetaEntity(threadId = threadId)).copy(archived = true),
        )
    }

    fun resolveThreadId(address: String, messageId: Long? = null): Long {
        if (messageId != null && messageId > 0L) {
            try {
                context.contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(Telephony.Sms.THREAD_ID),
                    "${Telephony.Sms._ID}=?",
                    arrayOf(messageId.toString()),
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        return cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                    }
                }
            } catch (_: Exception) {
            }
        }
        return try {
            Telephony.Threads.getOrCreateThreadId(context, address)
        } catch (_: Exception) {
            0L
        }
    }

    private fun observeSmsChanges(): Flow<Unit> = callbackFlow {
        trySend(Unit)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(Unit)
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                observer,
            )
        } catch (_: SecurityException) {
        }
        awaitClose {
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {
            }
        }
    }

    private fun loadConversations(
        categoryFilter: MessageCategory?,
        metaMap: Map<Long, MessageMetaEntity>,
    ): List<Conversation> {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE,
            Telephony.Sms.SUBSCRIPTION_ID,
        )

        val latestByThread = linkedMapOf<Long, ConversationBuilder>()

        try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC",
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val threadIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val readIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
                val subIdx = cursor.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val threadId = cursor.getLong(threadIdx)
                    val address = cursor.getString(addrIdx).orEmpty()
                    val body = cursor.getString(bodyIdx).orEmpty()
                    val date = cursor.getLong(dateIdx)
                    val read = cursor.getInt(readIdx) == 1
                    val subId = if (subIdx >= 0 && !cursor.isNull(subIdx)) cursor.getInt(subIdx) else -1

                    val meta = metaMap[id]
                    val classification = if (meta != null) {
                        MessageClassifier.Classification(
                            MessageCategory.fromName(meta.category),
                            meta.otpCode,
                        )
                    } else {
                        MessageClassifier.classify(body, address).also { result ->
                            scope.launch {
                                metaDao.upsert(
                                    MessageMetaEntity(id, result.category.name, result.otpCode),
                                )
                            }
                        }
                    }

                    val existing = latestByThread[threadId]
                    if (existing == null) {
                        latestByThread[threadId] = ConversationBuilder(
                            threadId = threadId,
                            address = address,
                            snippet = body,
                            date = date,
                            unreadCount = if (!read) 1 else 0,
                            subscriptionId = subId,
                            category = classification.category,
                            otpCode = classification.otpCode,
                        )
                    } else {
                        if (!read) existing.unreadCount++
                        // Keep category of the latest message (already set)
                    }
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        }

        return latestByThread.values
            .map { builder ->
                Conversation(
                    threadId = builder.threadId,
                    address = builder.address,
                    contactName = ContactHelper.resolveDisplayName(context, builder.address),
                    snippet = builder.snippet,
                    date = builder.date,
                    unreadCount = builder.unreadCount,
                    subscriptionId = builder.subscriptionId,
                    category = builder.category,
                    otpCode = builder.otpCode,
                )
            }
            .filter { categoryFilter == null || it.category == categoryFilter }
    }

    private fun loadThreadMessages(
        threadId: Long,
        metaMap: Map<Long, MessageMetaEntity>,
    ): List<SmsMessage> {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE,
            Telephony.Sms.SUBSCRIPTION_ID,
        )
        val result = mutableListOf<SmsMessage>()
        try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} ASC",
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val threadIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val readIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
                val typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                val subIdx = cursor.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val address = cursor.getString(addrIdx).orEmpty()
                    val body = cursor.getString(bodyIdx).orEmpty()
                    val meta = metaMap[id]
                    val classification = if (meta != null) {
                        MessageClassifier.Classification(
                            MessageCategory.fromName(meta.category),
                            meta.otpCode,
                        )
                    } else {
                        MessageClassifier.classify(body, address)
                    }
                    result += SmsMessage(
                        id = id,
                        threadId = cursor.getLong(threadIdx),
                        address = address,
                        body = body,
                        date = cursor.getLong(dateIdx),
                        type = cursor.getInt(typeIdx),
                        read = cursor.getInt(readIdx) == 1,
                        subscriptionId = if (subIdx >= 0 && !cursor.isNull(subIdx)) cursor.getInt(subIdx) else -1,
                        category = classification.category,
                        otpCode = classification.otpCode,
                    )
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        }
        return result
    }

    private data class ConversationBuilder(
        val threadId: Long,
        val address: String,
        val snippet: String,
        val date: Long,
        var unreadCount: Int,
        val subscriptionId: Int,
        val category: MessageCategory,
        val otpCode: String?,
    )
}
