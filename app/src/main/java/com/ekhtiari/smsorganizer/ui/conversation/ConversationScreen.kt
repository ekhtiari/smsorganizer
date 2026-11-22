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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.domain.MessageCategory
import com.ekhtiari.smsorganizer.domain.SimInfo
import com.ekhtiari.smsorganizer.domain.SmsMessage
import com.ekhtiari.smsorganizer.ui.components.AvatarCircle
import com.ekhtiari.smsorganizer.ui.components.formatMessageTime
import com.ekhtiari.smsorganizer.ui.components.rememberUiHaptics
import com.ekhtiari.smsorganizer.ui.components.warmFieldColors
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
    val context = LocalContext.current

    LaunchedEffect(threadId, address) {
        viewModel.load(threadId, address)
    }

    if (ui.confirmDeleteMessageId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteMessage,
            title = { Text(stringResource(R.string.delete_message)) },
            text = { Text(stringResource(R.string.delete_message_confirm)) },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDeleteMessage,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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

    ConversationContent(
        ui = ui,
        messages = messages,
        onBack = onBack,
        onDraftChange = viewModel::updateDraft,
        onSelectSim = viewModel::selectSim,
        onSend = viewModel::send,
        onDelete = viewModel::requestDeleteMessage,
        onCopyOtp = { code ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("OTP", code))
            Toast.makeText(context, R.string.otp_copied, Toast.LENGTH_SHORT).show()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationContent(
    ui: ConversationUiState,
    messages: List<SmsMessage>,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSelectSim: (Int) -> Unit,
    onSend: () -> Unit,
    onDelete: (Long) -> Unit,
    onCopyOtp: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val haptics = rememberUiHaptics()
    val sendEnabled = ui.draft.isNotBlank() && !ui.sending
    val sendScale by animateFloatAsState(
        targetValue = if (sendEnabled) 1f else 0.92f,
        animationSpec = SoftMotion.tweenSoft(SoftMotion.FastMs),
        label = "sendScale",
    )
    val title = ui.displayName.ifBlank { ui.address }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarCircle(name = title.ifBlank { "?" }, size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (ui.address.isNotBlank() && ui.displayName != ui.address) {
                                Text(
                                    text = ui.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            ) {
                itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                    val group = bubbleGrouping(messages, index)
                    MessageBubble(
                        message = message,
                        firstInGroup = group.first,
                        lastInGroup = group.second,
                        onCopyOtp = onCopyOtp,
                        onDelete = { onDelete(message.id) },
                        modifier = Modifier
                            .padding(top = if (group.first && index > 0) 10.dp else 2.dp)
                            .animateItem(
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
                SimPicker(
                    sims = ui.sims,
                    selectedId = ui.selectedSimId,
                    onSelect = { id ->
                        haptics.tick()
                        onSelectSim(id)
                    },
                )
            }

            AnimatedVisibility(
                visible = ui.error != null,
                enter = SoftMotion.fadeSlideUp(),
                exit = SoftMotion.fadeSlideDown(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = ui.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .animateContentSize(animationSpec = SoftMotion.tweenSoft(SoftMotion.FastMs)),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    TextField(
                        value = ui.draft,
                        onValueChange = onDraftChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.type_message)) },
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Content,
                        ),
                        colors = warmFieldColors(),
                        shape = RoundedCornerShape(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            haptics.confirm()
                            onSend()
                        },
                        enabled = sendEnabled,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .scale(sendScale),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimPicker(
    sims: List<SimInfo>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sims.forEach { sim ->
            val selected = selectedId == sim.subscriptionId
            FilterChip(
                selected = selected,
                onClick = { onSelect(sim.subscriptionId) },
                label = { Text(sim.label) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.SimCard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                shape = RoundedCornerShape(50),
            )
        }
    }
}

private fun bubbleGrouping(messages: List<SmsMessage>, index: Int): Pair<Boolean, Boolean> {
    val message = messages[index]
    val previous = messages.getOrNull(index - 1)
    val next = messages.getOrNull(index + 1)
    val gapMs = 5 * 60_000L
    val first = previous == null ||
        previous.isOutgoing != message.isOutgoing ||
        message.date - previous.date > gapMs
    val last = next == null ||
        next.isOutgoing != message.isOutgoing ||
        next.date - message.date > gapMs
    return first to last
}

private fun chatBubbleShape(outgoing: Boolean, first: Boolean, last: Boolean): RoundedCornerShape {
    val large = 22.dp
    val tight = 8.dp
    val tail = 6.dp
    return RoundedCornerShape(
        topStart = if (!outgoing && !first) tight else large,
        topEnd = if (outgoing && !first) tight else large,
        bottomStart = when {
            !outgoing && last -> tail
            !outgoing -> tight
            else -> large
        },
        bottomEnd = when {
            outgoing && last -> tail
            outgoing -> tight
            else -> large
        },
    )
}

@Composable
private fun MessageBubble(
    message: SmsMessage,
    firstInGroup: Boolean,
    lastInGroup: Boolean,
    onCopyOtp: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outgoing = message.isOutgoing
    val dark = isSystemInDarkTheme()
    val bubbleColor = when {
        outgoing && dark -> MaterialTheme.colorScheme.primaryContainer
        outgoing -> MaterialTheme.colorScheme.primary
        dark -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val onBubble = when {
        outgoing && dark -> MaterialTheme.colorScheme.onPrimaryContainer
        outgoing -> MaterialTheme.colorScheme.onPrimary
        dark -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val shape = chatBubbleShape(outgoing, firstInGroup, lastInGroup)
    val meta = onBubble.copy(alpha = 0.78f)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (outgoing) {
            DeleteMessageButton(onDelete)
        }
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .animateContentSize(animationSpec = SoftMotion.tweenSoft(SoftMotion.FastMs))
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.body,
                color = onBubble,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDirection = TextDirection.Content,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = formatMessageTime(message.date),
                    style = MaterialTheme.typography.labelSmall.copy(
                        textDirection = TextDirection.Content,
                    ),
                    color = meta,
                )
                StatusIcon(message.type, tint = meta)
                if (message.subscriptionId >= 0) {
                    Icon(
                        Icons.Outlined.SimCard,
                        contentDescription = null,
                        tint = meta,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = "SIM",
                        style = MaterialTheme.typography.labelSmall,
                        color = meta,
                    )
                }
            }
            AnimatedVisibility(
                visible = message.category == MessageCategory.OTP && message.otpCode != null,
                enter = fadeIn(SoftMotion.tweenSoft(SoftMotion.FastMs)) +
                    scaleIn(
                        animationSpec = SoftMotion.tweenSoft(SoftMotion.FastMs),
                        initialScale = 0.94f,
                    ),
                exit = fadeOut(SoftMotion.tweenSoft(SoftMotion.FastMs)),
            ) {
                TextButton(
                    onClick = { message.otpCode?.let(onCopyOtp) },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = onBubble),
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.copy_otp) + " ${message.otpCode}")
                }
            }
        }
        if (!outgoing) {
            DeleteMessageButton(onDelete)
        }
    }
}

@Composable
private fun DeleteMessageButton(onDelete: () -> Unit) {
    IconButton(
        onClick = onDelete,
        modifier = Modifier.size(36.dp),
    ) {
        Icon(
            Icons.Outlined.Delete,
            contentDescription = stringResource(R.string.delete_message),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun StatusIcon(type: Int, tint: androidx.compose.ui.graphics.Color) {
    val icon: ImageVector? = when (type) {
        SmsMessage.TYPE_SENT -> Icons.Outlined.Done
        SmsMessage.TYPE_OUTBOX, SmsMessage.TYPE_QUEUED -> Icons.Outlined.Schedule
        SmsMessage.TYPE_FAILED -> Icons.Outlined.ErrorOutline
        else -> null
    }
    if (icon != null) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
    }
}
