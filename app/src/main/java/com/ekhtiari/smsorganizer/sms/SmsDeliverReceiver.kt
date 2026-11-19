package com.ekhtiari.smsorganizer.sms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.ekhtiari.smsorganizer.SmsOrganizerApp
import com.ekhtiari.smsorganizer.domain.MessageClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Required by Android for the default SMS app — receives SMS_DELIVER
 * and must write messages into the Telephony provider.
 */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val address = messages.firstOrNull()?.displayOriginatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
        val timestamp = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
        val subId = intent.getIntExtra("subscription", -1)
            .takeIf { it >= 0 }
            ?: intent.getIntExtra(SubscriptionExtras.SUBSCRIPTION_KEY, -1)

        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, timestamp)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            if (subId >= 0) put(Telephony.Sms.SUBSCRIPTION_ID, subId)
        }

        val uri = try {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        } catch (_: Exception) {
            null
        }

        val classification = MessageClassifier.classify(body, address)
        val app = context.applicationContext as? SmsOrganizerApp
        val messageId = uri?.lastPathSegment?.toLongOrNull()

        if (app != null && messageId != null) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    app.smsRepository.saveClassification(
                        messageId = messageId,
                        category = classification.category,
                        otpCode = classification.otpCode,
                    )
                    app.notificationHelper.notifyIncoming(
                        address = address,
                        body = body,
                        category = classification.category,
                        otpCode = classification.otpCode,
                        threadId = uri?.let {
                            // thread id resolved later via repository when opening
                            messageId
                        } ?: 0L,
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            (context.applicationContext as? SmsOrganizerApp)
                ?.notificationHelper
                ?.notifyIncoming(address, body, classification.category, classification.otpCode, 0L)
        }
    }
}

object SubscriptionExtras {
    const val SUBSCRIPTION_KEY = "subscription"
}
