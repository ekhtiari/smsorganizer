package com.ekhtiari.smsorganizer.domain

data class SimInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val number: String?,
    val isDefaultSms: Boolean,
) {
    val label: String
        get() = buildString {
            append("SIM ${slotIndex + 1}")
            if (carrierName.isNotBlank()) append(" · $carrierName")
            else if (displayName.isNotBlank()) append(" · $displayName")
        }
}
