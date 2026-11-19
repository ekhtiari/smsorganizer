package com.ekhtiari.smsorganizer.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Required stub for default SMS role. Full MMS media pipeline can be expanded later;
 * Android still requires this receiver to be present and permission-gated.
 */
class MmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return
        // Acknowledge receipt; MMS content parsing is out of scope for v1 SMS organizer.
    }
}
