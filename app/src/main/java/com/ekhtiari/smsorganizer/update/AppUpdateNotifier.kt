package com.ekhtiari.smsorganizer.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ekhtiari.smsorganizer.R

/**
 * Low-urgency notification that opens the update URL. Posting is skipped when
 * notification permission is missing so the in-app prompt can still be shown.
 */
class AppUpdateNotifier(private val context: Context) : UpdateNotifier {

    override fun show(offer: UpdateOffer): Boolean {
        if (!canPost()) return false
        ensureChannel()
        val open = pendingView(offer.downloadUrl, requestCode = REQUEST_CONTENT) ?: return false
        val actionLabel = context.getString(
            if (offer.directApk) R.string.update_download else R.string.update_view_release,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_update_title))
            .setContentText(context.getString(R.string.notification_update_body, offer.versionName))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(R.string.notification_update_body, offer.versionName),
                ),
            )
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(open)
            .setColor(context.getColor(R.color.notification_color))
        pendingView(offer.downloadUrl, requestCode = REQUEST_ACTION)?.let { action ->
            builder.addAction(0, actionLabel, action)
        }
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun canPost(): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_updates),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_updates_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun pendingView(url: String, requestCode: Int): PendingIntent? {
        if (!url.startsWith("https://")) return null
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "app_updates"
        const val NOTIFICATION_ID = 51001
        private const val REQUEST_CONTENT = 51002
        private const val REQUEST_ACTION = 51003
    }
}
