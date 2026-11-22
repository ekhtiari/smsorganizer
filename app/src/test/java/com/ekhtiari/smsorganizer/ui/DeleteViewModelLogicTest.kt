package com.ekhtiari.smsorganizer.ui

import com.ekhtiari.smsorganizer.data.repository.DeleteOutcome
import com.ekhtiari.smsorganizer.data.repository.DeleteUndo
import com.ekhtiari.smsorganizer.data.repository.SmsContentSource
import com.ekhtiari.smsorganizer.data.repository.SmsDeleter
import com.ekhtiari.smsorganizer.data.repository.SmsDeletion
import com.ekhtiari.smsorganizer.data.repository.SmsRefreshBus
import com.ekhtiari.smsorganizer.data.repository.StoredSms
import com.ekhtiari.smsorganizer.data.repository.MessageMetaStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteViewModelLogicTest {

    @Test
    fun confirmThreadDeletePublishesUndoAndClearsTheDialog() = runTest {
        val deleter = FakeDeleter()
        val undo = DeleteUndo(listOf(sms(1, 9)))
        deleter.threadOutcome = DeleteOutcome.Success(1, undo)
        val logic = DeleteViewModelLogic(deleter)

        logic.requestDeleteThread(9)
        assertEquals(9L, logic.state.value.confirmThreadId)
        logic.confirmThread()

        assertNull(logic.state.value.confirmThreadId)
        assertEquals(listOf(9L), deleter.threadDeletes)
        val notice = logic.state.value.notice!!
        assertEquals(DeleteNoticeKind.ConversationDeleted, notice.kind)
        assertEquals(undo, notice.undo)
    }

    @Test
    fun confirmMessageDeletePublishesUndo() = runTest {
        val deleter = FakeDeleter()
        val undo = DeleteUndo(listOf(sms(4, 2)))
        deleter.messageOutcome = DeleteOutcome.Success(1, undo)
        val logic = DeleteViewModelLogic(deleter)

        logic.requestDeleteMessage(4)
        logic.cancel()
        assertNull(logic.state.value.confirmMessageId)

        logic.requestDeleteMessage(4)
        logic.confirmMessage()

        assertEquals(DeleteNoticeKind.MessageDeleted, logic.state.value.notice?.kind)
        assertEquals(listOf(4L), deleter.messageDeletes)
    }

    @Test
    fun missingDefaultSmsRoleShowsAnActionableNoticeAndDoesNotDelete() = runTest {
        val deleter = FakeDeleter()
        deleter.threadOutcome = DeleteOutcome.RequiresDefaultSms
        val logic = DeleteViewModelLogic(deleter)

        logic.requestDeleteThread(3)
        logic.confirmThread()

        assertEquals(DeleteNoticeKind.RequiresDefaultSms, logic.state.value.notice?.kind)
        assertNull(logic.state.value.notice?.undo)
        assertEquals(listOf(3L), deleter.threadDeletes)
    }

    @Test
    fun providerFailureShowsAnErrorNotice() = runTest {
        val deleter = FakeDeleter()
        deleter.messageOutcome = DeleteOutcome.Failed("The message is still in the SMS provider")
        val logic = DeleteViewModelLogic(deleter)

        logic.requestDeleteMessage(8)
        logic.confirmMessage()

        assertEquals(DeleteNoticeKind.Failed, logic.state.value.notice?.kind)
    }

    @Test
    fun undoRestoresAndAFailedUndoShowsAnError() = runTest {
        val deleter = FakeDeleter()
        deleter.threadOutcome = DeleteOutcome.Success(1, DeleteUndo(listOf(sms(1, 9))))
        deleter.undoOk = false
        val logic = DeleteViewModelLogic(deleter)

        logic.requestDeleteThread(9)
        logic.confirmThread()
        val noticeId = logic.state.value.notice!!.id
        logic.undo()

        assertEquals(1, deleter.undos.size)
        assertEquals(DeleteNoticeKind.UndoFailed, logic.state.value.notice?.kind)
        logic.dismiss(noticeId)
        assertEquals(DeleteNoticeKind.UndoFailed, logic.state.value.notice?.kind)
        logic.dismiss(logic.state.value.notice!!.id)
        assertNull(logic.state.value.notice)
    }

    @Test
    fun confirmingAThreadDeleteRefreshesObservers() = runTest {
        val content = RecordingContent(
            messages = mutableListOf(
                sms(1, 7, "keep-me").copy(threadId = 7),
                sms(2, 7, "gone"),
            ),
        )
        content.messages.removeAll { it.threadId != 7L }
        content.messages += sms(3, 8, "other")
        val bus = SmsRefreshBus()
        val deletion = SmsDeletion(content, EmptyMeta(), bus)
        val logic = DeleteViewModelLogic(RepositoryDeleter(deletion))
        val sizes = mutableListOf<Int>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.signals.collect { sizes += content.messages.size }
        }

        logic.requestDeleteThread(7)
        logic.confirmThread()

        assertEquals(DeleteNoticeKind.ConversationDeleted, logic.state.value.notice?.kind)
        assertTrue(content.messages.none { it.threadId == 7L })
        assertEquals(listOf(8L), content.messages.map { it.threadId })
        assertEquals(listOf(1), sizes)
        job.cancel()
    }
}

private class FakeDeleter : SmsDeleter {
    var messageOutcome: DeleteOutcome = DeleteOutcome.Failed("unset")
    var threadOutcome: DeleteOutcome = DeleteOutcome.Failed("unset")
    var undoOk: Boolean = true
    val messageDeletes = mutableListOf<Long>()
    val threadDeletes = mutableListOf<Long>()
    val undos = mutableListOf<DeleteUndo>()

    override suspend fun deleteMessageNow(messageId: Long): DeleteOutcome {
        messageDeletes += messageId
        return messageOutcome
    }

    override suspend fun deleteThreadNow(threadId: Long): DeleteOutcome {
        threadDeletes += threadId
        return threadOutcome
    }

    override suspend fun undoDelete(undo: DeleteUndo): Boolean {
        undos += undo
        return undoOk
    }
}

private class RepositoryDeleter(private val deletion: SmsDeletion) : SmsDeleter {
    override suspend fun deleteMessageNow(messageId: Long) = deletion.deleteMessage(messageId)
    override suspend fun deleteThreadNow(threadId: Long) = deletion.deleteThread(threadId)
    override suspend fun undoDelete(undo: DeleteUndo) = deletion.undo(undo)
}

private class RecordingContent(
    val messages: MutableList<StoredSms>,
) : SmsContentSource {
    override fun canDeleteSms() = true
    override fun findMessage(messageId: Long) = messages.find { it.id == messageId }
    override fun findThread(threadId: Long) = messages.filter { it.threadId == threadId }
    override fun deleteMessage(messageId: Long): Int {
        val before = messages.size
        messages.removeAll { it.id == messageId }
        return before - messages.size
    }
    override fun deleteThread(threadId: Long): Int {
        val before = messages.size
        messages.removeAll { it.threadId == threadId }
        return before - messages.size
    }
    override fun insertMessage(message: StoredSms): Long = error("unused")
    override fun notifyChanged() = Unit
}

private class EmptyMeta : MessageMetaStore {
    override suspend fun metaFor(messageId: Long) = null
    override suspend fun deleteMessages(messageIds: List<Long>) = Unit
    override suspend fun deleteThread(threadId: Long) = Unit
    override suspend fun restoreMessage(messageId: Long, category: String, otpCode: String?) = Unit
}

private fun sms(id: Long, threadId: Long, body: String = "body") = StoredSms(
    id = id,
    threadId = threadId,
    address = "+15550001",
    body = body,
    dateMillis = 10L,
    type = 1,
    read = true,
)
