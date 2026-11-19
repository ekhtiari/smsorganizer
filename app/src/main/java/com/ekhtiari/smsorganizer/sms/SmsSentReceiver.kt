package com.ekhtiari.smsorganizer.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(context, "SMS failed to send", Toast.LENGTH_SHORT).show()
        }
    }
}

class SmsDeliveredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Delivery reports are optional; no UI toast to avoid noise.
    }
}
