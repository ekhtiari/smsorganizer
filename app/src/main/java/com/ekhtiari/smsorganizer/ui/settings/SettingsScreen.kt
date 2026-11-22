package com.ekhtiari.smsorganizer.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhtiari.smsorganizer.BuildConfig
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.SmsOrganizerApp
import com.ekhtiari.smsorganizer.ui.components.TonalIcon
import com.ekhtiari.smsorganizer.ui.components.rememberUiHaptics
import com.ekhtiari.smsorganizer.ui.theme.SoftMotion
import com.ekhtiari.smsorganizer.update.UpdateCheckResult
import com.ekhtiari.smsorganizer.util.SmsPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenDefaultApps: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as SmsOrganizerApp
    var isDefault by remember { mutableStateOf(false) }
    var hasPerms by remember { mutableStateOf(false) }
    var roleAvailable by remember { mutableStateOf(true) }
    var simSummary by remember { mutableStateOf("") }
    val updateOffer by app.updateChecker.offer.collectAsStateWithLifecycle()
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    val scope = rememberCoroutineScope()
    val pendingUpdate = updateOffer
    val updateStatus = when {
        checkingUpdate -> stringResource(R.string.update_checking)
        pendingUpdate != null -> stringResource(
            R.string.update_available_status,
            pendingUpdate.versionName,
        )
        updateResult is UpdateCheckResult.UpToDate -> stringResource(R.string.update_up_to_date)
        updateResult is UpdateCheckResult.Failed -> stringResource(R.string.update_check_failed)
        else -> stringResource(R.string.update_check_hint)
    }

    fun refresh() {
        isDefault = SmsPermissions.isDefaultSmsApp(context)
        hasPerms = SmsPermissions.hasAll(context)
        roleAvailable = SmsPermissions.isSmsRoleAvailable(context)
        simSummary = app.simManager.getActiveSims()
            .joinToString("\n") { "${it.label} (subId=${it.subscriptionId})" }
            .ifBlank { "No SIMs detected (need READ_PHONE_STATE)" }
    }

    LaunchedEffect(Unit) { refresh() }

    SettingsContent(
        isDefault = isDefault,
        hasPerms = hasPerms,
        roleAvailable = roleAvailable,
        simSummary = simSummary,
        versionLabel = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        updateStatus = updateStatus,
        checkingUpdate = checkingUpdate,
        onBack = onBack,
        onRequestDefaultSms = onRequestDefaultSms,
        onRequestPermissions = onRequestPermissions,
        onOpenAppInfo = onOpenAppInfo,
        onOpenDefaultApps = onOpenDefaultApps,
        onReclassify = {
            CoroutineScope(Dispatchers.IO).launch {
                app.smsRepository.classifyMissingMessages(2000)
            }
        },
        onCheckUpdate = {
            checkingUpdate = true
            scope.launch {
                try {
                    updateResult = withContext(Dispatchers.IO) {
                        app.updateChecker.check(force = true)
                    }
                } finally {
                    checkingUpdate = false
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    isDefault: Boolean,
    hasPerms: Boolean,
    roleAvailable: Boolean,
    simSummary: String,
    versionLabel: String,
    updateStatus: String,
    checkingUpdate: Boolean,
    onBack: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenDefaultApps: () -> Unit,
    onReclassify: () -> Unit,
    onCheckUpdate: () -> Unit,
) {
    val haptics = rememberUiHaptics()
    val transparentItem = ListItemDefaults.colors(containerColor = Color.Transparent)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .animateContentSize(animationSpec = SoftMotion.tweenSoft()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text("Default SMS app") },
                        supportingContent = {
                            Text(
                                when {
                                    isDefault -> "Enabled"
                                    !roleAvailable -> "Role blocked for sideloaded apps — allow restricted settings first"
                                    else -> "Not set — required to send & receive"
                                },
                            )
                        },
                        leadingContent = { TonalIcon(Icons.Outlined.Sms) },
                        trailingContent = {
                            if (!isDefault) {
                                Button(onClick = {
                                    haptics.click()
                                    onRequestDefaultSms()
                                }) {
                                    Text("Set")
                                }
                            }
                        },
                        colors = transparentItem,
                    )
                    AnimatedVisibility(
                        visible = !isDefault,
                        enter = SoftMotion.fadeSlideUp(),
                        exit = SoftMotion.fadeSlideDown(),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.default_sms_denied_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(
                                onClick = onOpenAppInfo,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.open_app_info))
                            }
                            OutlinedButton(
                                onClick = onOpenDefaultApps,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Icon(
                                    Icons.Outlined.Apps,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.open_default_apps))
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    )
                    ListItem(
                        headlineContent = { Text("Permissions") },
                        supportingContent = {
                            Text(if (hasPerms) "Granted" else "Missing required permissions")
                        },
                        leadingContent = { TonalIcon(Icons.Outlined.VerifiedUser) },
                        trailingContent = {
                            if (!hasPerms) {
                                Button(onClick = {
                                    haptics.click()
                                    onRequestPermissions()
                                }) {
                                    Text(stringResource(R.string.grant))
                                }
                            }
                        },
                        colors = transparentItem,
                    )
                }
            }

            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text("SIM cards") },
                        supportingContent = { Text(simSummary) },
                        leadingContent = { TonalIcon(Icons.Outlined.SimCard) },
                        colors = transparentItem,
                    )
                }
            }

            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.update_check)) },
                        supportingContent = { Text(updateStatus) },
                        leadingContent = { TonalIcon(Icons.Outlined.SystemUpdate) },
                        trailingContent = {
                            Button(
                                onClick = {
                                    haptics.click()
                                    onCheckUpdate()
                                },
                                enabled = !checkingUpdate,
                            ) {
                                Text(stringResource(R.string.update_check_action))
                            }
                        },
                        colors = transparentItem,
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        haptics.click()
                        onReclassify()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Re-classify messages")
                }
            }

            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text(versionLabel) },
                        leadingContent = { TonalIcon(Icons.Outlined.Info) },
                        colors = transparentItem,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isSystemInDarkTheme()) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}
