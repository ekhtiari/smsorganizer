package com.ekhtiari.smsorganizer.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.domain.MessageCategory
import com.ekhtiari.smsorganizer.domain.SmsMessage
import com.ekhtiari.smsorganizer.ui.components.formatMessageTime
import com.ekhtiari.smsorganizer.ui.theme.SoftMotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    threadId: Long,
    address: String,
    onBack: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val sendEnabled = ui.draft.isNotBlank() && !ui.sending
    val sendScale by animateFloatAsState(
        targetValue = if (sendEnabled) 1f else 0.92f,
        animationSpec = SoftMotion.tweenSoft(SoftMotion.FastMs),
        label = "sendScale",
    )

    LaunchedEffect(threadId, address) {
        viewModel.load(threadId, address)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    if (ui.confirmDeleteMessageId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteMessage,
            title = { Text(stringResource(R.string.delete_message)) },
            text = { Text(stringResource(R.string.delete_message_confirm)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteMessage) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteMessage) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(ui.displayName.ifBlank { address })
                        if (ui.address.isNotBlank() && ui.displayName != ui.address) {
                            Text(
                                text = ui.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        // Handle IME with imePadding() so the composer stays above the keyboard
        // under edge-to-edge (Scaffold safeDrawing alone is unreliable here).
        contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onCopyOtp = { code ->
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("OTP", code))
                            Toast.makeText(context, R.string.otp_copied, Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { viewModel.requestDeleteMessage(message.id) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = SoftMotion.tweenSoft(SoftMotion.FastMs),
                            fadeOutSpec = SoftMotion.tweenSoft(SoftMotion.FastMs),
                            placementSpec = SoftMotion.tweenSoft(),
                        ),
                    )
                }
            }

            AnimatedVisibility(
                visible = ui.sims.size > 1,
                enter = SoftMotion.fadeSlideUp(),
                exit = SoftMotion.fadeSlideDown(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ui.sims.forEach { sim ->
                        FilterChip(
                            selected = ui.selectedSimId == sim.subscriptionId,
                            onClick = { viewModel.selectSim(sim.subscriptionId) },
                            label = { Text(sim.label) },
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = ui.error != null,
                enter = SoftMotion.fadeSlideUp(),
                exit = SoftMotion.fadeSlideDown(),
            ) {
                Text(
                    text = ui.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .animateContentSize(animationSpec = SoftMotion.tweenSoft(SoftMotion.FastMs)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextField(
                        value = ui.draft,
                        onValueChange = viewModel::updateDraft,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.type_message)) },
                        maxLines = 4,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        shape = RoundedCornerShape(20.dp),
                    )
                    IconButton(
                        onClick = viewModel::send,
                        enabled = sendEnabled,
                        modifier = Modifier.scale(sendScale),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send),
                            tint = if (sendEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: SmsMessage,
    onCopyOtp: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outgoing = message.type == SmsMessage.TYPE_SENT ||
        message.type == SmsMessage.TYPE_OUTBOX ||
        message.type == SmsMessage.TYPE_QUEUED
    val onBubble = if (outgoing) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (outgoing) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_message),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                )
            }
        }
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    color = if (outgoing) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (outgoing) 18.dp else 4.dp,
                        bottomEnd = if (outgoing) 4.dp else 18.dp,
                    ),
                )
                .padding(12.dp)
                .animateContentSize(animationSpec = SoftMotion.tweenSoft(SoftMotion.FastMs)),
        ) {
            Text(
                text = message.body,
                color = onBubble,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatMessageTime(message.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (outgoing) onBubble.copy(alpha = 0.8f) else onBubble,
                )
                if (message.subscriptionId >= 0) {
                    Text(
                        text = " · SIM",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (outgoing) onBubble.copy(alpha = 0.8f) else onBubble,
                    )
                }
            }
            AnimatedVisibility(
                visible = message.category == MessageCategory.OTP && message.otpCode != null,
                enter = fadeIn(SoftMotion.tweenSoft(SoftMotion.FastMs)) +
                    scaleIn(animationSpec = SoftMotion.tweenSoft(SoftMotion.FastMs), initialScale = 0.94f),
                exit = fadeOut(SoftMotion.tweenSoft(SoftMotion.FastMs)),
            ) {
                TextButton(onClick = { message.otpCode?.let(onCopyOtp) }) {
                    Text(stringResource(R.string.copy_otp) + " ${message.otpCode}")
                }
            }
        }
        if (!outgoing) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_message),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                )
            }
        }
    }
}
