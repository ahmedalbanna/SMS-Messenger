package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class DbMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val systemSmsId: Long? = null,
    val address: String,
    val contactName: String?,
    val body: String,
    val date: Long,
    val type: Int, // 1 = RECEIVED (INBOX), 2 = SENT, 3 = DRAFT, 4 = FAILED
    val isRead: Boolean,
    val threadId: Long // Group messages in same conversation
)
