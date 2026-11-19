package com.ekhtiari.smsorganizer.domain

import java.util.regex.Pattern

/**
 * Rule-based classifier inspired by Microsoft SMS Organizer categories.
 * Runs fully on-device with no network calls.
 */
object MessageClassifier {

    private val otpPatterns = listOf(
        Pattern.compile(
            """(?i)(?:otp|one[\s-]?time(?:\s+password|\s+pin)?|verification(?:\s+code)?|auth(?:entication)?\s+code|security\s+code|login\s+code|passcode)[^\d]{0,20}(\d{4,8})""",
        ),
        Pattern.compile(
            """(?i)(?:code|pin|password)\s*(?:is|:)?\s*(\d{4,8})""",
        ),
        Pattern.compile(
            """(?i)(\d{4,8})\s+is\s+your\s+(?:otp|code|verification|login)""",
        ),
        Pattern.compile(
            """(?i)(?:کد|رمز)(?:\s+تایید|\s+یکبارمصرف)?[^\d۰-۹]{0,20}([\d۰-۹]{4,8})""",
        ),
    )

    private val transactionKeywords = listOf(
        "debited", "credited", "withdrawn", "deposit", "balance", "a/c", "acct",
        "transaction", "txn", "purchase", "payment of", "paid", "refund",
        "inr", "rs.", "rs ", "usd", "eur", "irr", "rial", "toman",
        "bank", "card ending", "avl bal", "available bal", "upi",
        "واریز", "برداشت", "موجودی", "تراکنش", "حساب", "کارت",
    )

    private val offerKeywords = listOf(
        "offer", "sale", "discount", "off!", "% off", "promo", "promotion",
        "deal", "coupon", "free*", "subscribe", "unsubscribe", "click here",
        "limited time", "buy now", "shop now", "flat", "cashback",
        "تخفیف", "پیشنهاد", "فروش ویژه", "کد تخفیف",
    )

    data class Classification(
        val category: MessageCategory,
        val otpCode: String? = null,
    )

    fun classify(body: String, address: String = ""): Classification {
        val text = body.trim()
        if (text.isEmpty()) return Classification(MessageCategory.PERSONAL)

        extractOtp(text)?.let { code ->
            return Classification(MessageCategory.OTP, normalizeDigits(code))
        }

        val lower = text.lowercase()
        val addr = address.lowercase()

        if (looksLikeTransaction(lower, addr)) {
            return Classification(MessageCategory.TRANSACTIONS)
        }
        if (looksLikeOffer(lower)) {
            return Classification(MessageCategory.OFFERS)
        }
        return Classification(MessageCategory.PERSONAL)
    }

    fun extractOtp(body: String): String? {
        for (pattern in otpPatterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val code = matcher.group(1) ?: continue
                if (code.length in 4..8) return code
            }
        }
        return null
    }

    private fun looksLikeTransaction(lower: String, address: String): Boolean {
        val hitCount = transactionKeywords.count { lower.contains(it) }
        val bankSender = address.any { it.isLetter() } &&
            (address.contains("bank") || address.contains("upi") ||
                Regex("""(?i)[A-Z]{2,}-?[A-Z0-9]{2,}""").containsMatchIn(address))
        return hitCount >= 2 || (hitCount >= 1 && bankSender) ||
            Regex("""(?i)(?:rs\.?|inr|usd|€|£)\s*[\d,]+\.?\d*""").containsMatchIn(lower)
    }

    private fun looksLikeOffer(lower: String): Boolean {
        val hits = offerKeywords.count { lower.contains(it) }
        return hits >= 2 || (hits >= 1 && (lower.contains("http") || lower.contains("www.")))
    }

    private fun normalizeDigits(raw: String): String {
        val persian = "۰۱۲۳۴۵۶۷۸۹"
        return buildString {
            for (ch in raw) {
                val idx = persian.indexOf(ch)
                append(if (idx >= 0) ('0' + idx) else ch)
            }
        }
    }
}
