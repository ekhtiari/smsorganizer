package com.ekhtiari.smsorganizer.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.provider.Telephony

class SmsSender(
    private val context: Context,
    private val simManager: SimManager,
) {

    fun sendText(
        address: String,
        body: String,
        subscriptionId: Int?,
        threadId: Long? = null,
    ): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val subId = simManager.resolveSendSubscriptionId(subscriptionId)
        val smsManager = getSmsManager(subId)

        val sentIntent = PendingIntent.getBroadcast(
            context,
            address.hashCode(),
            Intent(context, SmsSentReceiver::class.java),
            pendingFlags(),
        )
        val deliveredIntent = PendingIntent.getBroadcast(
            context,
            address.hashCode() + 1,
            Intent(context, SmsDeliveredReceiver::class.java),
            pendingFlags(),
        )

        val parts = smsManager.divideMessage(body)
        if (parts.size == 1) {
            smsManager.sendTextMessage(address, null, body, sentIntent, deliveredIntent)
        } else {
            val sentIntents = ArrayList(List(parts.size) { sentIntent })
            val deliveredIntents = ArrayList(List(parts.size) { deliveredIntent })
            smsManager.sendMultipartTextMessage(
                address,
                null,
                parts,
                sentIntents,
                deliveredIntents,
            )
        }

        // Default SMS app must write to the provider so the reply appears in-thread
        if (Telephony.Sms.getDefaultSmsPackage(context) == context.packageName) {
            writeSentMessage(address, body, subId, threadId)
        }
        return true
    }

    private fun writeSentMessage(
        address: String,
        body: String,
        subscriptionId: Int,
        threadId: Long?,
    ) {
        val resolvedThreadId = when {
            threadId != null && threadId > 0L -> threadId
            else -> resolveThreadId(address)
        }
        if (resolvedThreadId <= 0L) return

        try {
            val values = SentSmsWrite.build(
                address = address,
                body = body,
                dateMillis = System.currentTimeMillis(),
                subscriptionId = subscriptionId,
                threadId = resolvedThreadId,
            ).toContentValues()
            context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
        } catch (_: Exception) {
            // Provider write may fail if not default; ignore
        }
    }

    private fun resolveThreadId(address: String): Long {
        return try {
            Telephony.Threads.getOrCreateThreadId(context, address)
        } catch (_: Exception) {
            0L
        }
    }

    @Suppress("DEPRECATION")
    private fun getSmsManager(subscriptionId: Int): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
                .createForSubscriptionId(subscriptionId)
        } else {
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        }
    }

    private fun pendingFlags(): Int {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return flags
    }
}
