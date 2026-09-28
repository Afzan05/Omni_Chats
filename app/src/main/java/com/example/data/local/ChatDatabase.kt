package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class, UserEntity::class],
    version = 3,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "omnichat_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial data asynchronously
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialData(database.chatDao(), database.userDao())
                    }
                }
            }

            private suspend fun seedInitialData(dao: ChatDao, userDao: UserDao) {
                val now = System.currentTimeMillis()

                val sessions = listOf(
                    ChatSessionEntity(
                        id = "session_nova",
                        title = "Nova",
                        contactName = "Nova",
                        contactRole = "AI Assistant & Polymath",
                        avatarEmoji = "✨",
                        avatarColorHex = "#6366F1",
                        isPinned = true,
                        lastMessage = "Hello! I'm Nova, your AI companion. How can I brighten or organize your day?",
                        lastMessageTime = now - 60_000,
                        unreadCount = 0,
                        systemPrompt = "You are Nova, an intelligent, empathetic, and knowledgeable AI assistant. You provide articulate, thoughtful answers, structured formatting, and engaging conversation.",
                        category = "AI"
                    ),
                    ChatSessionEntity(
                        id = "session_devbyte",
                        title = "DevByte",
                        contactName = "DevByte",
                        contactRole = "Senior Software Engineer",
                        avatarEmoji = "⚡",
                        avatarColorHex = "#10B981",
                        isPinned = true,
                        lastMessage = "Ready to review algorithms, debug code, or discuss modern system architecture.",
                        lastMessageTime = now - 180_000,
                        unreadCount = 1,
                        systemPrompt = "You are DevByte, an expert software architect and engineer. You explain complex code concepts clearly, use clean markdown code blocks with syntax highlighting hints, and give pragmatic advice.",
                        category = "AI"
                    ),
                    ChatSessionEntity(
                        id = "session_lyra",
                        title = "Lyra",
                        contactName = "Lyra",
                        contactRole = "Creative Wordsmith & Poet",
                        avatarEmoji = "✍️",
                        avatarColorHex = "#EC4899",
                        isPinned = false,
                        lastMessage = "Words are paints on the canvas of thought. What shall we compose today?",
                        lastMessageTime = now - 720_000,
                        unreadCount = 0,
                        systemPrompt = "You are Lyra, an imaginative storyteller and lyricist. You craft vivid descriptions, poetry, short stories, and engaging dialogues.",
                        category = "AI"
                    ),
                    ChatSessionEntity(
                        id = "session_marco",
                        title = "Chef Marco",
                        contactName = "Chef Marco",
                        contactRole = "Culinary Maestro",
                        avatarEmoji = "🍳",
                        avatarColorHex = "#F97316",
                        isPinned = false,
                        lastMessage = "Tell me what ingredients you have in your fridge, and we'll make culinary magic!",
                        lastMessageTime = now - 3_600_000,
                        unreadCount = 0,
                        systemPrompt = "You are Chef Marco, a passionate culinary artist. You suggest delectable recipes, seasoning tricks, ingredient substitutions, and step-by-step cooking techniques.",
                        category = "AI"
                    ),
                    ChatSessionEntity(
                        id = "session_atlas",
                        title = "Atlas",
                        contactName = "Atlas",
                        contactRole = "Global Explorer & Guide",
                        avatarEmoji = "🌍",
                        avatarColorHex = "#06B6D4",
                        isPinned = false,
                        lastMessage = "Where in the world are we daydreaming of traveling next?",
                        lastMessageTime = now - 86_400_000,
                        unreadCount = 0,
                        systemPrompt = "You are Atlas, a seasoned globetrotter and cultural navigator. You provide hidden gem recommendations, local customs, packing tips, and curated itineraries.",
                        category = "AI"
                    ),
                    ChatSessionEntity(
                        id = "dm_15552345678",
                        title = "Elena Rostova",
                        contactName = "Elena Rostova",
                        contactRole = "+1 (555) 234-5678",
                        avatarEmoji = "🎨",
                        avatarColorHex = "#EC4899",
                        isPinned = false,
                        lastMessage = "Hey! Loved the new interface design mockups! Are we syncing today?",
                        lastMessageTime = now - 120_000,
                        unreadCount = 1,
                        systemPrompt = "Direct Message with Elena Rostova",
                        category = "Direct"
                    ),
                    ChatSessionEntity(
                        id = "dm_15553456789",
                        title = "Marcus Vance",
                        contactName = "Marcus Vance",
                        contactRole = "+1 (555) 345-6789",
                        avatarEmoji = "💻",
                        avatarColorHex = "#3B82F6",
                        isPinned = false,
                        lastMessage = "The real-time WebSocket and Room sync pipeline is looking solid. Let's merge the PR!",
                        lastMessageTime = now - 900_000,
                        unreadCount = 0,
                        systemPrompt = "Direct Message with Marcus Vance",
                        category = "Direct"
                    )
                )
                dao.insertSessions(sessions)

                // Seed messages for Nova
                val novaMessages = listOf(
                    ChatMessageEntity(
                        sessionId = "session_nova",
                        sender = "contact",
                        content = "Welcome to OmniChat! 🚀 I'm Nova, your conversational AI assistant. You can ask me questions, practice languages, plan projects, or brainstorm ideas.",
                        timestamp = now - 300_000,
                        messageType = "TEXT",
                        isStarred = true,
                        reaction = "❤️",
                        deliveryStatus = "READ"
                    ),
                    ChatMessageEntity(
                        sessionId = "session_nova",
                        sender = "user",
                        content = "Hey Nova! Can you help me organize my day and give me a motivational thought?",
                        timestamp = now - 180_000,
                        messageType = "TEXT",
                        deliveryStatus = "READ"
                    ),
                    ChatMessageEntity(
                        sessionId = "session_nova",
                        sender = "contact",
                        content = "Here is a quick framework for your day:\n\n1. **High-Impact Priority**: Tackle your most creative task first.\n2. **Quick Wins**: Knock out three 5-minute tasks to build momentum.\n3. **Mindful Reset**: Take a 10-minute walk without your phone.\n\n*\"The secret of getting ahead is getting started.\"* You've got this! ✨",
                        timestamp = now - 60_000,
                        messageType = "TEXT",
                        reaction = "💡",
                        deliveryStatus = "READ"
                    )
                )
                dao.insertMessages(novaMessages)

                // Seed messages for DevByte
                val devMessages = listOf(
                    ChatMessageEntity(
                        sessionId = "session_devbyte",
                        sender = "contact",
                        content = "Hello developer! Here is a clean Kotlin snippet demonstrating asynchronous Flows:\n\n```kotlin\nfun fetchUpdates(): Flow<State> = flow {\n    emit(State.Loading)\n    val data = repository.loadData()\n    emit(State.Success(data))\n}.flowOn(Dispatchers.IO)\n```\nLet me know what we are building today!",
                        timestamp = now - 180_000,
                        messageType = "TEXT",
                        isStarred = true,
                        reaction = "⚡",
                        deliveryStatus = "READ"
                    )
                )
                dao.insertMessages(devMessages)

                // Seed messages for Direct Message with Elena Rostova
                val elenaMessages = listOf(
                    ChatMessageEntity(
                        sessionId = "dm_15552345678",
                        sender = "contact",
                        content = "Hey! How's the new project coming along?",
                        timestamp = now - 600_000,
                        messageType = "TEXT",
                        deliveryStatus = "READ"
                    ),
                    ChatMessageEntity(
                        sessionId = "dm_15552345678",
                        sender = "user",
                        content = "Just shipped the real-time direct messaging update! You can see delivery receipts and online presence now.",
                        timestamp = now - 300_000,
                        messageType = "TEXT",
                        deliveryStatus = "READ"
                    ),
                    ChatMessageEntity(
                        sessionId = "dm_15552345678",
                        sender = "contact",
                        content = "Hey! Loved the new interface design mockups! Are we syncing today?",
                        timestamp = now - 120_000,
                        messageType = "TEXT",
                        deliveryStatus = "READ",
                        reaction = "❤️"
                    )
                )
                dao.insertMessages(elenaMessages)

                // Seed registered community users for Direct Messaging directory
                val defaultUsers = listOf(
                    UserEntity(
                        phoneNumber = "+15552345678",
                        name = "Elena Rostova",
                        countryCode = "+1",
                        avatarEmoji = "🎨",
                        avatarColorHex = "#EC4899",
                        salt = "salt_elena",
                        credentialHash = "hash_elena",
                        isLoggedIn = false,
                        isOnline = true,
                        bio = "Product Designer & UI enthusiast ☕"
                    ),
                    UserEntity(
                        phoneNumber = "+15553456789",
                        name = "Marcus Vance",
                        countryCode = "+1",
                        avatarEmoji = "💻",
                        avatarColorHex = "#3B82F6",
                        salt = "salt_marcus",
                        credentialHash = "hash_marcus",
                        isLoggedIn = false,
                        isOnline = true,
                        bio = "Distributed systems architect. Coffee -> Code 🚀"
                    ),
                    UserEntity(
                        phoneNumber = "+15554567890",
                        name = "Aria Patel",
                        countryCode = "+1",
                        avatarEmoji = "🚀",
                        avatarColorHex = "#8B5CF6",
                        salt = "salt_aria",
                        credentialHash = "hash_aria",
                        isLoggedIn = false,
                        isOnline = true,
                        bio = "Mobile Engineer building delightful mobile apps ✨"
                    ),
                    UserEntity(
                        phoneNumber = "+819012345678",
                        name = "Kai Takahashi",
                        countryCode = "+81",
                        avatarEmoji = "🎧",
                        avatarColorHex = "#10B981",
                        salt = "salt_kai",
                        credentialHash = "hash_kai",
                        isLoggedIn = false,
                        isOnline = false,
                        bio = "Audio engineer & Tokyo soundscapes 🎵"
                    ),
                    UserEntity(
                        phoneNumber = "+33612345678",
                        name = "Chloe Dubois",
                        countryCode = "+33",
                        avatarEmoji = "🌿",
                        avatarColorHex = "#F59E0B",
                        salt = "salt_chloe",
                        credentialHash = "hash_chloe",
                        isLoggedIn = false,
                        isOnline = true,
                        bio = "Nature researcher, photographer & hiker 🏔️"
                    )
                )
                userDao.insertUsers(defaultUsers)
            }
        }
    }
}
