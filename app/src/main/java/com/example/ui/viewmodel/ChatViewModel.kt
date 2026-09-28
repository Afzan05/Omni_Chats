package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.UserEntity
import com.example.data.remote.GeminiClient
import com.example.data.repository.AuthRepository
import com.example.data.repository.ChatRepository
import com.example.ui.model.AuthMode
import com.example.ui.model.AuthStep
import com.example.ui.model.ChatFilter
import com.example.ui.model.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val repository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // --- Authentication State ---
    val activeUser: StateFlow<UserEntity?> = authRepository.getActiveUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Auth)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _screenStack = mutableListOf<Screen>(Screen.Auth)

    private val _authMode = MutableStateFlow(AuthMode.SIGN_IN)
    val authMode: StateFlow<AuthMode> = _authMode.asStateFlow()

    private val _authStep = MutableStateFlow(AuthStep.PHONE_INPUT)
    val authStep: StateFlow<AuthStep> = _authStep.asStateFlow()

    private val _authPhoneInput = MutableStateFlow("")
    val authPhoneInput: StateFlow<String> = _authPhoneInput.asStateFlow()

    private val _authCountryCode = MutableStateFlow("+1")
    val authCountryCode: StateFlow<String> = _authCountryCode.asStateFlow()

    private val _authNameInput = MutableStateFlow("")
    val authNameInput: StateFlow<String> = _authNameInput.asStateFlow()

    private val _authOtpInput = MutableStateFlow("")
    val authOtpInput: StateFlow<String> = _authOtpInput.asStateFlow()

    private val _authAvatarEmoji = MutableStateFlow("😎")
    val authAvatarEmoji: StateFlow<String> = _authAvatarEmoji.asStateFlow()

    private val _authAvatarColorHex = MutableStateFlow("#4F46E5")
    val authAvatarColorHex: StateFlow<String> = _authAvatarColorHex.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _simulatedOtpNotification = MutableStateFlow<String?>(null)
    val simulatedOtpNotification: StateFlow<String?> = _simulatedOtpNotification.asStateFlow()

    private val _otpCountdown = MutableStateFlow(0)
    val otpCountdown: StateFlow<Int> = _otpCountdown.asStateFlow()
    private var otpTimerJob: Job? = null

    // --- Chat & Session State ---
    private val _activeFilter = MutableStateFlow(ChatFilter.ALL)
    val activeFilter: StateFlow<ChatFilter> = _activeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allSessions: StateFlow<List<ChatSessionEntity>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSessions: StateFlow<List<ChatSessionEntity>> = combine(
        allSessions,
        _activeFilter,
        _searchQuery
    ) { sessions, filter, query ->
        sessions.filter { session ->
            val matchesFilter = when (filter) {
                ChatFilter.ALL -> true
                ChatFilter.PINNED -> session.isPinned
                ChatFilter.AI -> session.category == "AI"
                ChatFilter.DIRECT -> session.category != "AI"
            }
            val matchesQuery = if (query.isBlank()) true else {
                session.contactName.contains(query, ignoreCase = true) ||
                        session.contactRole.contains(query, ignoreCase = true) ||
                        session.lastMessage.contains(query, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    val activeSession: StateFlow<ChatSessionEntity?> = _activeSessionId.flatMapLatest { id ->
        if (id != null) repository.getSession(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val messages: StateFlow<List<ChatMessageEntity>> = _activeSessionId.flatMapLatest { id ->
        if (id != null) repository.getMessages(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val starredMessages: StateFlow<List<ChatMessageEntity>> = repository.getStarredMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAiTyping = MutableStateFlow(false)
    val isAiTyping: StateFlow<Boolean> = _isAiTyping.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // Voice recording simulation
    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()
    private var voiceTimerJob: Job? = null

    // Audio Call simulation
    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds: StateFlow<Int> = _callDurationSeconds.asStateFlow()

    private val _isCallMuted = MutableStateFlow(false)
    val isCallMuted: StateFlow<Boolean> = _isCallMuted.asStateFlow()

    private val _isCallSpeaker = MutableStateFlow(false)
    val isCallSpeaker: StateFlow<Boolean> = _isCallSpeaker.asStateFlow()

    val allRegisteredUsers: StateFlow<List<UserEntity>> = authRepository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val otherUsers: StateFlow<List<UserEntity>> = combine(
        allRegisteredUsers,
        activeUser
    ) { users, current ->
        val currentPhone = current?.phoneNumber
        users.filter { it.phoneNumber != currentPhone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dmSearchQuery = MutableStateFlow("")
    val dmSearchQuery: StateFlow<String> = _dmSearchQuery.asStateFlow()

    fun setDmSearchQuery(query: String) {
        _dmSearchQuery.value = query
    }

    private var callTimerJob: Job? = null

    val isGeminiConfigured: Boolean
        get() = GeminiClient.hasValidApiKey()

    init {
        viewModelScope.launch {
            authRepository.ensureDefaultUsers()
        }
        // Observe login session state to route between Auth and Chat
        viewModelScope.launch {
            authRepository.getActiveUser().collect { user ->
                if (user != null && user.isLoggedIn) {
                    if (_currentScreen.value == Screen.Auth) {
                        _screenStack.clear()
                        _screenStack.add(Screen.ChatList)
                        _currentScreen.value = Screen.ChatList
                    }
                } else {
                    _screenStack.clear()
                    _screenStack.add(Screen.Auth)
                    _currentScreen.value = Screen.Auth
                }
            }
        }
    }

    // --- Authentication Actions ---

    fun setAuthMode(mode: AuthMode) {
        _authMode.value = mode
        _authErrorMessage.value = null
        _authStep.value = AuthStep.PHONE_INPUT
    }

    fun setAuthPhoneInput(phone: String) {
        _authPhoneInput.value = phone.filter { it.isDigit() }
        _authErrorMessage.value = null
    }

    fun setAuthCountryCode(code: String) {
        _authCountryCode.value = code
    }

    fun setAuthNameInput(name: String) {
        _authNameInput.value = name
        _authErrorMessage.value = null
    }

    fun setAuthOtpInput(otp: String) {
        _authOtpInput.value = otp.filter { it.isDigit() }.take(6)
        _authErrorMessage.value = null
    }

    fun setAuthAvatar(emoji: String, colorHex: String) {
        _authAvatarEmoji.value = emoji
        _authAvatarColorHex.value = colorHex
    }

    fun resetAuthToPhoneStep() {
        _authStep.value = AuthStep.PHONE_INPUT
        _authOtpInput.value = ""
        _authErrorMessage.value = null
    }

    fun dismissOtpNotification() {
        _simulatedOtpNotification.value = null
    }

    fun autoFillOtp(code: String) {
        _authOtpInput.value = code
    }

    fun requestOtp() {
        val phoneDigits = _authPhoneInput.value.trim()
        if (phoneDigits.length < 7) {
            _authErrorMessage.value = "Please enter a valid mobile number (at least 7 digits)"
            return
        }

        val fullPhoneNumber = "${_authCountryCode.value}$phoneDigits"

        if (_authMode.value == AuthMode.SIGN_UP && _authNameInput.value.trim().isBlank()) {
            _authErrorMessage.value = "Please enter your name"
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            delay(500) // Realistic secure network dispatch delay

            if (_authMode.value == AuthMode.SIGN_IN) {
                val exists = authRepository.checkUserExists(fullPhoneNumber)
                if (!exists) {
                    _isAuthLoading.value = false
                    _authErrorMessage.value = "No account found with $fullPhoneNumber. Switch to Sign Up to create one!"
                    return@launch
                }
            }

            val plainOtp = authRepository.sendOtp(fullPhoneNumber)
            _isAuthLoading.value = false
            _authStep.value = AuthStep.OTP_VERIFICATION
            _simulatedOtpNotification.value = plainOtp
            startOtpCountdown()
        }
    }

    private fun startOtpCountdown() {
        _otpCountdown.value = 60
        otpTimerJob?.cancel()
        otpTimerJob = viewModelScope.launch {
            while (_otpCountdown.value > 0) {
                delay(1000)
                _otpCountdown.value -= 1
            }
        }
    }

    fun resendOtp() {
        val fullPhoneNumber = "${_authCountryCode.value}${_authPhoneInput.value.trim()}"
        val plainOtp = authRepository.sendOtp(fullPhoneNumber)
        _simulatedOtpNotification.value = plainOtp
        startOtpCountdown()
    }

    fun verifyOtpAndAuthenticate() {
        val enteredOtp = _authOtpInput.value.trim()
        if (enteredOtp.length < 6) {
            _authErrorMessage.value = "Please enter the full 6-digit verification code"
            return
        }

        val fullPhoneNumber = "${_authCountryCode.value}${_authPhoneInput.value.trim()}"

        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            delay(600) // Verification processing delay

            val result = if (_authMode.value == AuthMode.SIGN_IN) {
                authRepository.verifyOtpAndLogin(fullPhoneNumber, enteredOtp)
            } else {
                authRepository.signUpWithOtp(
                    phoneNumber = fullPhoneNumber,
                    name = _authNameInput.value.trim(),
                    countryCode = _authCountryCode.value,
                    emoji = _authAvatarEmoji.value,
                    colorHex = _authAvatarColorHex.value,
                    enteredOtp = enteredOtp
                )
            }

            _isAuthLoading.value = false
            result.onSuccess {
                _simulatedOtpNotification.value = null
                _authOtpInput.value = ""
                _screenStack.clear()
                _screenStack.add(Screen.ChatList)
                _currentScreen.value = Screen.ChatList
            }.onFailure { error ->
                _authErrorMessage.value = error.message ?: "Authentication failed. Please try again."
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _screenStack.clear()
            _screenStack.add(Screen.Auth)
            _currentScreen.value = Screen.Auth
            _authStep.value = AuthStep.PHONE_INPUT
            _authOtpInput.value = ""
            _authPhoneInput.value = ""
        }
    }

    // --- Navigation ---

    fun navigateTo(screen: Screen) {
        _screenStack.add(screen)
        _currentScreen.value = screen
        if (screen is Screen.Conversation) {
            _activeSessionId.value = screen.sessionId
            viewModelScope.launch {
                repository.markSessionRead(screen.sessionId)
            }
        } else if (screen is Screen.Call) {
            _activeSessionId.value = screen.sessionId
            startCallTimer()
        }
    }

    fun navigateBack(): Boolean {
        if (_screenStack.size > 1) {
            _screenStack.removeAt(_screenStack.lastIndex)
            val previous = _screenStack.last()
            _currentScreen.value = previous
            if (previous is Screen.Conversation) {
                _activeSessionId.value = previous.sessionId
            } else if (previous is Screen.ChatList) {
                _activeSessionId.value = null
            }
            return true
        }
        return false
    }

    fun setFilter(filter: ChatFilter) {
        _activeFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun startDirectMessage(user: UserEntity) {
        viewModelScope.launch {
            val sessionId = repository.getOrCreateDirectSession(user)
            navigateTo(Screen.Conversation(sessionId))
        }
    }

    fun startDirectMessageByPhone(name: String, phoneNumber: String) {
        viewModelScope.launch {
            val cleanPhone = phoneNumber.trim()
            val existing = allRegisteredUsers.value.find { it.phoneNumber == cleanPhone }
            val user = existing ?: UserEntity(
                phoneNumber = cleanPhone,
                name = name.ifBlank { cleanPhone },
                countryCode = "+1",
                avatarEmoji = "💬",
                avatarColorHex = "#6366F1",
                salt = "salt",
                credentialHash = "hash"
            )
            val sessionId = repository.getOrCreateDirectSession(user)
            navigateTo(Screen.Conversation(sessionId))
        }
    }

    fun sendMessage(text: String = _inputText.value) {
        val trimmed = text.trim()
        val sessionId = _activeSessionId.value ?: return
        if (trimmed.isBlank()) return

        _inputText.value = ""
        viewModelScope.launch {
            val userMsg = repository.sendUserMessage(sessionId, trimmed)

            // Step 1: Real-time message delivery progression
            delay(250)
            repository.updateDeliveryStatus(userMsg.id, "SENT")

            delay(400)
            repository.updateDeliveryStatus(userMsg.id, "DELIVERED")

            delay(550)
            repository.updateDeliveryStatus(userMsg.id, "READ")

            val currentSession = activeSession.value
            val isDirectMessage = currentSession?.category == "Direct"

            _isAiTyping.value = true
            try {
                if (isDirectMessage) {
                    delay(1200) // realistic peer typing delay
                    val reply = generateDirectMessageReply(currentSession?.contactName ?: "Contact", trimmed)
                    repository.receiveDirectMessage(sessionId, reply)
                } else {
                    repository.getAiResponse(sessionId, trimmed)
                }
            } finally {
                _isAiTyping.value = false
            }
        }
    }

    private fun generateDirectMessageReply(contactName: String, userMessage: String): String {
        return when {
            contactName.contains("Elena", ignoreCase = true) -> {
                val replies = listOf(
                    "Hey! I checked the design specs—the contrast and font hierarchy look super clean! ✨",
                    "Love this! I'm updating our Figma design tokens right now to match.",
                    "Awesome! Are we good to present this in the product review later today? 🎨",
                    "Got your message! I'm tweaking the micro-interaction transitions now."
                )
                replies.random()
            }
            contactName.contains("Marcus", ignoreCase = true) -> {
                val replies = listOf(
                    "Hey! Just reviewed your code. The reactive Room queries and coroutines look rock solid! 🚀",
                    "All unit tests and integration checks are green on CI. Ready to merge whenever you are.",
                    "Great work on keeping the components decoupled. Very clean implementation.",
                    "Sounds like a plan! Let me pull the latest branch and run a local verification build."
                )
                replies.random()
            }
            contactName.contains("Aria", ignoreCase = true) -> {
                val replies = listOf(
                    "Hey there! Just tested the direct messaging UI on device—the animations and transitions are so smooth! 📱",
                    "I love how fast the message delivery receipts update. Feels super responsive!",
                    "Awesome idea! Let me know if you want to collaborate on the next feature set.",
                    "Hey! Great to hear from you. Everything looks fantastic on my end!"
                )
                replies.random()
            }
            contactName.contains("Kai", ignoreCase = true) -> {
                val replies = listOf(
                    "Hey! Just tuned the audio levels and acoustic filters. Loving the vibes! 🎧",
                    "Got your note! I'm in the studio working on new soundscapes—will ping you back soon!",
                    "That sounds brilliant. Let's sync up over coffee this week!"
                )
                replies.random()
            }
            contactName.contains("Chloe", ignoreCase = true) -> {
                val replies = listOf(
                    "Hey! Just got back from field research—the views were breathtaking! 🏔️ Hope you're having a wonderful day!",
                    "Thanks for checking in! Everything is going great on my side. Talk soon!",
                    "That's fantastic news! So excited for how the project is shaping up."
                )
                replies.random()
            }
            else -> {
                val replies = listOf(
                    "Hey! Got your message. That sounds great, let's keep in touch! 👍",
                    "Thanks for reaching out! I'm looking into that right now.",
                    "Appreciate the update! Let me know if you need anything else on my end.",
                    "Hey! Everything looks good here. Talk to you soon!"
                )
                replies.random()
            }
        }
    }

    fun sendImageMessage(description: String = "Attached photo") {
        val sessionId = _activeSessionId.value ?: return
        viewModelScope.launch {
            val userMsg = repository.sendUserMessage(
                sessionId = sessionId,
                content = description,
                messageType = "IMAGE",
                mediaUri = "sample_image"
            )
            delay(250)
            repository.updateDeliveryStatus(userMsg.id, "SENT")
            delay(400)
            repository.updateDeliveryStatus(userMsg.id, "DELIVERED")
            delay(550)
            repository.updateDeliveryStatus(userMsg.id, "READ")

            val currentSession = activeSession.value
            val isDirectMessage = currentSession?.category == "Direct"

            _isAiTyping.value = true
            try {
                if (isDirectMessage) {
                    delay(1200)
                    val reply = "Photo received! Looks great. 📷✨"
                    repository.receiveDirectMessage(sessionId, reply)
                } else {
                    repository.getAiResponse(sessionId, "I sent an image: $description")
                }
            } finally {
                _isAiTyping.value = false
            }
        }
    }

    fun startVoiceRecording() {
        _isRecordingVoice.value = true
        _recordingDurationSeconds.value = 0
        voiceTimerJob?.cancel()
        voiceTimerJob = viewModelScope.launch {
            while (_isRecordingVoice.value) {
                delay(1000)
                _recordingDurationSeconds.value += 1
            }
        }
    }

    fun stopVoiceRecording(send: Boolean) {
        voiceTimerJob?.cancel()
        val duration = _recordingDurationSeconds.value
        _isRecordingVoice.value = false
        _recordingDurationSeconds.value = 0

        val sessionId = _activeSessionId.value ?: return
        if (send && duration > 0) {
            viewModelScope.launch {
                val userMsg = repository.sendUserMessage(
                    sessionId = sessionId,
                    content = "Voice note (${duration}s)",
                    messageType = "VOICE",
                    audioDurationSec = duration
                )
                delay(250)
                repository.updateDeliveryStatus(userMsg.id, "SENT")
                delay(400)
                repository.updateDeliveryStatus(userMsg.id, "DELIVERED")
                delay(550)
                repository.updateDeliveryStatus(userMsg.id, "READ")

                val currentSession = activeSession.value
                val isDirectMessage = currentSession?.category == "Direct"

                _isAiTyping.value = true
                try {
                    if (isDirectMessage) {
                        delay(1200)
                        val reply = "Listened to your voice message! Loud and clear. 🎙️👍"
                        repository.receiveDirectMessage(sessionId, reply)
                    } else {
                        repository.getAiResponse(sessionId, "I sent a voice note (${duration}s)")
                    }
                } finally {
                    _isAiTyping.value = false
                }
            }
        }
    }

    private fun startCallTimer() {
        _isCallActive.value = true
        _callDurationSeconds.value = 0
        _isCallMuted.value = false
        _isCallSpeaker.value = false
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (_isCallActive.value) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    fun toggleCallMute() {
        _isCallMuted.value = !_isCallMuted.value
    }

    fun toggleCallSpeaker() {
        _isCallSpeaker.value = !_isCallSpeaker.value
    }

    fun endCall() {
        callTimerJob?.cancel()
        _isCallActive.value = false
        _callDurationSeconds.value = 0
        navigateBack()
    }

    fun toggleStar(message: ChatMessageEntity) {
        viewModelScope.launch {
            repository.toggleStar(message.id, !message.isStarred)
        }
    }

    fun setReaction(messageId: Long, emoji: String) {
        viewModelScope.launch {
            repository.setReaction(messageId, emoji)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun clearChat(sessionId: String) {
        viewModelScope.launch {
            repository.clearChat(sessionId)
        }
    }

    fun togglePin(sessionId: String, isCurrentlyPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePin(sessionId, !isCurrentlyPinned)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                navigateBack()
            }
        }
    }

    fun createNewSession(
        name: String,
        role: String,
        emoji: String,
        colorHex: String,
        systemPrompt: String,
        greeting: String
    ) {
        viewModelScope.launch {
            val newId = repository.createSession(
                title = name,
                contactName = name,
                contactRole = role,
                emoji = emoji,
                colorHex = colorHex,
                systemPrompt = systemPrompt,
                initialGreeting = greeting
            )
            navigateTo(Screen.Conversation(newId))
        }
    }

    companion object {
        fun provideFactory(
            repository: ChatRepository,
            authRepository: AuthRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatViewModel(repository, authRepository) as T
                }
            }
    }
}
