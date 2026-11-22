package com.ekhtiari.smsorganizer.ui.conversation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ekhtiari.smsorganizer.SmsOrganizerApp
import com.ekhtiari.smsorganizer.domain.SimInfo
import com.ekhtiari.smsorganizer.domain.SmsMessage
import com.ekhtiari.smsorganizer.ui.DeleteNotice
import com.ekhtiari.smsorganizer.ui.DeleteViewModelLogic
import com.ekhtiari.smsorganizer.util.ContactHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConversationUiState(
    val threadId: Long = 0L,
    val address: String = "",
    val displayName: String = "",
    val draft: String = "",
    val selectedSimId: Int? = null,
    val sims: List<SimInfo> = emptyList(),
    val sending: Boolean = false,
    val error: String? = null,
    val confirmDeleteMessageId: Long? = null,
    val deleteNotice: DeleteNotice? = null,
    val loading: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SmsOrganizerApp

    private val _ui = MutableStateFlow(ConversationUiState())
    private val deletion = DeleteViewModelLogic(app.smsRepository)
    val ui: StateFlow<ConversationUiState> = combine(_ui, deletion.state) { state, delete ->
        state.copy(
            confirmDeleteMessageId = delete.confirmMessageId,
            deleteNotice = delete.notice,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConversationUiState())

    private val threadIdFlow = MutableStateFlow(0L)

    val messages: StateFlow<List<SmsMessage>> =
        threadIdFlow.flatMapLatest { id ->
            if (id == 0L) flowOf(emptyList())
            else app.smsRepository.observeThread(id)
        }.onEach {
            if (_ui.value.loading) {
                _ui.value = _ui.value.copy(loading = false)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(threadId: Long, address: String) {
        val sims = app.simManager.getActiveSims()
        val defaultSim = sims.firstOrNull { it.isDefaultSms }?.subscriptionId
            ?: sims.firstOrNull()?.subscriptionId
        val name = ContactHelper.resolveDisplayName(getApplication(), address) ?: address
        _ui.value = _ui.value.copy(
            threadId = threadId,
            address = address,
            displayName = name,
            sims = sims,
            selectedSimId = defaultSim,
            loading = true,
            error = null,
        )
        threadIdFlow.value = threadId
        app.smsRepository.markThreadRead(threadId)
    }

    fun updateDraft(text: String) {
        _ui.value = _ui.value.copy(draft = text)
    }

    fun selectSim(subscriptionId: Int) {
        _ui.value = _ui.value.copy(selectedSimId = subscriptionId)
    }

    fun send() {
        val state = _ui.value
        val body = state.draft.trim()
        if (body.isEmpty() || state.address.isBlank()) return

        viewModelScope.launch {
            _ui.value = state.copy(sending = true, error = null)
            val ok = app.smsSender.sendText(
                address = state.address,
                body = body,
                subscriptionId = state.selectedSimId,
                threadId = state.threadId,
            )
            _ui.value = if (ok) {
                state.copy(draft = "", sending = false)
            } else {
                state.copy(
                    sending = false,
                    error = "Failed to send. Check SMS permission / default app.",
                )
            }
        }
    }

    fun requestDeleteMessage(messageId: Long) = deletion.requestDeleteMessage(messageId)

    fun cancelDeleteMessage() = deletion.cancel()

    fun confirmDeleteMessage() {
        viewModelScope.launch { deletion.confirmMessage() }
    }

    fun undoDelete() {
        viewModelScope.launch { deletion.undo() }
    }

    fun dismissDeleteNotice(id: Long) = deletion.dismiss(id)
}
