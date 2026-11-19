package com.ekhtiari.smsorganizer.domain

enum class MessageCategory(val displayName: String) {
    PERSONAL("Personal"),
    TRANSACTIONS("Transactions"),
    OFFERS("Offers"),
    OTP("OTP");

    companion object {
        fun fromName(name: String?): MessageCategory =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: PERSONAL
    }
}
