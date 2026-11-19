package com.ekhtiari.smsorganizer.sms

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import com.ekhtiari.smsorganizer.SmsOrganizerApp

/**
 * Required for default SMS app — handles RESPOND_VIA_MESSAGE
 * (quick reply from the phone call UI).
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == android.telephony.TelephonyManager.ACTION_RESPOND_VIA_MESSAGE) {
            val address = intent.data?.schemeSpecificPart
                ?: intent.getStringExtra("android.intent.extra.PHONE_NUMBER")
            val body = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            if (!address.isNullOrBlank() && body.isNotBlank()) {
                val app = application as SmsOrganizerApp
                app.smsSender.sendText(Uri.decode(address), body, null)
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
