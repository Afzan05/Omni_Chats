package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ChatDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.ChatRepository
import com.example.ui.model.Screen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CallScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.ContactProfileScreen
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.NewChatDialog
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.StarredMessagesScreen
import com.example.ui.screens.UserDirectoryScreen
import com.example.ui.theme.OmniChatTheme
import com.example.ui.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val database = ChatDatabase.getDatabase(applicationContext)
        val chatRepository = ChatRepository(database.chatDao())
        val authRepository = AuthRepository(database.userDao())
        ChatViewModel.provideFactory(chatRepository, authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmniChatTheme {
                OmniChatApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun OmniChatApp(viewModel: ChatViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()

    // Auth States
    val authMode by viewModel.authMode.collectAsStateWithLifecycle()
    val authStep by viewModel.authStep.collectAsStateWithLifecycle()
    val authPhoneInput by viewModel.authPhoneInput.collectAsStateWithLifecycle()
    val authCountryCode by viewModel.authCountryCode.collectAsStateWithLifecycle()
    val authNameInput by viewModel.authNameInput.collectAsStateWithLifecycle()
    val authOtpInput by viewModel.authOtpInput.collectAsStateWithLifecycle()
    val authAvatarEmoji by viewModel.authAvatarEmoji.collectAsStateWithLifecycle()
    val authAvatarColorHex by viewModel.authAvatarColorHex.collectAsStateWithLifecycle()
    val authErrorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val simulatedOtpNotification by viewModel.simulatedOtpNotification.collectAsStateWithLifecycle()
    val otpCountdown by viewModel.otpCountdown.collectAsStateWithLifecycle()

    // Chat States
    val filteredSessions by viewModel.filteredSessions.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val starredMessages by viewModel.starredMessages.collectAsStateWithLifecycle()
    val isAiTyping by viewModel.isAiTyping.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsStateWithLifecycle()
    val recordingDurationSeconds by viewModel.recordingDurationSeconds.collectAsStateWithLifecycle()
    val callDurationSeconds by viewModel.callDurationSeconds.collectAsStateWithLifecycle()
    val isCallMuted by viewModel.isCallMuted.collectAsStateWithLifecycle()
    val isCallSpeaker by viewModel.isCallSpeaker.collectAsStateWithLifecycle()

    var showNewChatDialog by remember { mutableStateOf(false) }

    when (val screen = currentScreen) {
        is Screen.Auth -> {
            AuthScreen(
                viewModel = viewModel,
                authMode = authMode,
                authStep = authStep,
                phoneInput = authPhoneInput,
                countryCode = authCountryCode,
                nameInput = authNameInput,
                otpInput = authOtpInput,
                avatarEmoji = authAvatarEmoji,
                avatarColorHex = authAvatarColorHex,
                errorMessage = authErrorMessage,
                isLoading = isAuthLoading,
                simulatedOtp = simulatedOtpNotification,
                otpCountdown = otpCountdown
            )
        }

        is Screen.ChatList -> {
            ChatListScreen(
                viewModel = viewModel,
                sessions = filteredSessions,
                activeFilter = activeFilter,
                searchQuery = searchQuery,
                onOpenNewChatDialog = { showNewChatDialog = true }
            )
        }

        is Screen.Conversation -> {
            ConversationScreen(
                viewModel = viewModel,
                session = activeSession,
                messages = messages,
                isAiTyping = isAiTyping,
                inputText = inputText,
                isRecordingVoice = isRecordingVoice,
                recordingDurationSeconds = recordingDurationSeconds
            )
        }

        is Screen.Call -> {
            CallScreen(
                viewModel = viewModel,
                session = activeSession,
                durationSeconds = callDurationSeconds,
                isMuted = isCallMuted,
                isSpeaker = isCallSpeaker
            )
        }

        is Screen.StarredMessages -> {
            StarredMessagesScreen(
                viewModel = viewModel,
                starredMessages = starredMessages
            )
        }

        is Screen.ContactProfile -> {
            ContactProfileScreen(
                viewModel = viewModel,
                session = activeSession
            )
        }

        is Screen.ProfileSettings -> {
            ProfileSettingsScreen(
                viewModel = viewModel,
                user = activeUser
            )
        }

        is Screen.UserDirectory -> {
            UserDirectoryScreen(
                viewModel = viewModel
            )
        }
    }

    if (showNewChatDialog) {
        NewChatDialog(
            onDismiss = { showNewChatDialog = false },
            onCreateChat = { name, role, emoji, colorHex, prompt, greeting ->
                showNewChatDialog = false
                viewModel.createNewSession(name, role, emoji, colorHex, prompt, greeting)
            }
        )
    }
}
