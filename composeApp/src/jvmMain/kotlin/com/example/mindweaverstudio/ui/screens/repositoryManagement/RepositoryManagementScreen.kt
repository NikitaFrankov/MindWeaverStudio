package com.example.mindweaverstudio.ui.screens.repositoryManagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementComponent
import com.example.mindweaverstudio.components.repositoryManagement.RepositoryManagementStore
import com.example.mindweaverstudio.ui.screens.repositoryManagement.models.UiRepositoryMessage
import com.example.mindweaverstudio.ui.theme.MindWeaverTheme

@Composable
fun RepositoryManagementScreen(component: RepositoryManagementComponent) {
    val state by component.state.collectAsStateWithLifecycle()
    
    RepositoryManagementScreen(
        state = state,
        intentHandler = component::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepositoryManagementScreen(
    state: RepositoryManagementStore.State,
    intentHandler: (RepositoryManagementStore.Intent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        RepositoryManagementHeader()

        Spacer(modifier = Modifier.height(16.dp))
        
        // Messages
        val listState = rememberLazyListState()
        
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.messages) { message ->
                when(message) {
                    is UiRepositoryMessage.UserMessage -> UserMessageBubble(message)
                    is UiRepositoryMessage.AssistantMessage -> AssistantPlainTextMessage(message)
                    is UiRepositoryMessage.ThinkingMessage -> ThinkingMessage(message)
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
        
        // Input section at bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.currentMessage,
                onValueChange = { intentHandler(RepositoryManagementStore.Intent.UpdateMessage(it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about repository management...") },
                enabled = !state.isLoading,
                maxLines = 3
            )
            
            Button(
                onClick = { intentHandler(RepositoryManagementStore.Intent.SendMessage) },
                enabled = state.currentMessage.isNotBlank() && !state.isLoading
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Send")
            }
        }
        
        // Error handling
        state.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MindWeaverTheme.colors.errorSurface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindWeaverTheme.colors.textInvert,
                        modifier = Modifier.weight(1f)
                    )
                    
                    TextButton(
                        onClick = { intentHandler(RepositoryManagementStore.Intent.ClearError) }
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositoryManagementHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Repository Management",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun UserMessageBubble(message: UiRepositoryMessage.UserMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(start = 64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MindWeaverTheme.colors.accent500
            )
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MindWeaverTheme.colors.textInvert,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun AssistantPlainTextMessage(message: UiRepositoryMessage.AssistantMessage) {
    Text(
        text = message.content,
        style = MaterialTheme.typography.bodyMedium,
        color = MindWeaverTheme.colors.textPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun ThinkingMessage(message: UiRepositoryMessage.ThinkingMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MindWeaverTheme.colors.textSecondary
        )
    }
}