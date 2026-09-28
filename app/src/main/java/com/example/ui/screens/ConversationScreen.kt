package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.ui.components.AvatarBadge
import com.example.ui.components.ChatInputBar
import com.example.ui.components.MessageBubble
import com.example.ui.components.QuickPromptRow
import com.example.ui.components.TypingIndicator
import com.example.ui.model.QuickPrompts
import com.example.ui.model.Screen
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: ChatViewModel,
    session: ChatSessionEntity?,
    messages: List<ChatMessageEntity>,
    isAiTyping: Boolean,
    inputText: String,
    isRecordingVoice: Boolean,
    recordingDurationSeconds: Int,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val listState = rememberLazyListState()
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Auto-scroll to bottom on new message or typing state change
    LaunchedEffect(messages.size, isAiTyping) {
        val totalCount = messages.size + if (isAiTyping) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    val contactName = session?.contactName ?: "Chat"
    val contactEmoji = session?.avatarEmoji ?: "💬"
    val contactColorHex = session?.avatarColorHex ?: "#6366F1"
    val contactRole = session?.contactRole ?: "Assistant"

    val prompts = remember(contactName) {
        QuickPrompts.getPromptsForPersona(contactName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                session?.let { viewModel.navigateTo(Screen.ContactProfile(it.id)) }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        AvatarBadge(
                            emoji = contactEmoji,
                            colorHex = contactColorHex,
                            size = 40.dp,
                            fontSize = 20,
                            isOnline = true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = contactName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val statusText = when {
                                isAiTyping -> "typing…"
                                session?.category == "Direct" -> "🟢 Online • Direct Message"
                                else -> contactRole
                            }
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                color = if (isAiTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("conversation_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Call button
                    IconButton(
                        onClick = {
                            session?.let { viewModel.navigateTo(Screen.Call(it.id)) }
                        },
                        modifier = Modifier.testTag("start_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Voice Call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Profile / Info button
                    IconButton(
                        onClick = {
                            session?.let { viewModel.navigateTo(Screen.ContactProfile(it.id)) }
                        },
                        modifier = Modifier.testTag("contact_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Contact Info"
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.testTag("conversation_more_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            session?.let { currentSession ->
                                DropdownMenuItem(
                                    text = { Text(if (currentSession.isPinned) "Unpin" else "Pin Chat") },
                                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.togglePin(currentSession.id, currentSession.isPinned)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Chat") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.clearChat(currentSession.id)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(modifier = Modifier.imePadding()) {
                // Quick suggestions chips
                if (messages.size <= 2 && prompts.isNotEmpty()) {
                    QuickPromptRow(
                        prompts = prompts,
                        onSelectPrompt = { promptText ->
                            viewModel.sendMessage(promptText)
                        }
                    )
                }

                ChatInputBar(
                    text = inputText,
                    onTextChanged = { viewModel.setInputText(it) },
                    onSendText = { viewModel.sendMessage() },
                    onAttachClick = { showAttachmentSheet = true },
                    isRecordingVoice = isRecordingVoice,
                    recordingDurationSeconds = recordingDurationSeconds,
                    onStartVoiceRecording = { viewModel.startVoiceRecording() },
                    onStopVoiceRecording = { send -> viewModel.stopVoiceRecording(send) }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        contactEmoji = contactEmoji,
                        contactColorHex = contactColorHex,
                        onToggleStar = { viewModel.toggleStar(message) },
                        onSetReaction = { emoji -> viewModel.setReaction(message.id, emoji) },
                        onDelete = { viewModel.deleteMessage(message.id) }
                    )
                }

                if (isAiTyping) {
                    item {
                        TypingIndicator(
                            contactEmoji = contactEmoji,
                            contactColorHex = contactColorHex
                        )
                    }
                }
            }
        }
    }

    // Attachment Modal Bottom Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Share Content",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AttachmentOptionCard(
                        icon = Icons.Default.Image,
                        title = "Photo",
                        subtitle = "Send an image",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            showAttachmentSheet = false
                            viewModel.sendImageMessage("Scenic nature landscape photo")
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    AttachmentOptionCard(
                        icon = Icons.Default.Code,
                        title = "Code",
                        subtitle = "Share snippet",
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            showAttachmentSheet = false
                            viewModel.sendMessage("Can you review this code:\n```kotlin\nsuspend fun load() = withContext(Dispatchers.IO) { ... }\n```")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AttachmentOptionCard(
                        icon = Icons.Default.Mic,
                        title = "Voice Note",
                        subtitle = "3-sec voice sample",
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = {
                            showAttachmentSheet = false
                            session?.let {
                                viewModel.startVoiceRecording()
                                // stop and send after 3 seconds
                                viewModel.stopVoiceRecording(true)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    AttachmentOptionCard(
                        icon = Icons.Default.Call,
                        title = "Voice Call",
                        subtitle = "Simulate live call",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            showAttachmentSheet = false
                            session?.let { viewModel.navigateTo(Screen.Call(it.id)) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun AttachmentOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
