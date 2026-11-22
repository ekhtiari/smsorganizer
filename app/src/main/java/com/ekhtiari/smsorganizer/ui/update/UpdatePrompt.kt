package com.ekhtiari.smsorganizer.ui.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhtiari.smsorganizer.BuildConfig
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.SmsOrganizerApp
import com.ekhtiari.smsorganizer.ui.components.rememberUiHaptics

/** In-app prompt for a newer GitHub release. Renders nothing when no update is pending. */
@Composable
fun UpdatePrompt() {
    val context = LocalContext.current
    val app = context.applicationContext as? SmsOrganizerApp ?: return
    val offer by app.updateChecker.offer.collectAsStateWithLifecycle()
    val pending = offer ?: return
    val haptics = rememberUiHaptics()
    val actionLabel = stringResource(
        if (pending.directApk) R.string.update_download else R.string.update_view_release,
    )
    AlertDialog(
        onDismissRequest = { app.updateChecker.hideForSession() },
        icon = {
            Icon(
                Icons.Outlined.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(stringResource(R.string.update_available_title)) },
        text = {
            Text(
                stringResource(
                    R.string.update_available_body,
                    pending.versionName,
                    BuildConfig.VERSION_NAME,
                ),
            )
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        confirmButton = {
            Button(
                onClick = {
                    haptics.click()
                    if (openUpdateUrl(context, pending.downloadUrl)) {
                        app.updateChecker.hideForSession()
                    }
                },
            ) {
                Icon(
                    if (pending.directApk) Icons.Outlined.Download else Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = { app.updateChecker.dismissCurrent() }) {
                Text(stringResource(R.string.update_later))
            }
        },
    )
}

internal fun openUpdateUrl(context: Context, url: String): Boolean {
    if (!url.startsWith("https://")) return false
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    return runCatching { context.startActivity(intent) }.isSuccess
}
