package com.ekhtiari.smsorganizer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MessageClassifierTest {

    @Test
    fun classifiesOtpEnglish() {
        val result = MessageClassifier.classify("Your OTP is 482913. Do not share.")
        assertEquals(MessageCategory.OTP, result.category)
        assertEquals("482913", result.otpCode)
    }

    @Test
    fun classifiesOtpVerificationCode() {
        val result = MessageClassifier.classify("Verification code: 120455")
        assertEquals(MessageCategory.OTP, result.category)
        assertEquals("120455", result.otpCode)
    }

    @Test
    fun classifiesTransaction() {
        val result = MessageClassifier.classify(
            "INR 1,250.00 debited from A/C XX1234. Avl Bal Rs. 9,800. Txn on 01-Jan.",
            address = "HDFCBK",
        )
        assertEquals(MessageCategory.TRANSACTIONS, result.category)
        assertNull(result.otpCode)
    }

    @Test
    fun classifiesOffer() {
        val result = MessageClassifier.classify(
            "Flash sale! 50% off limited time. Shop now at www.example.com — unsubscribe anytime.",
        )
        assertEquals(MessageCategory.OFFERS, result.category)
    }

    @Test
    fun personalByDefault() {
        val result = MessageClassifier.classify("Hey, are we still meeting at 5?")
        assertEquals(MessageCategory.PERSONAL, result.category)
        assertNull(result.otpCode)
    }

    @Test
    fun extractOtpReturnsNullWhenAbsent() {
        assertNull(MessageClassifier.extractOtp("Just a hello message"))
        assertNotNull(MessageClassifier.extractOtp("Your login code is 998877"))
    }
}
