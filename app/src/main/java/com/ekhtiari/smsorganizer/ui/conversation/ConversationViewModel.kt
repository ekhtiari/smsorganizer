package com.ekhtiari.smsorganizer.ui.conversation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ekhtiari.smsorganizer.SmsOrganizerApp
import com.ekhtiari.smsorganizer.domain.SimInfo
import com.ekhtiari.smsorganizer.domain.SmsMessage
import com.ekhtiari.smsorganizer.util.ContactHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
)

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SmsOrganizerApp

    private val _ui = MutableStateFlow(ConversationUiState())
    val ui: StateFlow<ConversationUiState> = _ui.asStateFlow()

    private val threadIdFlow = MutableStateFlow(0L)

    val messages: StateFlow<List<SmsMessage>> =
        threadIdFlow.flatMapLatest { id ->
            if (id == 0L) flowOf(emptyList())
            else app.smsRepository.observeThread(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(threadId: Long, address: String) {
        val sims = app.simManager.getActiveSims()
        val defaultSim = sims.firstOrNull { it.isDefaultSms }?.subscriptionId
            ?: sims.firstOrNull()?.subscriptionId
        val name = ContactHelper.resolveDisplayName(getApplication(), address) ?: address
        threadIdFlow.value = threadId
        _ui.value = _ui.value.copy(
            threadId = threadId,
            address = address,
            displayName = name,
            sims = sims,
            selectedSimId = defaultSim,
        )
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

    fun requestDeleteMessage(messageId: Long) {
        _ui.value = _ui.value.copy(confirmDeleteMessageId = messageId)
    }

    fun cancelDeleteMessage() {
        _ui.value = _ui.value.copy(confirmDeleteMessageId = null)
    }

    fun confirmDeleteMessage() {
        val messageId = _ui.value.confirmDeleteMessageId ?: return
        _ui.value = _ui.value.copy(confirmDeleteMessageId = null)
        viewModelScope.launch {
            app.smsRepository.deleteMessageNow(messageId)
        }
    }
}
