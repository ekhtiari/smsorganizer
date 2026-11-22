package com.ekhtiari.smsorganizer.ui.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhtiari.smsorganizer.BuildConfig
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.SmsOrganizerApp

/** In-app prompt for a newer GitHub release. Renders nothing when no update is pending. */
@Composable
fun UpdatePrompt() {
    val context = LocalContext.current
    val app = context.applicationContext as? SmsOrganizerApp ?: return
    val offer by app.updateChecker.offer.collectAsStateWithLifecycle()
    val pending = offer ?: return
    val actionLabel = stringResource(
        if (pending.directApk) R.string.update_download else R.string.update_view_release,
    )
    AlertDialog(
        onDismissRequest = { app.updateChecker.hideForSession() },
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
        confirmButton = {
            TextButton(
                onClick = {
                    if (openUpdateUrl(context, pending.downloadUrl)) {
                        app.updateChecker.hideForSession()
                    }
                },
            ) {
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
