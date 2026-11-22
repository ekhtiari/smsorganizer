package com.ekhtiari.smsorganizer.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SentSmsWriteTest {

    @Test
    fun buildIncludesThreadIdSoConversationCanLoadReply() {
        val values = SentSmsWrite.build(
            address = "+989121234567",
            body = "hello",
            dateMillis = 1_700_000_000_000L,
            subscriptionId = 2,
            threadId = 42L,
        )

        assertEquals(42L, values.threadId)
        assertEquals("+989121234567", values.address)
        assertEquals("hello", values.body)
        assertEquals(1, values.read)
        assertEquals(1, values.seen)
        assertEquals(2, values.type) // Telephony.Sms.MESSAGE_TYPE_SENT
        assertEquals(2, values.subscriptionId)
        assertTrue(values.threadId > 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun buildRejectsMissingThreadId() {
        SentSmsWrite.build(
            address = "+989121234567",
            body = "hello",
            dateMillis = 1L,
            subscriptionId = 1,
            threadId = 0L,
        )
    }
}
