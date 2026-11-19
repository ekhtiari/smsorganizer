package com.ekhtiari.smsorganizer.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.ekhtiari.smsorganizer.domain.SimInfo

class SimManager(private val context: Context) {

    fun getActiveSims(): List<SimInfo> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val subscriptionManager =
            context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()

        val defaultSmsSubId = SubscriptionManager.getDefaultSmsSubscriptionId()

        return try {
            subscriptionManager.activeSubscriptionInfoList.orEmpty().map { info ->
                SimInfo(
                    subscriptionId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                    displayName = info.displayName?.toString().orEmpty(),
                    carrierName = info.carrierName?.toString().orEmpty(),
                    number = info.number,
                    isDefaultSms = info.subscriptionId == defaultSmsSubId,
                )
            }.sortedBy { it.slotIndex }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    fun resolveSendSubscriptionId(preferred: Int?): Int {
        val sims = getActiveSims()
        if (sims.isEmpty()) return SubscriptionManager.getDefaultSmsSubscriptionId()
        if (preferred != null && sims.any { it.subscriptionId == preferred }) return preferred
        sims.firstOrNull { it.isDefaultSms }?.let { return it.subscriptionId }
        return sims.first().subscriptionId
    }

    fun labelFor(subscriptionId: Int): String {
        val sim = getActiveSims().firstOrNull { it.subscriptionId == subscriptionId }
        return sim?.label ?: "SIM"
    }
}
