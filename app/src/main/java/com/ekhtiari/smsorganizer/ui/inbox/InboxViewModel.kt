package com.ekhtiari.smsorganizer.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ekhtiari.smsorganizer.SmsOrganizerApp
import com.ekhtiari.smsorganizer.domain.Conversation
import com.ekhtiari.smsorganizer.domain.MessageCategory
import com.ekhtiari.smsorganizer.domain.SimInfo
import com.ekhtiari.smsorganizer.util.SmsPermissions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InboxUiState(
    val category: MessageCategory? = null,
    val searchQuery: String = "",
    val isDefaultSms: Boolean = false,
    val hasPermissions: Boolean = false,
    val smsRoleAvailable: Boolean = true,
    val defaultSmsRequestDenied: Boolean = false,
    val permissionBannerDismissed: Boolean = false,
    val defaultSmsBannerDismissed: Boolean = false,
    val sims: List<SimInfo> = emptyList(),
    val confirmDeleteThreadId: Long? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SmsOrganizerApp

    private val _ui = MutableStateFlow(InboxUiState())
    val ui: StateFlow<InboxUiState> = _ui.asStateFlow()

    private val categoryFilter = MutableStateFlow<MessageCategory?>(null)
    private val searchQuery = MutableStateFlow("")

    val conversations: StateFlow<List<Conversation>> =
        searchQuery.flatMapLatest { query ->
            if (query.isNotBlank()) app.smsRepository.search(query)
            else categoryFilter.flatMapLatest { cat ->
                app.smsRepository.observeConversations(cat)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        val ctx = getApplication<Application>()
        val isDefault = SmsPermissions.isDefaultSmsApp(ctx)
        val hasPermissions = SmsPermissions.hasAll(ctx)
        _ui.value = _ui.value.copy(
            isDefaultSms = isDefault,
            hasPermissions = hasPermissions,
            smsRoleAvailable = SmsPermissions.isSmsRoleAvailable(ctx),
            defaultSmsRequestDenied = if (isDefault) false else _ui.value.defaultSmsRequestDenied,
            permissionBannerDismissed = if (hasPermissions) false else _ui.value.permissionBannerDismissed,
            defaultSmsBannerDismissed = if (isDefault) false else _ui.value.defaultSmsBannerDismissed,
            sims = app.simManager.getActiveSims(),
            category = categoryFilter.value,
            searchQuery = searchQuery.value,
        )
    }

    fun markDefaultSmsRequestDenied() {
        if (!_ui.value.isDefaultSms) {
            _ui.value = _ui.value.copy(defaultSmsRequestDenied = true)
        }
    }

    fun dismissPermissionBanner() {
        _ui.value = _ui.value.copy(permissionBannerDismissed = true)
    }

    fun dismissDefaultSmsBanner() {
        _ui.value = _ui.value.copy(defaultSmsBannerDismissed = true)
    }

    fun setCategory(category: MessageCategory?) {
        categoryFilter.value = category
        _ui.value = _ui.value.copy(category = category)
    }

    fun setSearch(query: String) {
        searchQuery.value = query
        _ui.value = _ui.value.copy(searchQuery = query)
    }

    fun requestDeleteConversation(threadId: Long) {
        _ui.value = _ui.value.copy(confirmDeleteThreadId = threadId)
    }

    fun cancelDeleteConversation() {
        _ui.value = _ui.value.copy(confirmDeleteThreadId = null)
    }

    fun confirmDeleteConversation() {
        val threadId = _ui.value.confirmDeleteThreadId ?: return
        _ui.value = _ui.value.copy(confirmDeleteThreadId = null)
        viewModelScope.launch {
            app.smsRepository.deleteThreadNow(threadId)
        }
    }

    fun archiveConversation(threadId: Long) {
        viewModelScope.launch {
            app.smsRepository.archiveThread(threadId)
        }
    }

    fun reclassify() {
        viewModelScope.launch {
            app.smsRepository.classifyMissingMessages(1000)
        }
    }
}
