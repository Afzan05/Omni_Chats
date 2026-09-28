package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.remote.GeminiClient
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(private val chatDao: ChatDao) {

    fun getAllSessions(): Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getSession(sessionId: String): Flow<ChatSessionEntity?> = chatDao.getSessionById(sessionId)

    fun getMessages(sessionId: String): Flow<List<ChatMessageEntity>> = chatDao.getMessages(sessionId)

    fun getStarredMessages(): Flow<List<ChatMessageEntity>> = chatDao.getStarredMessages()

    fun searchMessages(query: String): Flow<List<ChatMessageEntity>> = chatDao.searchMessages(query)

    suspend fun markSessionRead(sessionId: String) {
        chatDao.resetUnreadCount(sessionId)
    }

    suspend fun sendUserMessage(
        sessionId: String,
        content: String,
        messageType: String = "TEXT",
        mediaUri: String? = null,
        audioDurationSec: Int = 0
    ): ChatMessageEntity {
        val now = System.currentTimeMillis()
        val userMsg = ChatMessageEntity(
            sessionId = sessionId,
            sender = "user",
            content = content,
            timestamp = now,
            messageType = messageType,
            mediaUri = mediaUri,
            audioDurationSec = audioDurationSec,
            deliveryStatus = "SENDING"
        )
        val insertedId = chatDao.insertMessage(userMsg)
        val preview = when (messageType) {
            "IMAGE" -> "📷 Photo"
            "VOICE" -> "🎙️ Voice message (${audioDurationSec}s)"
            else -> content
        }
        chatDao.updateLastMessage(sessionId, preview, now)
        return userMsg.copy(id = insertedId)
    }

    suspend fun updateDeliveryStatus(messageId: Long, status: String) {
        chatDao.updateDeliveryStatus(messageId, status)
    }

    suspend fun receiveDirectMessage(
        sessionId: String,
        content: String,
        messageType: String = "TEXT"
    ): ChatMessageEntity {
        val now = System.currentTimeMillis()
        val peerMsg = ChatMessageEntity(
            sessionId = sessionId,
            sender = "contact",
            content = content,
            timestamp = now,
            messageType = messageType,
            deliveryStatus = "READ"
        )
        val insertedId = chatDao.insertMessage(peerMsg)
        chatDao.updateLastMessage(sessionId, content, now)
        return peerMsg.copy(id = insertedId)
    }

    suspend fun getOrCreateDirectSession(targetUser: com.example.data.local.UserEntity): String {
        val cleanPhone = targetUser.phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
        val sessionId = "dm_$cleanPhone"
        val existing = chatDao.getSessionByIdDirect(sessionId)
        if (existing != null) {
            return sessionId
        }
        val now = System.currentTimeMillis()
        val session = ChatSessionEntity(
            id = sessionId,
            title = targetUser.name,
            contactName = targetUser.name,
            contactRole = targetUser.phoneNumber,
            avatarEmoji = targetUser.avatarEmoji,
            avatarColorHex = targetUser.avatarColorHex,
            isPinned = false,
            lastMessage = "Direct conversation started",
            lastMessageTime = now,
            unreadCount = 0,
            systemPrompt = "Direct Message with ${targetUser.name}",
            category = "Direct"
        )
        chatDao.insertSession(session)
        return sessionId
    }

    suspend fun getAiResponse(sessionId: String, userMessage: String): ChatMessageEntity {
        val session = chatDao.getSessionByIdDirect(sessionId)
        val systemPrompt = session?.systemPrompt ?: ""
        val personaName = session?.contactName ?: "Assistant"
        val recentMessages = chatDao.getRecentMessages(sessionId, limit = 10)

        val reply = GeminiClient.generateReply(
            systemPrompt = systemPrompt,
            recentMessages = recentMessages.reversed(),
            userMessage = userMessage,
            personaName = personaName
        )

        val now = System.currentTimeMillis()
        val botMsg = ChatMessageEntity(
            sessionId = sessionId,
            sender = "contact",
            content = reply,
            timestamp = now,
            messageType = "TEXT"
        )
        val insertedId = chatDao.insertMessage(botMsg)
        chatDao.updateLastMessage(sessionId, reply, now)
        return botMsg.copy(id = insertedId)
    }

    suspend fun toggleStar(messageId: Long, isStarred: Boolean) {
        chatDao.toggleStar(messageId, isStarred)
    }

    suspend fun setReaction(messageId: Long, reaction: String?) {
        chatDao.setReaction(messageId, reaction)
    }

    suspend fun deleteMessage(messageId: Long) {
        chatDao.deleteMessage(messageId)
    }

    suspend fun clearChat(sessionId: String) {
        chatDao.clearMessages(sessionId)
        chatDao.updateLastMessage(sessionId, "Chat cleared", System.currentTimeMillis())
    }

    suspend fun togglePin(sessionId: String, isPinned: Boolean) {
        chatDao.togglePin(sessionId, isPinned)
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.clearMessages(sessionId)
        chatDao.deleteSession(sessionId)
    }

    suspend fun createSession(
        title: String,
        contactName: String,
        contactRole: String,
        emoji: String,
        colorHex: String,
        systemPrompt: String,
        initialGreeting: String = ""
    ): String {
        val id = "session_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val session = ChatSessionEntity(
            id = id,
            title = title,
            contactName = contactName,
            contactRole = contactRole,
            avatarEmoji = emoji,
            avatarColorHex = colorHex,
            isPinned = false,
            lastMessage = if (initialGreeting.isNotBlank()) initialGreeting else "Chat started",
            lastMessageTime = now,
            unreadCount = 0,
            systemPrompt = systemPrompt,
            category = "AI"
        )
        chatDao.insertSession(session)

        if (initialGreeting.isNotBlank()) {
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = id,
                    sender = "contact",
                    content = initialGreeting,
                    timestamp = now,
                    messageType = "TEXT"
                )
            )
        }
        return id
    }
}
