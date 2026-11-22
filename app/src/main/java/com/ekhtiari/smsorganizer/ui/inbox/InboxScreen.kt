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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.domain.Conversation
import com.ekhtiari.smsorganizer.domain.MessageCategory
import com.ekhtiari.smsorganizer.ui.components.AvatarCircle
import com.ekhtiari.smsorganizer.ui.components.CategoryPill
import com.ekhtiari.smsorganizer.ui.components.EmptyState
import com.ekhtiari.smsorganizer.ui.components.formatRelativeTime
import com.ekhtiari.smsorganizer.ui.components.rememberUiHaptics
import com.ekhtiari.smsorganizer.ui.components.warmFieldColors
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

    if (ui.confirmDeleteThreadId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteConversation,
            title = { Text(stringResource(R.string.delete_conversation)) },
            text = { Text(stringResource(R.string.delete_conversation_confirm)) },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDeleteConversation,
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
                TextButton(onClick = viewModel::cancelDeleteConversation) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    InboxContent(
        ui = ui,
        conversations = conversations,
        onSearch = viewModel::setSearch,
        onCategory = viewModel::setCategory,
        onOpenThread = onOpenThread,
        onCompose = onCompose,
        onSettings = onSettings,
        onArchive = viewModel::archiveConversation,
        onDelete = viewModel::requestDeleteConversation,
        onCopyOtp = { code ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("OTP", code))
            Toast.makeText(context, R.string.otp_copied, Toast.LENGTH_SHORT).show()
        },
        onRequestDefaultSms = onRequestDefaultSms,
        onRequestPermissions = onRequestPermissions,
        onOpenAppInfo = onOpenAppInfo,
        onOpenDefaultApps = onOpenDefaultApps,
        onDismissPermission = viewModel::dismissPermissionBanner,
        onDismissDefaultSms = viewModel::dismissDefaultSmsBanner,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxContent(
    ui: InboxUiState,
    conversations: List<Conversation>,
    onSearch: (String) -> Unit,
    onCategory: (MessageCategory?) -> Unit,
    onOpenThread: (threadId: Long, address: String) -> Unit,
    onCompose: () -> Unit,
    onSettings: () -> Unit,
    onArchive: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onCopyOtp: (String) -> Unit,
    onRequestDefaultSms: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenDefaultApps: () -> Unit,
    onDismissPermission: () -> Unit,
    onDismissDefaultSms: () -> Unit,
) {
    val haptics = rememberUiHaptics()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BoxBrand()
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptics.click()
                        onSettings()
                    }) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.click()
                    onCompose()
                },
                icon = {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = null,
                    )
                },
                text = { Text(stringResource(R.string.new_message)) },
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 6.dp,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedVisibility(
                visible = !ui.hasPermissions && !ui.permissionBannerDismissed,
                enter = SoftMotion.fadeSlideUp(),
                exit = SoftMotion.fadeSlideDown(),
            ) {
                Banner(
                    icon = Icons.Outlined.Sms,
                    text = stringResource(R.string.permission_required),
                    action = stringResource(R.string.grant),
                    onAction = onRequestPermissions,
                    onClose = onDismissPermission,
                )
            }

            AnimatedVisibility(
                visible = ui.hasPermissions && !ui.isDefaultSms && !ui.defaultSmsBannerDismissed,
                enter = SoftMotion.fadeSlideUp(),
                exit = SoftMotion.fadeSlideDown(),
            ) {
                Banner(
                    icon = Icons.Outlined.Info,
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
                    onClose = onDismissDefaultSms,
                )
            }

            AnimatedVisibility(visible = ui.sims.size > 1) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.SimCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = ui.sims.joinToString("  ·  ") { it.label },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            TextField(
                value = ui.searchQuery,
                onValueChange = onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .animateContentSize(animationSpec = SoftMotion.tweenSoft()),
                placeholder = { Text(stringResource(R.string.search)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = if (ui.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearch("") }) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                colors = warmFieldColors(),
                shape = MaterialTheme.shapes.extraLarge,
            )

            CategoryTabs(
                selected = ui.category,
                onSelect = { category ->
                    haptics.tick()
                    onCategory(category)
                },
            )

            AnimatedContent(
                targetState = ui.category,
                transitionSpec = SoftMotion.tabContentTransform(
                    indexOf = ::categoryIndex,
                    rtl = rtl,
                ),
                label = "inboxTabContent",
                modifier = Modifier.weight(1f),
            ) {
                if (conversations.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.no_conversations),
                        hint = stringResource(R.string.empty_conversations_hint),
                        icon = Icons.AutoMirrored.Outlined.Chat,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 104.dp),
                    ) {
                        itemsIndexed(
                            items = conversations,
                            key = { _, item -> item.threadId },
                        ) { index, conversation ->
                            Column(
                                modifier = Modifier.animateItem(
                                    fadeInSpec = SoftMotion.tweenSoft(
                                        SoftMotion.FastMs + index.coerceAtMost(8) * SoftMotion.StaggerMs,
                                    ),
                                    fadeOutSpec = SoftMotion.tweenSoft(SoftMotion.FastMs),
                                    placementSpec = SoftMotion.tweenSoft(),
                                ),
                            ) {
                                ConversationRow(
                                    conversation = conversation,
                                    onClick = {
                                        haptics.click()
                                        onOpenThread(conversation.threadId, conversation.address)
                                    },
                                    onArchive = { onArchive(conversation.threadId) },
                                    onDelete = { onDelete(conversation.threadId) },
                                    onCopyOtp = onCopyOtp,
                                )
                                if (index != conversations.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxBrand() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Forum,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun Banner(
    icon: ImageVector,
    text: String,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    tertiaryAction: String? = null,
    onTertiaryAction: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(animationSpec = SoftMotion.tweenSoft()),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(20.dp),
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, end = 4.dp),
                )
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.Start) {
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
}

private data class CategoryTab(
    val labelRes: Int,
    val category: MessageCategory?,
    val icon: ImageVector,
)

private val CategoryTabsSpec = listOf(
    CategoryTab(R.string.category_all, null, Icons.AutoMirrored.Outlined.Chat),
    CategoryTab(R.string.category_personal, MessageCategory.PERSONAL, Icons.Outlined.Person),
    CategoryTab(R.string.category_transactions, MessageCategory.TRANSACTIONS, Icons.Outlined.AccountBalance),
    CategoryTab(R.string.category_offers, MessageCategory.OFFERS, Icons.Outlined.LocalOffer),
    CategoryTab(R.string.category_otp, MessageCategory.OTP, Icons.Outlined.Password),
)

private fun categoryIndex(category: MessageCategory?): Int =
    CategoryTabsSpec.indexOfFirst { it.category == category }.coerceAtLeast(0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTabs(
    selected: MessageCategory?,
    onSelect: (MessageCategory?) -> Unit,
) {
    val selectedIndex = categoryIndex(selected)
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 12.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        },
    ) {
        CategoryTabsSpec.forEachIndexed { index, tab ->
            val isSelected = selectedIndex == index
            Tab(
                selected = isSelected,
                onClick = { onSelect(tab.category) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(tab.labelRes),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onCopyOtp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val unread = conversation.unreadCount > 0
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (unread) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .animateContentSize(animationSpec = SoftMotion.tweenSoft(SoftMotion.FastMs)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarCircle(
            name = conversation.displayName,
            modifier = Modifier.padding(start = 8.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedVisibility(
                    visible = unread,
                    enter = fadeIn(SoftMotion.tweenSoft(SoftMotion.FastMs)),
                    exit = fadeOut(SoftMotion.tweenSoft(SoftMotion.FastMs)),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(conversation.unreadCount.coerceAtMost(99).toString())
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Text(
                    text = formatRelativeTime(conversation.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unread) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = conversation.snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (unread) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CategoryPill(conversation.category)
                if (conversation.subscriptionId >= 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            Icons.Outlined.SimCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "SIM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                conversation.otpCode?.let { code ->
                    TextButton(
                        onClick = { onCopyOtp(code) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.copy_otp) + " $code")
                    }
                }
            }
        }
        IconButton(onClick = onArchive) {
            Icon(
                Icons.Outlined.Archive,
                contentDescription = stringResource(R.string.archive),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
