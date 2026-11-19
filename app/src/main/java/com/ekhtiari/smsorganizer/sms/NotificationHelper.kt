package com.ekhtiari.smsorganizer.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ekhtiari.smsorganizer.MainActivity
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.domain.MessageCategory
import com.ekhtiari.smsorganizer.util.ContactHelper

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_OTP = "otp"
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                context.getString(R.string.notification_channel_messages),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_OTP,
                context.getString(R.string.notification_channel_otp),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "One-time verification codes"
            },
        )
    }

    fun notifyIncoming(
        address: String,
        body: String,
        category: MessageCategory,
        otpCode: String?,
        threadId: Long,
    ) {
        ensureChannels()
        val title = ContactHelper.resolveDisplayName(context, address) ?: address
        val channel = if (category == MessageCategory.OTP) CHANNEL_OTP else CHANNEL_MESSAGES
        val content = if (otpCode != null) "Code: $otpCode — $body" else body

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_THREAD_ID, threadId)
            putExtra(MainActivity.EXTRA_ADDRESS, address)
        }
        val pending = PendingIntent.getActivity(
            context,
            (address + threadId).hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setColor(context.getColor(R.color.notification_color))
            .setPriority(
                if (category == MessageCategory.OTP) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT,
            )

        if (otpCode != null) {
            val copyIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_COPY_OTP
                putExtra(MainActivity.EXTRA_OTP, otpCode)
            }
            val copyPending = PendingIntent.getActivity(
                context,
                otpCode.hashCode(),
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, context.getString(R.string.copy_otp), copyPending)
        }

        try {
            NotificationManagerCompat.from(context)
                .notify((address.hashCode() and 0x7FFFFFFF), builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted
        }
    }
}
