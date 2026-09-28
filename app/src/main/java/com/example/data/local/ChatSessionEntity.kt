package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val contactName: String,
    val contactRole: String,
    val avatarEmoji: String,
    val avatarColorHex: String = "#6366F1",
    val isPinned: Boolean = false,
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val systemPrompt: String = "",
    val category: String = "AI" // "AI", "Direct", "Group"
)
