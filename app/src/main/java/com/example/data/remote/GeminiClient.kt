package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.ChatMessageEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    fun hasValidApiKey(): Boolean {
        val key = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun generateReply(
        systemPrompt: String,
        recentMessages: List<ChatMessageEntity>,
        userMessage: String,
        personaName: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (hasValidApiKey()) {
            try {
                // Build contents history
                val contents = mutableListOf<GeminiContent>()
                for (msg in recentMessages.takeLast(10)) {
                    val role = if (msg.sender == "user") "user" else "model"
                    contents.add(
                        GeminiContent(
                            role = role,
                            parts = listOf(GeminiPart(text = msg.content))
                        )
                    )
                }
                // Add current user prompt
                contents.add(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = userMessage))
                    )
                )

                val request = GeminiRequest(
                    contents = contents,
                    systemInstruction = if (systemPrompt.isNotBlank()) {
                        GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
                    } else null,
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.7f,
                        topP = 0.95f,
                        maxOutputTokens = 1500
                    )
                )

                val response = service.generateContent(apiKey = apiKey, request = request)
                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!replyText.isNullOrBlank()) {
                    return@withContext replyText
                } else if (response.error != null) {
                    Log.w(TAG, "Gemini API error: ${response.error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to call Gemini API, falling back to smart persona response", e)
            }
        }

        // Realistic typing simulation delay for offline / smart local persona response
        delay(900)
        generateIntelligentPersonaFallback(personaName, userMessage)
    }

    private fun generateIntelligentPersonaFallback(personaName: String, prompt: String): String {
        val clean = prompt.trim().lowercase()

        return when {
            personaName.contains("DevByte", ignoreCase = true) -> {
                when {
                    clean.contains("kotlin") || clean.contains("code") || clean.contains("flow") -> {
                        "Here is a recommended idiomatic Kotlin approach:\n\n```kotlin\nclass ChatViewModel(private val repository: ChatRepository) : ViewModel() {\n    val messages = repository.getMessages(sessionId)\n        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())\n}\n```\nKey advantages:\n• Unidirectional data flow\n• Clean lifecycle scoping\n• Prevents unnecessary recompositions!"
                    }
                    clean.contains("architecture") || clean.contains("pattern") -> {
                        "In modern Android, MVVM + Clean Architecture is the gold standard:\n\n1. **Data Layer**: Room SQLite Database + Retrofit APIs\n2. **Domain/Repository**: Single source of truth, reactive Flows\n3. **UI Layer**: Jetpack Compose with unidirectional StateFlow\n\nWould you like me to walk through a repository or dependency injection pattern?"
                    }
                    clean.contains("bug") || clean.contains("error") -> {
                        "Let's troubleshoot that! Could you paste the stack trace or the exact line triggering the exception? I'll analyze thread boundaries, nullability, or coroutine context issues."
                    }
                    else -> {
                        "I've analyzed your query about: \"$prompt\".\n\nPragmatic recommendation:\n1. Break it down into testable units.\n2. Ensure thread-safety on background dispatchers (`Dispatchers.IO`).\n3. Leverage immutability in Compose state objects.\n\nLet me know if you want a code snippet or benchmark comparison!"
                    }
                }
            }

            personaName.contains("Lyra", ignoreCase = true) -> {
                when {
                    clean.contains("poem") || clean.contains("verse") -> {
                        "A spark across the digital deep,\nWhere whispered thoughts and wonders leap.\nThrough indigo skies and silent keys,\nOur voices drift like starlit breeze.\n\nWhat mood shall our next stanza explore?"
                    }
                    clean.contains("story") -> {
                        "The old library clock ticked backward once, then twice. At first, Julian assumed it was fatigue from hours spent deciphering forgotten celestial cartographies. But when the ink on page forty-seven began to shimmer like liquid amethyst, he knew the map was no longer describing where stars had been—it was charting where they were going to fall.\n\nShall Julian follow the constellations or seal the tome?"
                    }
                    else -> {
                        "\"$prompt\"—an intriguing prompt for our narrative loom. Words have a melody of their own, and in this quiet moment, the possibilities stretch outward like morning mist across a sunlit valley. What atmosphere shall we weave next?"
                    }
                }
            }

            personaName.contains("Marco", ignoreCase = true) || personaName.contains("Chef", ignoreCase = true) -> {
                when {
                    clean.contains("pasta") || clean.contains("italian") -> {
                        "Ah, bellissima! The secret to authentic pasta:\n\n1. **Pasta Water**: Salt it until it tastes like gentle seawater!\n2. **Mantecatura**: Reserve 1/2 cup of starchy pasta water to emulsify with extra virgin olive oil and freshly grated Parmigiano-Reggiano.\n3. **Al Dente**: Pull it 1 minute before package directions and finish cooking directly inside the sauce.\n\nBuon appetito!"
                    }
                    clean.contains("dinner") || clean.contains("quick") || clean.contains("recipe") -> {
                        "Here is my 15-minute skillet sensation:\n\n• **Crispy Garlic Butter Skillet Chicken or Mushrooms**\n• Sauté minced garlic and shallots in olive oil\n• Sear at high heat with fresh rosemary & a squeeze of lemon juice\n• Finish with a splash of cream or white grape juice.\n\nServe over crusty bread or warm quinoa! What do you think?"
                    }
                    else -> {
                        "Magnifico! Cooking is all about balance—salt, acid, heat, and fat. Regarding \"$prompt\", always remember to season in layers and taste as you go. What ingredients are calling your name today?"
                    }
                }
            }

            personaName.contains("Atlas", ignoreCase = true) -> {
                when {
                    clean.contains("japan") || clean.contains("tokyo") || clean.contains("kyoto") -> {
                        "Japan is pure magic! A traveler's insider tip:\n\n• In **Kyoto**, skip the crowded afternoon at Fushimi Inari and hike the mountain trail at sunrise.\n• In **Tokyo**, explore the bohemian alleys of Shimokitazawa for vintage vinyl and hand-drip espresso.\n• Don't miss a local Ekiben (bento box) on the Shinkansen bullet train!\n\nShall we map out a 7-day itinerary?"
                    }
                    clean.contains("pack") || clean.contains("trip") -> {
                        "My top rule for globetrotting: **One carry-on, endless freedom**.\n\n1. Merino wool layers (breathable & odor-resistant)\n2. Universal adapter with dual USB-C ports\n3. Download offline maps and keep a digital scan of your passport in secure storage.\n\nWhere is your compass pointing next?"
                    }
                    else -> {
                        "The world is a book, and those who do not travel read only one page! Regarding \"$prompt\": whether you are exploring bustling night markets or serene alpine trails, the best memories come from spontaneous detours. Where are we heading?"
                    }
                }
            }

            else -> {
                // Nova / General Assistant
                when {
                    clean.contains("hello") || clean.contains("hi") || clean.contains("hey") -> {
                        "Hello! Wonderful to connect with you. I'm ready to help you brainstorm ideas, organize your schedule, answer complex questions, or simply have an inspiring chat. What's on your mind today?"
                    }
                    clean.contains("joke") || clean.contains("funny") -> {
                        "Why do programmers prefer dark mode?\n\nBecause light attracts bugs! 🐛\n\nHope that brought a smile to your screen!"
                    }
                    clean.contains("help") || clean.contains("feature") -> {
                        "Here is what you can do in OmniChat:\n\n• **AI Personas**: Switch between specialized guides (DevByte for code, Lyra for writing, Chef Marco, Atlas).\n• **Voice & Audio**: Send voice notes and simulate audio calls.\n• **Attachments**: Share photos and quick media.\n• **Search & Star**: Bookmark important insights for later.\n• **Real AI**: Connect your Gemini API key in AI Studio Secrets for live streaming intelligence!"
                    }
                    else -> {
                        "Thank you for sharing that! That is a fascinating perspective on \"$prompt\".\n\nHere are three key insights to consider:\n1. **Clarity**: Focusing on one primary objective yields the quickest progress.\n2. **Perspective**: Exploring counter-arguments often reveals fresh angles.\n3. **Next Steps**: We can break this into actionable milestones whenever you are ready.\n\nHow would you like to proceed?"
                    }
                }
            }
        }
    }
}
