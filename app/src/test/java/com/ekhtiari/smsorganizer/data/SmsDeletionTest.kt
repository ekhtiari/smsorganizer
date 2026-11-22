package com.ekhtiari.smsorganizer.data

import com.ekhtiari.smsorganizer.data.repository.DeleteOutcome
import com.ekhtiari.smsorganizer.data.repository.MessageMetaStore
import com.ekhtiari.smsorganizer.data.repository.SmsContentSource
import com.ekhtiari.smsorganizer.data.repository.SmsDeleteTarget
import com.ekhtiari.smsorganizer.data.repository.SmsDeletion
import com.ekhtiari.smsorganizer.data.repository.SmsRefreshBus
import com.ekhtiari.smsorganizer.data.repository.StoredSms
import com.ekhtiari.smsorganizer.data.repository.executeDeletePlan
import com.ekhtiari.smsorganizer.data.repository.messageDeletePlan
import com.ekhtiari.smsorganizer.data.repository.threadDeletePlan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SmsDeletionTest {

    @Test
    fun messagePlanTriesItemUriBeforeSelection() {
        val plan = messageDeletePlan(15L)
        assertEquals(
            listOf(SmsDeleteTarget.MessageItem, SmsDeleteTarget.MessageSelection),
            plan.map { it.target },
        )
        assertTrue(plan.all { it.id == 15L })
    }

    @Test
    fun threadPlanTriesConversationUrisThenSmsSelection() {
        val plan = threadDeletePlan(9L)
        assertEquals(
            listOf(
                SmsDeleteTarget.ConversationItem,
                SmsDeleteTarget.SmsConversationItem,
                SmsDeleteTarget.ConversationMessages,
            ),
            plan.map { it.target },
        )
    }

    @Test
    fun deletePlanStopsAtTheFirstPositiveCount() {
        val calls = mutableListOf<SmsDeleteTarget>()
        val count = executeDeletePlan(messageDeletePlan(1L)) { attempt ->
            calls += attempt.target
            if (attempt.target == SmsDeleteTarget.MessageItem) 1 else 0
        }
        assertEquals(1, count)
        assertEquals(listOf(SmsDeleteTarget.MessageItem), calls)
    }

    @Test
    fun deletePlanFallsThroughWhenTheFirstUriRemovesNothing() {
        val calls = mutableListOf<SmsDeleteTarget>()
        val count = executeDeletePlan(threadDeletePlan(4L)) { attempt ->
            calls += attempt.target
            if (attempt.target == SmsDeleteTarget.ConversationMessages) 3 else 0
        }
        assertEquals(3, count)
        assertEquals(
            listOf(
                SmsDeleteTarget.ConversationItem,
                SmsDeleteTarget.SmsConversationItem,
                SmsDeleteTarget.ConversationMessages,
            ),
            calls,
        )
    }

    @Test
    fun deletePlanPropagatesProviderErrors() {
        var second = false
        try {
            executeDeletePlan(messageDeletePlan(1L)) { attempt ->
                if (attempt.target == SmsDeleteTarget.MessageItem) {
                    throw SecurityException("not default")
                }
                second = true
                1
            }
            error("expected SecurityException")
        } catch (_: SecurityException) {
        }
        assertFalse(second)
    }

    @Test
    fun refusesDeleteWhenTheAppIsNotTheDefaultSmsApp() = runTest {
        val content = FakeSmsContent(canDelete = false, messages = mutableListOf(sms(id = 5, threadId = 1)))
        val meta = FakeMeta()
        val bus = SmsRefreshBus()
        val deletion = SmsDeletion(content, meta, bus)
        val events = collect(bus)

        val message = deletion.deleteMessage(5)
        val thread = deletion.deleteThread(1)

        assertEquals(DeleteOutcome.RequiresDefaultSms, message)
        assertEquals(DeleteOutcome.RequiresDefaultSms, thread)
        assertEquals(listOf(sms(id = 5, threadId = 1)), content.messages)
        assertTrue(meta.deletedMessages.isEmpty())
        assertTrue(events.isEmpty())
        events.cancel()
    }

    @Test
    fun deleteMessageRemovesTheRowUpdatesRoomAndRefreshes() = runTest {
        val content = FakeSmsContent(messages = mutableListOf(sms(id = 5, threadId = 1, body = "hello")))
        val meta = FakeMeta().apply { metas[5L] = "OTP" to "123456" }
        val bus = SmsRefreshBus()
        val deletion = SmsDeletion(content, meta, bus)
        val events = collect(bus)

        val outcome = deletion.deleteMessage(5)

        val success = outcome as DeleteOutcome.Success
        assertEquals(1, success.deletedCount)
        assertEquals("hello", success.undo.messages.single().body)
        assertEquals("OTP", success.undo.messages.single().category)
        assertEquals("123456", success.undo.messages.single().otpCode)
        assertTrue(content.messages.isEmpty())
        assertEquals(listOf(5L), meta.deletedMessages)
        assertEquals(1, events.size)
        events.cancel()
    }

    @Test
    fun deleteMessageFailsWhenTheProviderKeepsTheRow() = runTest {
        val content = FakeSmsContent(
            messages = mutableListOf(sms(id = 5, threadId = 1)),
            deleteRemoves = false,
        )
        val meta = FakeMeta().apply { metas[5L] = "PERSONAL" to null }
        val bus = SmsRefreshBus()
        val deletion = SmsDeletion(content, meta, bus)
        val events = collect(bus)

        val outcome = deletion.deleteMessage(5)

        assertTrue(outcome is DeleteOutcome.Failed)
        assertEquals(1, content.messages.size)
        assertTrue(meta.deletedMessages.isEmpty())
        assertTrue(events.isEmpty())
        events.cancel()
    }

    @Test
    fun deleteThreadFallsBackToEachMessageAndRefreshes() = runTest {
        val content = FakeSmsContent(
            messages = mutableListOf(
                sms(id = 1, threadId = 7, body = "a"),
                sms(id = 2, threadId = 7, body = "b"),
                sms(id = 3, threadId = 8, body = "other"),
            ),
            threadDeleteRemoves = false,
        )
        val meta = FakeMeta()
        val bus = SmsRefreshBus()
        val deletion = SmsDeletion(content, meta, bus)
        val events = collect(bus)

        val outcome = deletion.deleteThread(7)

        val success = outcome as DeleteOutcome.Success
        assertEquals(2, success.deletedCount)
        assertEquals(listOf("a", "b"), success.undo.messages.map { it.body })
        assertEquals(listOf(3L), content.messages.map { it.id })
        assertEquals(listOf(1L, 2L), meta.deletedMessages)
        assertEquals(listOf(7L), meta.deletedThreads)
        assertEquals(listOf("thread:7", "message:1", "message:2"), content.deletes)
        assertEquals(1, events.size)
        events.cancel()
    }

    @Test
    fun deleteThreadReportsFailureWhenRowsRemain() = runTest {
        val content = FakeSmsContent(
            messages = mutableListOf(sms(id = 1, threadId = 7)),
            threadDeleteRemoves = false,
            deleteRemoves = false,
        )
        val meta = FakeMeta()
        val bus = SmsRefreshBus()
        val deletion = SmsDeletion(content, meta, bus)

        val outcome = deletion.deleteThread(7)

        assertTrue(outcome is DeleteOutcome.Failed)
        assertEquals(1, content.messages.size)
        assertTrue(meta.deletedMessages.isEmpty())
        assertTrue(meta.deletedThreads.isEmpty())
    }

    @Test
    fun undoReinsertsMessagesRestoresMetaAndRefreshes() = runTest {
        val content = FakeSmsContent()
        val meta = FakeMeta()
        val bus = SmsRefreshBus()
        val deletion = SmsDeletion(content, meta, bus)
        content.messages += sms(id = 4, threadId = 2, body = "back")
        val removed = deletion.deleteMessage(4) as DeleteOutcome.Success
        val events = collect(bus)

        val restored = deletion.undo(removed.undo)

        assertTrue(restored)
        assertEquals("back", content.messages.single().body)
        assertEquals("PERSONAL", meta.restored.single().second)
        assertNull(meta.restored.single().third)
        assertEquals(1, events.size)
        events.cancel()
    }

    @Test
    fun undoFailureDoesNotClaimSuccess() = runTest {
        val content = FakeSmsContent(insertSucceeds = false)
        val deletion = SmsDeletion(content, FakeMeta(), SmsRefreshBus())
        content.messages += sms(id = 4, threadId = 2)
        val removed = deletion.deleteMessage(4) as DeleteOutcome.Success

        assertFalse(deletion.undo(removed.undo))
        assertTrue(content.messages.isEmpty())
    }

    @Test
    fun manualRefreshIsVisibleToCollectors() = runTest {
        val bus = SmsRefreshBus()
        val events = collect(bus)
        bus.notifyChanged()
        bus.notifyChanged()
        assertEquals(2, events.size)
        events.cancel()
    }

    private fun kotlinx.coroutines.test.TestScope.collect(bus: SmsRefreshBus): RefreshEvents {
        val events = mutableListOf<Unit>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.signals.collect { events += Unit }
        }
        return RefreshEvents(events, job)
    }
}

private class RefreshEvents(
    private val events: List<Unit>,
    private val job: kotlinx.coroutines.Job,
) {
    val size: Int get() = events.size
    fun isEmpty() = events.isEmpty()
    fun cancel() = job.cancel()
}

private fun sms(
    id: Long,
    threadId: Long,
    body: String = "body",
    category: String? = "PERSONAL",
) = StoredSms(
    id = id,
    threadId = threadId,
    address = "+15551212",
    body = body,
    dateMillis = 1_700_000_000_000L,
    type = 1,
    read = true,
    category = category,
)

private class FakeSmsContent(
    var canDelete: Boolean = true,
    val messages: MutableList<StoredSms> = mutableListOf(),
    var deleteRemoves: Boolean = true,
    var threadDeleteRemoves: Boolean = true,
    var insertSucceeds: Boolean = true,
) : SmsContentSource {
    val deletes = mutableListOf<String>()
    var notifications = 0

    override fun canDeleteSms(): Boolean = canDelete

    override fun findMessage(messageId: Long): StoredSms? = messages.find { it.id == messageId }

    override fun findThread(threadId: Long): List<StoredSms> = messages.filter { it.threadId == threadId }

    override fun deleteMessage(messageId: Long): Int {
        deletes += "message:$messageId"
        if (!deleteRemoves) return 0
        val before = messages.size
        messages.removeAll { it.id == messageId }
        return before - messages.size
    }

    override fun deleteThread(threadId: Long): Int {
        deletes += "thread:$threadId"
        if (!threadDeleteRemoves) return 0
        val before = messages.size
        messages.removeAll { it.threadId == threadId }
        return before - messages.size
    }

    override fun insertMessage(message: StoredSms): Long? {
        if (!insertSucceeds) return null
        val id = (messages.maxOfOrNull { it.id } ?: 0L) + 100L
        messages += message.copy(id = id)
        return id
    }

    override fun notifyChanged() {
        notifications++
    }
}

private class FakeMeta : MessageMetaStore {
    val metas = mutableMapOf<Long, Pair<String, String?>>()
    val deletedMessages = mutableListOf<Long>()
    val deletedThreads = mutableListOf<Long>()
    val restored = mutableListOf<Triple<Long, String, String?>>()

    override suspend fun metaFor(messageId: Long): Pair<String, String?>? = metas[messageId]

    override suspend fun deleteMessages(messageIds: List<Long>) {
        deletedMessages += messageIds
        messageIds.forEach { metas.remove(it) }
    }

    override suspend fun deleteThread(threadId: Long) {
        deletedThreads += threadId
    }

    override suspend fun restoreMessage(messageId: Long, category: String, otpCode: String?) {
        restored += Triple(messageId, category, otpCode)
        metas[messageId] = category to otpCode
    }
}
