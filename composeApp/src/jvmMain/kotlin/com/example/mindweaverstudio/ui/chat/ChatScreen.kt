package com.example.mindweaverstudio.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mindweaverstudio.components.chat.ChatComponent
import com.example.mindweaverstudio.components.chat.ChatStore
import com.example.mindweaverstudio.data.model.chat.ChatMessage
import com.example.mindweaverstudio.data.model.chat.StructuredOutput
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ChatScreen(component: ChatComponent) {
    val state by component.state.collectAsState()
    
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
                MessageBubble(message)
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
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatMessage.ROLE_USER
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isUser) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.presentableContent,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StructuredOutputView(data: StructuredOutput) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Главный ответ
            val answerText = when (data.answer.type) {
                "number" -> data.answer.value.jsonPrimitive.doubleOrNull?.toString() ?: data.answer.value.toString()
                "string" -> data.answer.value.jsonPrimitive.contentOrNull ?: data.answer.value.toString()
                "boolean" -> data.answer.value.jsonPrimitive.booleanOrNull?.toString() ?: data.answer.value.toString()
                else -> data.answer.value.toString()
            }

            Text(
                text = "Ответ: $answerText",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Шаги / пункты
            if (data.points.isNotEmpty()) {
                Text(
                    "Шаги / важные моменты:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 150.dp)
                ) {
                    itemsIndexed(data.points) { index, point ->
                        val color = when (point.kind) {
                            "step" -> Color(0xFF4CAF50) // зелёный для шага
                            "fact" -> Color(0xFF2196F3) // синий для факта
                            "note" -> Color(0xFFFFC107) // жёлтый для заметки
                            else -> Color.Gray
                        }
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "${index + 1}.",
                                color = color,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(24.dp)
                            )
                            Text(text = point.text)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary
            Text(
                text = "Пояснение: ${data.summary.text}",
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