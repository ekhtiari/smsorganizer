package com.ekhtiari.smsorganizer.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhtiari.smsorganizer.BuildConfig
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.SmsOrganizerApp
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

    fun refresh() {
        isDefault = SmsPermissions.isDefaultSmsApp(context)
        hasPerms = SmsPermissions.hasAll(context)
        roleAvailable = SmsPermissions.isSmsRoleAvailable(context)
        simSummary = app.simManager.getActiveSims()
            .joinToString("\n") { "${it.label} (subId=${it.subscriptionId})" }
            .ifBlank { "No SIMs detected (need READ_PHONE_STATE)" }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .animateContentSize(animationSpec = SoftMotion.tweenSoft()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
                trailingContent = {
                    if (!isDefault) {
                        Button(onClick = onRequestDefaultSms) {
                            Text("Set")
                        }
                    }
                },
            )

            AnimatedVisibility(
                visible = !isDefault,
                enter = SoftMotion.fadeSlideUp(),
                exit = SoftMotion.fadeSlideDown(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.default_sms_denied_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    OutlinedButton(
                        onClick = onOpenAppInfo,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.open_app_info))
                    }
                    OutlinedButton(
                        onClick = onOpenDefaultApps,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.open_default_apps))
                    }
                }
            }

            ListItem(
                headlineContent = { Text("Permissions") },
                supportingContent = {
                    Text(if (hasPerms) "Granted" else "Missing required permissions")
                },
                trailingContent = {
                    if (!hasPerms) {
                        Button(onClick = onRequestPermissions) {
                            Text("Grant")
                        }
                    }
                },
            )

            ListItem(
                headlineContent = { Text("SIM cards") },
                supportingContent = { Text(simSummary) },
            )

            Button(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        app.smsRepository.classifyMissingMessages(2000)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Re-classify messages")
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.update_check)) },
                supportingContent = {
                    val pending = updateOffer
                    Text(
                        when {
                            checkingUpdate -> stringResource(R.string.update_checking)
                            pending != null -> stringResource(
                                R.string.update_available_status,
                                pending.versionName,
                            )
                            updateResult is UpdateCheckResult.UpToDate ->
                                stringResource(R.string.update_up_to_date)
                            updateResult is UpdateCheckResult.Failed ->
                                stringResource(R.string.update_check_failed)
                            else -> stringResource(R.string.update_check_hint)
                        },
                    )
                },
                trailingContent = {
                    Button(
                        onClick = {
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
                        enabled = !checkingUpdate,
                    ) {
                        Text(stringResource(R.string.update_check_action))
                    }
                },
            )

            Text(
                text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}
