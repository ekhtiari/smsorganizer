package com.ekhtiari.smsorganizer.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.ekhtiari.smsorganizer.SmsOrganizerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DELETE = "com.ekhtiari.smsorganizer.NOTIF_DELETE"
        const val ACTION_ARCHIVE = "com.ekhtiari.smsorganizer.NOTIF_ARCHIVE"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val address = intent.getStringExtra(EXTRA_ADDRESS).orEmpty()
        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, 0L)
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, 0L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val app = context.applicationContext as? SmsOrganizerApp ?: return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_DELETE -> {
                        if (messageId > 0L) {
                            app.smsRepository.deleteMessageNow(messageId)
                        } else if (threadId > 0L) {
                            app.smsRepository.deleteThreadNow(threadId)
                        }
                    }
                    ACTION_ARCHIVE -> {
                        val resolved = when {
                            threadId > 0L -> threadId
                            else -> app.smsRepository.resolveThreadId(address, messageId.takeIf { it > 0L })
                        }
                        if (resolved > 0L) app.smsRepository.archiveThread(resolved)
                    }
                }
            } finally {
                if (notificationId >= 0) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                } else if (address.isNotBlank()) {
                    app.notificationHelper.dismiss(address)
                }
                pending.finish()
            }
        }
    }
}
