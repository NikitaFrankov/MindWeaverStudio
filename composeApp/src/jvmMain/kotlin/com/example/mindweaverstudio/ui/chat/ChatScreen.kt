package com.example.mindweaverstudio.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mindweaverstudio.components.chat.ChatComponent
import com.example.mindweaverstudio.components.chat.ChatStore
import com.example.mindweaverstudio.ui.model.UiChatMessage
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ChatScreen(component: ChatComponent) {
    val state by component.state.collectAsStateWithLifecycle()
    
    ChatScreen(
        state = state,
        intentHandler = component::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    state: ChatStore.State,
    intentHandler: (ChatStore.Intent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Chat with ai assistant",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                // Provider selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Provider:", style = MaterialTheme.typography.bodySmall)
                    FilterChip(
                        onClick = { intentHandler(ChatStore.Intent.ChangeProvider("DeepSeek")) },
                        label = { Text("DeepSeek") },
                        selected = state.selectedProvider == "DeepSeek"
                    )
                    FilterChip(
                        onClick = { intentHandler(ChatStore.Intent.ChangeProvider("ChatGPT")) },
                        label = { Text("ChatGPT") },
                        selected = state.selectedProvider == "ChatGPT"
                    )
                    FilterChip(
                        onClick = { intentHandler(ChatStore.Intent.ChangeProvider("Gemini")) },
                        label = { Text("Gemini") },
                        selected = state.selectedProvider == "Gemini"
                    )
                }
            }
            
            IconButton(
                onClick = { intentHandler(ChatStore.Intent.ClearChat) }
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear Chat")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Messages
        val listState = rememberLazyListState()
        
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.messages) { message ->
                when(message) {
                    is UiChatMessage.AssistantMessage -> AssistantMessage(message)
                    is UiChatMessage.UserMessage -> UserMessageBubble(message)
                }
            }
            
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        
        // Scroll to bottom when new message arrives
        LaunchedEffect(state.messages.size) {
            if (state.messages.isNotEmpty()) {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.currentMessage,
                onValueChange = { intentHandler(ChatStore.Intent.UpdateMessage(it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type your message...") },
                enabled = !state.isLoading,
                maxLines = 3
            )
            
            Button(
                onClick = { intentHandler(ChatStore.Intent.SendMessage) },
                enabled = state.currentMessage.isNotBlank() && !state.isLoading
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Send")
            }
        }
        
        // Error handling
        state.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    
                    TextButton(
                        onClick = { intentHandler(ChatStore.Intent.ClearError) }
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(message: UiChatMessage.UserMessage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.End)
            .widthIn(max = 280.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = message.presentableContent,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun AssistantMessage(message: UiChatMessage.AssistantMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            val data = message.structuredOutput
            // Главный ответ
            val answerText = when (data.answer.type) {
                "number" -> data.answer.value.jsonPrimitive.doubleOrNull?.toString() ?: data.answer.value.toString()
                "string" -> data.answer.value.jsonPrimitive.contentOrNull ?: data.answer.value.toString()
                "boolean" -> data.answer.value.jsonPrimitive.booleanOrNull?.toString() ?: data.answer.value.toString()
                else -> data.answer.value.toString()
            }

            Text(
                text = "Ответ: $answerText",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Шаги / пункты
            if (data.steps.isNotEmpty()) {
                Text(
                    "Шаги:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                data.steps.forEachIndexed { index, point ->
                    val color = Color(0xFF4CAF50)
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "•",
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(text = point.text)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))


            // Шаги / пункты
            if (data.factAndPoints.isNotEmpty()) {
                Text(
                    "Важные моменты:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                data.factAndPoints.forEachIndexed { index, point ->
                    val color = when (point.kind) {
                        "fact" -> Color.Blue // синий для факта
                        "note" -> Color.Magenta // жёлтый для заметки
                        else -> Color.Gray
                    }
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "•",
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(text = point.text)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary
            Text(
                text = "Краткое описание: ${data.summary.text}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Meta (если есть)
            data.meta?.let { meta ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    meta.confidence?.let { conf ->
                        Text(text = "Уверенность: ${(conf * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                    meta.source?.let { src ->
                        Text(text = "Источник: $src", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}