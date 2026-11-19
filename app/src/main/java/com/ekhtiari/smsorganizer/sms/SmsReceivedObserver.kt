package com.ekhtiari.smsorganizer.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.ekhtiari.smsorganizer.SmsOrganizerApp
import com.ekhtiari.smsorganizer.domain.MessageClassifier
import com.ekhtiari.smsorganizer.util.SmsPermissions

/**
 * Observes SMS_RECEIVED when this app is NOT the default SMS app,
 * so OTP / category notifications still work in read-only mode.
 */
class SmsReceivedObserver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (SmsPermissions.isDefaultSmsApp(context)) return // Deliver path already handled

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val address = messages.firstOrNull()?.displayOriginatingAddress.orEmpty()
        val body = messages.joinToString("") { it.displayMessageBody.orEmpty() }
        val classification = MessageClassifier.classify(body, address)

        (context.applicationContext as? SmsOrganizerApp)
            ?.notificationHelper
            ?.notifyIncoming(address, body, classification.category, classification.otpCode, 0L)
    }
}
