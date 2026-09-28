package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class PresetTemplate(
    val name: String,
    val role: String,
    val emoji: String,
    val colorHex: String,
    val prompt: String,
    val greeting: String
)

@Composable
fun NewChatDialog(
    onDismiss: () -> Unit,
    onCreateChat: (name: String, role: String, emoji: String, colorHex: String, prompt: String, greeting: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🤖") }
    var selectedColorHex by remember { mutableStateOf("#6366F1") }
    var prompt by remember { mutableStateOf("") }
    var greeting by remember { mutableStateOf("") }

    val presetTemplates = listOf(
        PresetTemplate(
            name = "Zen",
            role = "Mindfulness Coach",
            emoji = "🧘",
            colorHex = "#8B5CF6",
            prompt = "You are Zen, a calm, compassionate mindfulness guide. Offer grounding exercises, breathing techniques, and compassionate reflections.",
            greeting = "Take a deep breath and exhale slowly. Welcome to a quiet space for your mind. How can I support your peace today?"
        ),
        PresetTemplate(
            name = "FitCoach Max",
            role = "Personal Trainer",
            emoji = "🏋️",
            colorHex = "#EF4444",
            prompt = "You are FitCoach Max, a motivating, science-backed fitness and nutrition mentor. Give tailored workout splits, form tips, and protein targets.",
            greeting = "Ready to crush some goals? Let me know your workout target, equipment on hand, or nutrition question!"
        ),
        PresetTemplate(
            name = "Elena",
            role = "Polyglot Language Tutor",
            emoji = "📚",
            colorHex = "#0EA5E9",
            prompt = "You are Elena, a friendly and encouraging multilingual tutor. You teach conversational Spanish, French, Japanese, or German with corrections and vocabulary breakdowns.",
            greeting = "¡Hola! Bonjour! Which language would you like to practice today? We can do everyday roleplay or grammar drills."
        ),
        PresetTemplate(
            name = "Finley",
            role = "Personal Finance Mentor",
            emoji = "💰",
            colorHex = "#10B981",
            prompt = "You are Finley, a prudent, friendly financial educator. Explain budgeting (50/30/20), compound interest, savings habits, and index fund basics.",
            greeting = "Hello! Financial clarity is the best peace of mind. What savings, budgeting, or investing topic is on your mind?"
        )
    )

    val emojis = listOf("🤖", "✨", "⚡", "✍️", "🍳", "🌍", "🧘", "🏋️", "📚", "🎨", "🎵", "💰", "🩺", "🚀")
    val colors = listOf("#6366F1", "#10B981", "#EC4899", "#F97316", "#06B6D4", "#8B5CF6", "#EF4444", "#3B82F6")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Conversation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Presets Gallery
                Text(
                    text = "Quick Persona Presets",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetTemplates.forEach { preset ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable {
                                name = preset.name
                                role = preset.role
                                selectedEmoji = preset.emoji
                                selectedColorHex = preset.colorHex
                                prompt = preset.prompt
                                greeting = preset.greeting
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(preset.emoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = preset.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = preset.role,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name & Role Inputs
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact / Persona Name") },
                    placeholder = { Text("e.g. Alex, Math Wizard") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_chat_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role or Specialty") },
                    placeholder = { Text("e.g. Creative Director, Spanish Tutor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Emoji Picker
                Text(
                    text = "Avatar Emoji",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojis.forEach { emoji ->
                        val isSelected = selectedEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color Picker
                Text(
                    text = "Accent Color",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colors.forEach { hex ->
                        val color = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // System Prompt / Blueprint
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Personality & Knowledge Prompt") },
                    placeholder = { Text("Describe how they talk, what they specialize in, and their tone.") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Initial Greeting
                OutlinedTextField(
                    value = greeting,
                    onValueChange = { greeting = it },
                    label = { Text("Initial Greeting") },
                    placeholder = { Text("e.g. Hello! What can I help you with today?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onCreateChat(
                                    name.trim(),
                                    if (role.isNotBlank()) role.trim() else "AI Assistant",
                                    selectedEmoji,
                                    selectedColorHex,
                                    if (prompt.isNotBlank()) prompt.trim() else "You are a helpful assistant named $name.",
                                    if (greeting.isNotBlank()) greeting.trim() else "Hello! I'm $name. How can I help you today?"
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.testTag("create_chat_submit_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Chat")
                    }
                }
            }
        }
    }
}
