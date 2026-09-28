package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [
        Index("sessionId"),
        Index("timestamp")
    ]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val sender: String, // "user" or "contact"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: String = "TEXT", // "TEXT", "IMAGE", "VOICE"
    val mediaUri: String? = null,
    val audioDurationSec: Int = 0,
    val isStarred: Boolean = false,
    val reaction: String? = null, // e.g. "❤️", "👍", "🔥", "💡"
    val deliveryStatus: String = "READ" // "SENDING", "SENT", "DELIVERED", "READ"
)
