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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import com.ekhtiari.smsorganizer.ui.components.ExpressiveDialogIcon
import com.ekhtiari.smsorganizer.ui.components.rememberUiHaptics

/** In-app prompt for a newer GitHub release. Renders nothing when no update is pending. */
@Composable
fun UpdatePrompt() {
    val context = LocalContext.current
    val app = context.applicationContext as? SmsOrganizerApp ?: return
    val offer by app.updateChecker.offer.collectAsStateWithLifecycle()
    val pending = offer ?: return
    UpdatePromptDialog(
        versionName = pending.versionName,
        currentVersion = BuildConfig.VERSION_NAME,
        directApk = pending.directApk,
        onConfirm = {
            if (openUpdateUrl(context, pending.downloadUrl)) {
                app.updateChecker.hideForSession()
            }
        },
        onLater = { app.updateChecker.dismissCurrent() },
        onDismiss = { app.updateChecker.hideForSession() },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun UpdatePromptDialog(
    versionName: String,
    currentVersion: String,
    directApk: Boolean,
    onConfirm: () -> Unit,
    onLater: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberUiHaptics()
    val actionLabel = stringResource(
        if (directApk) R.string.update_download else R.string.update_view_release,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { ExpressiveDialogIcon(Icons.Outlined.SystemUpdate) },
        title = {
            Text(
                stringResource(R.string.update_available_title),
                style = MaterialTheme.typography.headlineSmallEmphasized,
            )
        },
        text = {
            Text(
                stringResource(
                    R.string.update_available_body,
                    ltrIsolate(versionName),
                    ltrIsolate(currentVersion),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        shape = MaterialTheme.shapes.extraExtraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        confirmButton = {
            Button(
                onClick = {
                    haptics.click()
                    onConfirm()
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Icon(
                    if (directApk) Icons.Outlined.Download else Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.update_later))
            }
        },
    )
}

/** Keeps tokens like 1.0.8 from flipping when they sit inside a Persian sentence. */
private fun ltrIsolate(value: String): String = "\u2066$value\u2069"

internal fun openUpdateUrl(context: Context, url: String): Boolean {
    if (!url.startsWith("https://")) return false
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    return runCatching { context.startActivity(intent) }.isSuccess
}
