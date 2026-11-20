package com.ekhtiari.smsorganizer.ui.inbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.domain.Conversation
import com.ekhtiari.smsorganizer.domain.MessageCategory
import com.ekhtiari.smsorganizer.ui.components.AvatarCircle
import com.ekhtiari.smsorganizer.ui.components.categoryChipLabel
import com.ekhtiari.smsorganizer.ui.components.formatRelativeTime
import com.ekhtiari.smsorganizer.ui.theme.SoftMotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onOpenThread: (threadId: Long, address: String) -> Unit,
    onCompose: () -> Unit,
    onSettings: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenDefaultApps: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.refreshStatus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCompose,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp,
                ),
            ) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.new_message))
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedVisibility(
                visible = !ui.hasPermissions,
                enter = SoftMotion.fadeSlideUp(),
                exit = SoftMotion.fadeSlideDown(),
            ) {
                Banner(
                    text = stringResource(R.string.permission_required),
                    action = "Grant",
                    onAction = onRequestPermissions,
                )
            }

            AnimatedVisibility(
                visible = ui.hasPermissions && !ui.isDefaultSms,
                enter = SoftMotion.fadeSlideUp(),
                exit = SoftMotion.fadeSlideDown(),
            ) {
                Banner(
                    text = if (ui.defaultSmsRequestDenied || !ui.smsRoleAvailable) {
                        stringResource(R.string.default_sms_denied_help)
                    } else {
                        stringResource(R.string.default_sms_prompt)
                    },
                    action = stringResource(R.string.set_default_sms),
                    onAction = onRequestDefaultSms,
                    secondaryAction = stringResource(R.string.open_app_info),
                    onSecondaryAction = onOpenAppInfo,
                    tertiaryAction = stringResource(R.string.open_default_apps),
                    onTertiaryAction = onOpenDefaultApps,
                )
            }

            AnimatedVisibility(visible = ui.sims.size > 1) {
                Text(
                    text = ui.sims.joinToString("  ·  ") { it.label },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            TextField(
                value = ui.searchQuery,
                onValueChange = viewModel::setSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .animateContentSize(animationSpec = SoftMotion.tweenSoft()),
                placeholder = { Text(stringResource(R.string.search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = MaterialTheme.shapes.large,
            )

            CategoryTabs(
                selected = ui.category,
                onSelect = viewModel::setCategory,
            )

            AnimatedContent(
                targetState = conversations.isEmpty(),
                transitionSpec = SoftMotion.tabContentTransform(),
                label = "inboxContent",
                modifier = Modifier.weight(1f),
            ) { empty ->
                if (empty) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.no_conversations),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp),
                    ) {
                        itemsIndexed(
                            items = conversations,
                            key = { _, item -> item.threadId },
                        ) { index, conversation ->
                            ConversationRow(
                                conversation = conversation,
                                onClick = { onOpenThread(conversation.threadId, conversation.address) },
                                onCopyOtp = { code ->
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("OTP", code))
                                    Toast.makeText(context, R.string.otp_copied, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = SoftMotion.tweenSoft(
                                        SoftMotion.FastMs + index.coerceAtMost(8) * SoftMotion.StaggerMs,
                                    ),
                                    fadeOutSpec = SoftMotion.tweenSoft(SoftMotion.FastMs),
                                    placementSpec = SoftMotion.tweenSoft(),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Banner(
    text: String,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    tertiaryAction: String? = null,
    onTertiaryAction: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(animationSpec = SoftMotion.tweenSoft()),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            TextButton(onClick = onAction) { Text(action) }
            if (secondaryAction != null && onSecondaryAction != null) {
                TextButton(onClick = onSecondaryAction) { Text(secondaryAction) }
            }
            if (tertiaryAction != null && onTertiaryAction != null) {
                TextButton(onClick = onTertiaryAction) { Text(tertiaryAction) }
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    selected: MessageCategory?,
    onSelect: (MessageCategory?) -> Unit,
) {
    val tabs = listOf<Pair<String, MessageCategory?>>(
        "All" to null,
        MessageCategory.PERSONAL.displayName to MessageCategory.PERSONAL,
        MessageCategory.TRANSACTIONS.displayName to MessageCategory.TRANSACTIONS,
        MessageCategory.OFFERS.displayName to MessageCategory.OFFERS,
        MessageCategory.OTP.displayName to MessageCategory.OTP,
    )
    val selectedIndex = tabs.indexOfFirst { it.second == selected }.coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 12.dp,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        tabs.forEachIndexed { index, (label, category) ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onSelect(category) },
                text = {
                    Text(
                        text = label,
                        fontWeight = if (selectedIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
            )
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit,
    onCopyOtp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarCircle(name = conversation.displayName)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                AnimatedVisibility(
                    visible = conversation.unreadCount > 0,
                    enter = fadeIn(SoftMotion.tweenSoft(SoftMotion.FastMs)),
                    exit = fadeOut(SoftMotion.tweenSoft(SoftMotion.FastMs)),
                ) {
                    Row {
                        Text(
                            text = "${conversation.unreadCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Text(
                    text = formatRelativeTime(conversation.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = conversation.snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = { Text(categoryChipLabel(conversation.category)) },
                    enabled = false,
                )
                if (conversation.subscriptionId >= 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SIM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                conversation.otpCode?.let { code ->
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onCopyOtp(code) }) {
                        Text(stringResource(R.string.copy_otp) + " $code")
                    }
                }
            }
        }
    }
}
