package com.ekhtiari.smsorganizer.ui

import com.ekhtiari.smsorganizer.data.repository.DeleteOutcome
import com.ekhtiari.smsorganizer.data.repository.DeleteUndo
import com.ekhtiari.smsorganizer.data.repository.SmsDeleter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DeleteNoticeKind {
    MessageDeleted,
    ConversationDeleted,
    RequiresDefaultSms,
    Failed,
    UndoFailed,
}

data class DeleteNotice(
    val id: Long,
    val kind: DeleteNoticeKind,
    val undo: DeleteUndo? = null,
)

data class DeleteUiState(
    val confirmThreadId: Long? = null,
    val confirmMessageId: Long? = null,
    val notice: DeleteNotice? = null,
)

/**
 * Confirm / delete / undo / snackbar state shared by the inbox and conversation screens.
 * Kept off [androidx.lifecycle.AndroidViewModel] so unit tests can run it without Android.
 */
class DeleteViewModelLogic(
    private val deleter: SmsDeleter,
) {
    private val stateInternal = MutableStateFlow(DeleteUiState())
    val state: StateFlow<DeleteUiState> = stateInternal.asStateFlow()
    private var nextId = 1L

    fun requestDeleteThread(threadId: Long) {
        stateInternal.update { it.copy(confirmThreadId = threadId) }
    }

    fun requestDeleteMessage(messageId: Long) {
        stateInternal.update { it.copy(confirmMessageId = messageId) }
    }

    fun cancel() {
        stateInternal.update { it.copy(confirmThreadId = null, confirmMessageId = null) }
    }

    suspend fun confirmThread() {
        val threadId = stateInternal.value.confirmThreadId ?: return
        stateInternal.update { it.copy(confirmThreadId = null) }
        publish(deleter.deleteThreadNow(threadId), conversation = true)
    }

    suspend fun confirmMessage() {
        val messageId = stateInternal.value.confirmMessageId ?: return
        stateInternal.update { it.copy(confirmMessageId = null) }
        publish(deleter.deleteMessageNow(messageId), conversation = false)
    }

    suspend fun undo() {
        val undo = stateInternal.value.notice?.undo ?: return
        stateInternal.update { it.copy(notice = null) }
        val restored = deleter.undoDelete(undo)
        if (!restored) {
            stateInternal.update {
                it.copy(notice = DeleteNotice(id = nextId++, kind = DeleteNoticeKind.UndoFailed))
            }
        }
    }

    fun dismiss(id: Long) {
        stateInternal.update { current ->
            if (current.notice?.id == id) current.copy(notice = null) else current
        }
    }

    private fun publish(outcome: DeleteOutcome, conversation: Boolean) {
        val notice = when (outcome) {
            is DeleteOutcome.Success -> DeleteNotice(
                id = nextId++,
                kind = if (conversation) {
                    DeleteNoticeKind.ConversationDeleted
                } else {
                    DeleteNoticeKind.MessageDeleted
                },
                undo = outcome.undo.takeIf { it.messages.isNotEmpty() },
            )
            DeleteOutcome.RequiresDefaultSms -> DeleteNotice(
                id = nextId++,
                kind = DeleteNoticeKind.RequiresDefaultSms,
            )
            is DeleteOutcome.Failed -> DeleteNotice(
                id = nextId++,
                kind = DeleteNoticeKind.Failed,
            )
        }
        stateInternal.update { it.copy(notice = notice) }
    }
}
