package com.ekhtiari.smsorganizer.ui.settings

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ekhtiari.smsorganizer.BuildConfig
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.SmsOrganizerApp
import com.ekhtiari.smsorganizer.util.SmsPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(false) }
    var hasPerms by remember { mutableStateOf(false) }
    var simSummary by remember { mutableStateOf("") }

    fun refresh() {
        isDefault = SmsPermissions.isDefaultSmsApp(context)
        hasPerms = SmsPermissions.hasAll(context)
        val app = context.applicationContext as SmsOrganizerApp
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
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ListItem(
                headlineContent = { Text("Default SMS app") },
                supportingContent = {
                    Text(if (isDefault) "Enabled" else "Not set — required to send & receive")
                },
                trailingContent = {
                    if (!isDefault) {
                        Button(onClick = onRequestDefaultSms) {
                            Text("Set")
                        }
                    }
                },
            )

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
                    val app = context.applicationContext as SmsOrganizerApp
                    CoroutineScope(Dispatchers.IO).launch {
                        app.smsRepository.classifyMissingMessages(2000)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Re-classify messages")
            }

            Text(
                text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}
