package com.ekhtiari.smsorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: MessageMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(metas: List<MessageMetaEntity>)

    @Query("SELECT * FROM message_meta WHERE messageId = :messageId")
    suspend fun get(messageId: Long): MessageMetaEntity?

    @Query("SELECT * FROM message_meta")
    fun observeAll(): Flow<List<MessageMetaEntity>>

    @Query("SELECT * FROM message_meta WHERE category = :category")
    suspend fun getByCategory(category: String): List<MessageMetaEntity>

    @Query("DELETE FROM message_meta WHERE messageId = :messageId")
    suspend fun delete(messageId: Long)
}

@Dao
interface ThreadMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: ThreadMetaEntity)

    @Query("SELECT * FROM thread_meta WHERE threadId = :threadId")
    suspend fun get(threadId: Long): ThreadMetaEntity?

    @Query("SELECT * FROM thread_meta WHERE blocked = 1")
    suspend fun getBlocked(): List<ThreadMetaEntity>
}
