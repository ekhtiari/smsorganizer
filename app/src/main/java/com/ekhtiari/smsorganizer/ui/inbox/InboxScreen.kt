package com.ekhtiari.smsorganizer.ui.inbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.domain.Conversation
import com.ekhtiari.smsorganizer.ui.DeleteSnackbarEffect
import com.ekhtiari.smsorganizer.domain.MessageCategory
import com.ekhtiari.smsorganizer.ui.components.AvatarCircle
import com.ekhtiari.smsorganizer.ui.components.CategoryPill
import com.ekhtiari.smsorganizer.ui.components.CopyCodeChip
import com.ekhtiari.smsorganizer.ui.components.EmptyState
import com.ekhtiari.smsorganizer.ui.components.ExpressiveDialogIcon
import com.ekhtiari.smsorganizer.ui.components.LoadingState
import com.ekhtiari.smsorganizer.ui.components.expressivePress
import com.ekhtiari.smsorganizer.ui.components.formatRelativeTime
import com.ekhtiari.smsorganizer.ui.components.rememberUiHaptics
import com.ekhtiari.smsorganizer.ui.components.filledFieldColors
import com.ekhtiari.smsorganizer.ui.theme.LocalReducedMotion
import com.ekhtiari.smsorganizer.ui.theme.SoftMotion
import com.ekhtiari.smsorganizer.ui.theme.sharedWith
import com.ekhtiari.smsorganizer.ui.theme.withMessageFont
import androidx.compose.foundation.interaction.MutableInteractionSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    sharedScope: AnimatedVisibilityScope? = null,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.refreshStatus() }

    if (ui.confirmDeleteThreadId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteConversation,
            icon = {
                ExpressiveDialogIcon(
                    icon = Icons.Outlined.Delete,
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                )
            },
            title = {
                Text(
                    stringResource(R.string.delete_conversation),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                )
            },
            text = { Text(stringResource(R.string.delete_conversation_confirm)) },
            shape = MaterialTheme.shapes.extraExtraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeleteConversation,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
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
        onUndoDelete = viewModel::undoDelete,
        onDismissDeleteNotice = viewModel::dismissDeleteNotice,
        sharedScope = sharedScope,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    onUndoDelete: () -> Unit = {},
    onDismissDeleteNotice: (Long) -> Unit = {},
    sharedScope: AnimatedVisibilityScope? = null,
) {
    val haptics = rememberUiHaptics()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reduce = LocalReducedMotion.current
    val snackbarHostState = remember { SnackbarHostState() }
    DeleteSnackbarEffect(
        notice = ui.deleteNotice,
        snackbarHostState = snackbarHostState,
        onUndo = onUndoDelete,
        onRequestDefaultSms = onRequestDefaultSms,
        onDismiss = onDismissDeleteNotice,
    )
    var toolbarExpanded by rememberSaveable { mutableStateOf(true) }
    val scrollAway = with(FloatingToolbarDefaults) {
        Modifier.floatingToolbarVerticalNestedScroll(
            expanded = toolbarExpanded,
            onExpand = { toolbarExpanded = true },
            onCollapse = { toolbarExpanded = false },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                    )
                },
                subtitle = {
                    Text(
                        text = stringResource(categoryLabel(ui.category)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptics.click()
                            onSettings()
                        },
                        shapes = IconButtonShapes(
                            shape = MaterialTheme.shapes.extraLarge,
                            pressedShape = MaterialTheme.shapes.small,
                        ),
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    subtitleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            // Scaffold does not inset the bottomBar. navigationBarsPadding covers
            // both the 3-button bar and the gesture handle, and lifts the FAB with the toolbar.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalFloatingToolbar(
                    expanded = toolbarExpanded,
                    floatingActionButton = {
                        FloatingToolbarDefaults.VibrantFloatingActionButton(
                            onClick = {
                                haptics.click()
                                onCompose()
                            },
                            shape = MaterialTheme.shapes.largeIncreased,
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.new_message),
                            )
                        }
                    },
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    CategoryTabsSpec.forEach { tab ->
                        CategoryToolButton(
                            selected = ui.category == tab.category,
                            icon = tab.icon,
                            label = stringResource(tab.labelRes),
                            onClick = {
                                haptics.tick()
                                onCategory(tab.category)
                            },
                        )
                    }
                }
            }
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
                enter = SoftMotion.fadeSlideUp(reduce),
                exit = SoftMotion.fadeSlideDown(reduce),
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
                enter = SoftMotion.fadeSlideUp(reduce),
                exit = SoftMotion.fadeSlideDown(reduce),
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
                        tint = MaterialTheme.colorScheme.tertiary,
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
                    .animateContentSize(animationSpec = SoftMotion.spatial(reduce)),
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
                textStyle = MaterialTheme.typography.bodyLarge.withMessageFont().copy(
                    textDirection = TextDirection.Content,
                ),
                colors = filledFieldColors(),
                shape = MaterialTheme.shapes.extraLarge,
            )

            AnimatedContent(
                targetState = ui.category,
                transitionSpec = SoftMotion.tabContentTransform(
                    indexOf = ::categoryIndex,
                    rtl = rtl,
                    reduce = reduce,
                ),
                label = "inboxTabContent",
                modifier = Modifier.weight(1f),
            ) { category ->
                when {
                    ui.loading && conversations.isEmpty() -> LoadingState()
                    conversations.isEmpty() -> EmptyState(
                        title = stringResource(R.string.no_conversations),
                        hint = stringResource(R.string.empty_conversations_hint),
                        icon = Icons.AutoMirrored.Outlined.Chat,
                    )
                    else -> LazyColumn(
                        modifier = scrollAway,
                        // Scaffold already pads by the bottom bar (including the nav inset).
                        // This extra space keeps the last row from sitting under the toolbar.
                        contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(
                            items = conversations,
                            key = { _, item -> "${category ?: "all"}-${item.threadId}" },
                        ) { index, conversation ->
                            ConversationRow(
                                conversation = conversation,
                                position = groupPosition(index, conversations.lastIndex),
                                onClick = {
                                    haptics.click()
                                    onOpenThread(conversation.threadId, conversation.address)
                                },
                                onArchive = { onArchive(conversation.threadId) },
                                onDelete = { onDelete(conversation.threadId) },
                                onCopyOtp = onCopyOtp,
                                sharedScope = sharedScope,
                                modifier = Modifier.animateItem(
                                    fadeInSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                                    fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                                    placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategoryToolButton(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0f)
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "categoryContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            LocalContentColor.current
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "categoryContent",
    )
    Surface(
        onClick = onClick,
        shape = if (selected) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .height(48.dp)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (selected) 12.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
            AnimatedVisibility(visible = selected) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val reduce = LocalReducedMotion.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(animationSpec = SoftMotion.spatial(reduce)),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLargeIncreased,
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                com.ekhtiari.smsorganizer.ui.components.TonalIcon(icon = icon)
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp),
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

private fun categoryLabel(category: MessageCategory?): Int =
    CategoryTabsSpec.first { it.category == category }.labelRes

private enum class GroupPosition { Single, Top, Middle, Bottom }

private fun groupPosition(index: Int, last: Int): GroupPosition = when {
    last == 0 -> GroupPosition.Single
    index == 0 -> GroupPosition.Top
    index == last -> GroupPosition.Bottom
    else -> GroupPosition.Middle
}

@Composable
private fun groupedShape(position: GroupPosition): RoundedCornerShape {
    val outer = 28.dp
    val inner = 8.dp
    return when (position) {
        GroupPosition.Single -> RoundedCornerShape(outer)
        GroupPosition.Top -> RoundedCornerShape(
            topStart = outer,
            topEnd = outer,
            bottomStart = inner,
            bottomEnd = inner,
        )
        GroupPosition.Middle -> RoundedCornerShape(inner)
        GroupPosition.Bottom -> RoundedCornerShape(
            topStart = inner,
            topEnd = inner,
            bottomStart = outer,
            bottomEnd = outer,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConversationRow(
    conversation: Conversation,
    position: GroupPosition,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onCopyOtp: (String) -> Unit,
    sharedScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier,
) {
    val unread = conversation.unreadCount > 0
    val interaction = remember { MutableInteractionSource() }
    val reduce = LocalReducedMotion.current
    Surface(
        onClick = onClick,
        shape = groupedShape(position),
        color = if (unread) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        interactionSource = interaction,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .expressivePress(interaction)
            .animateContentSize(animationSpec = SoftMotion.spatial(reduce, fast = true)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarCircle(
                name = conversation.displayName,
                modifier = Modifier.sharedWith("avatar-${conversation.threadId}", sharedScope),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = isolateBidi(conversation.displayName),
                            style = MaterialTheme.typography.titleMediumEmphasized.withMessageFont().copy(
                                textDirection = TextDirection.Content,
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .sharedWith(
                                    "title-${conversation.threadId}",
                                    sharedScope,
                                    bounds = true,
                                ),
                        )
                        if (unread) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) {
                                Text(
                                    conversation.unreadCount.coerceAtMost(99).toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        textDirection = TextDirection.Ltr,
                                    ),
                                )
                            }
                        }
                    }
                    Text(
                        text = formatRelativeTime(conversation.date),
                        style = MaterialTheme.typography.labelMedium.copy(
                            textDirection = TextDirection.Ltr,
                        ),
                        color = if (unread) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = conversation.snippet,
                    style = MaterialTheme.typography.bodyMedium.withMessageFont().copy(
                        textDirection = TextDirection.Content,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "SIM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            IconButton(
                onClick = onArchive,
                shapes = IconButtonShapes(
                    shape = MaterialTheme.shapes.large,
                    pressedShape = MaterialTheme.shapes.extraSmall,
                ),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    Icons.Outlined.Archive,
                    contentDescription = stringResource(R.string.archive),
                )
            }
            IconButton(
                onClick = onDelete,
                shapes = IconButtonShapes(
                    shape = MaterialTheme.shapes.large,
                    pressedShape = MaterialTheme.shapes.extraSmall,
                ),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete),
                )
            }
        }
        conversation.otpCode?.let { code ->
            CopyCodeChip(
                code = code,
                onClick = { onCopyOtp(code) },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        }
    }
}

/** Keep a sender name in its own direction so it cannot merge with the unread badge. */
private fun isolateBidi(text: String): String = "\u2068$text\u2069"
