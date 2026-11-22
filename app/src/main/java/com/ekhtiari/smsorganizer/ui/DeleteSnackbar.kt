package com.ekhtiari.smsorganizer.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.ekhtiari.smsorganizer.R

@Composable
fun DeleteSnackbarEffect(
    notice: DeleteNotice?,
    snackbarHostState: SnackbarHostState,
    onUndo: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    onDismiss: (Long) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(notice?.id) {
        val current = notice ?: return@LaunchedEffect
        val message = when (current.kind) {
            DeleteNoticeKind.MessageDeleted -> context.getString(R.string.delete_message_success)
            DeleteNoticeKind.ConversationDeleted -> context.getString(R.string.delete_conversation_success)
            DeleteNoticeKind.RequiresDefaultSms -> context.getString(R.string.delete_requires_default_sms)
            DeleteNoticeKind.Failed -> context.getString(R.string.delete_failed)
            DeleteNoticeKind.UndoFailed -> context.getString(R.string.delete_undo_failed)
        }
        val action = when {
            current.undo != null -> context.getString(R.string.undo)
            current.kind == DeleteNoticeKind.RequiresDefaultSms ->
                context.getString(R.string.set_default_sms)
            else -> null
        }
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = action,
            duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            when {
                current.undo != null -> onUndo()
                current.kind == DeleteNoticeKind.RequiresDefaultSms -> onRequestDefaultSms()
            }
        }
        onDismiss(current.id)
    }
}
