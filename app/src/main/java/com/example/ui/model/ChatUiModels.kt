package com.example.ui.model

import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity

sealed interface Screen {
    data object Auth : Screen
    data object ChatList : Screen
    data class Conversation(val sessionId: String) : Screen
    data class Call(val sessionId: String) : Screen
    data object StarredMessages : Screen
    data class ContactProfile(val sessionId: String) : Screen
    data object ProfileSettings : Screen
    data object UserDirectory : Screen
}

enum class AuthMode {
    SIGN_IN,
    SIGN_UP
}

enum class AuthStep {
    PHONE_INPUT,
    OTP_VERIFICATION
}

enum class ChatFilter(val label: String) {
    ALL("All"),
    PINNED("Pinned"),
    AI("AI Personas"),
    DIRECT("Direct")
}

data class QuickPrompt(
    val title: String,
    val prompt: String,
    val iconEmoji: String
)

object QuickPrompts {
    fun getPromptsForPersona(contactName: String): List<QuickPrompt> {
        return when {
            contactName.contains("DevByte", ignoreCase = true) -> listOf(
                QuickPrompt("Explain Flow", "Explain Kotlin Coroutines StateFlow vs SharedFlow with an example.", "⚡"),
                QuickPrompt("Clean Arch", "How should I structure Clean Architecture in modern Jetpack Compose?", "🏗️"),
                QuickPrompt("Debug Tips", "What are best practices for profiling Android memory leaks?", "🔍"),
                QuickPrompt("Room DB", "Show me how to write a Room DAO with reactive Flow queries.", "💾")
            )
            contactName.contains("Lyra", ignoreCase = true) -> listOf(
                QuickPrompt("Write a Poem", "Write a vivid poem about twilight falling over an electric city.", "✍️"),
                QuickPrompt("Sci-Fi Spark", "Give me a thrilling one-paragraph sci-fi opening hook.", "🚀"),
                QuickPrompt("Haiku", "Write a delicate haiku celebrating rain on green leaves.", "🍃"),
                QuickPrompt("Character Idea", "Describe an eccentric antique clockmaker with a hidden secret.", "⏳")
            )
            contactName.contains("Marco", ignoreCase = true) || contactName.contains("Chef", ignoreCase = true) -> listOf(
                QuickPrompt("Quick Dinner", "What delicious meal can I make in 15 minutes with simple pantry staples?", "🍳"),
                QuickPrompt("Pasta Secret", "How do I make the ultimate creamy Cacio e Pepe without clumping?", "🍝"),
                QuickPrompt("Knife Skills", "What are your top 3 pro knife tips for fast home prep?", "🔪"),
                QuickPrompt("Baking Hack", "What's the best substitute for buttermilk in pancakes?", "🥞")
            )
            contactName.contains("Atlas", ignoreCase = true) -> listOf(
                QuickPrompt("Kyoto Gems", "What are hidden, quiet spots to visit in Kyoto away from crowds?", "⛩️"),
                QuickPrompt("Pack Light", "Give me a capsule packing list for a 1-week European autumn trip.", "🎒"),
                QuickPrompt("Street Food", "What are the must-eat night market dishes in Taipei?", "🍜"),
                QuickPrompt("Solo Travel", "What are essential safety tips for a first-time solo traveler?", "🧭")
            )
            else -> listOf(
                QuickPrompt("Brainstorm", "Help me brainstorm 5 unique ideas for a weekend project.", "💡"),
                QuickPrompt("Explain Simple", "Explain quantum computing simply like I'm 12 years old.", "🧠"),
                QuickPrompt("Daily Motivation", "Share a powerful quote and thought to start my day with focus.", "☀️"),
                QuickPrompt("Time Management", "Give me a 3-step technique to beat procrastination today.", "⏱️")
            )
        }
    }
}
