package com.ekhtiari.smsorganizer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_meta")
data class MessageMetaEntity(
    @PrimaryKey val messageId: Long,
    val category: String,
    val otpCode: String? = null,
    val classifiedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "thread_meta")
data class ThreadMetaEntity(
    @PrimaryKey val threadId: Long,
    val pinnedCategory: String? = null,
    val blocked: Boolean = false,
    val archived: Boolean = false,
)
