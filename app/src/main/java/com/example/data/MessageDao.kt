package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE id IN (SELECT MAX(id) FROM messages GROUP BY address) ORDER BY date DESC")
    fun getConversationsFlow(): Flow<List<DbMessage>>

    @Query("SELECT * FROM messages WHERE address = :address ORDER BY date ASC")
    fun getMessageThreadFlow(address: String): Flow<List<DbMessage>>

    @Query("SELECT * FROM messages WHERE body LIKE :query OR address LIKE :query OR COALESCE(contactName, '') LIKE :query ORDER BY date DESC")
    fun searchMessages(query: String): Flow<List<DbMessage>>

    @Query("SELECT COUNT(*) FROM messages WHERE isRead = 0 AND type = 1")
    fun getUnreadCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DbMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<DbMessage>)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM messages WHERE address = :address")
    suspend fun deleteConversation(address: String)

    @Query("UPDATE messages SET isRead = 1 WHERE address = :address")
    suspend fun markAsRead(address: String)

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}
