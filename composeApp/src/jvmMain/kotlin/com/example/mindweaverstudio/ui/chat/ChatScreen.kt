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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mindweaverstudio.components.chat.ChatComponent
import com.example.mindweaverstudio.components.chat.ChatStore
import com.example.mindweaverstudio.data.model.AppLocale
import com.example.mindweaverstudio.ui.chat.utils.ChatStrings
import com.example.mindweaverstudio.ui.chat.utils.PromptModeSelector
import com.example.mindweaverstudio.ui.model.UiChatMessage
import com.example.mindweaverstudio.ui.model.AssistantMessagePresentation
import com.example.mindweaverstudio.ui.model.PromptModePresentation

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
        ChatHeader(
            selectedProvider = state.selectedProvider,
            selectedPromptMode = state.selectedPromptMode,
            currentMessage = state.currentMessage,
            isInRequirementsGathering = state.isInRequirementsGathering,
            onProviderChange = { provider ->
                intentHandler(ChatStore.Intent.ChangeProvider(provider))
            },
            onPromptModeChange = { promptMode ->
                intentHandler(ChatStore.Intent.ChangePromptMode(promptMode))
            },
            onClearChat = { intentHandler(ChatStore.Intent.ClearChat) },
            onRequestMcp = { intentHandler(ChatStore.Intent.RequestMcp) }
        )
        
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
                    is UiChatMessage.PlainTextMessage -> PlainTextMessage(message)
                    is UiChatMessage.RequirementsSummaryMessage -> RequirementsSummaryMessage(message)
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
                placeholder = { Text(ChatStrings.MESSAGE_PLACEHOLDER) },
                enabled = !state.isLoading,
                maxLines = 3
            )
            
            Button(
                onClick = { intentHandler(ChatStore.Intent.SendMessage) },
                enabled = state.currentMessage.isNotBlank() && !state.isLoading
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = ChatStrings.SEND_DESCRIPTION)
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
                        Text(ChatStrings.DISMISS_ERROR)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatHeader(
    selectedProvider: String,
    selectedPromptMode: String,
    currentMessage: String,
    isInRequirementsGathering: Boolean,
    onProviderChange: (String) -> Unit,
    onPromptModeChange: (String) -> Unit,
    onClearChat: () -> Unit,
    onRequestMcp: () -> Unit
) {
    val locale = remember(currentMessage) { 
        if (currentMessage.isNotEmpty()) AppLocale.detectFromText(currentMessage) else AppLocale.getDefault() 
    }
    val availableModes = remember(locale) { 
        PromptModePresentation.getAllLocalizedModes(locale) 
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = ChatStrings.HEADER_TITLE,
                style = MaterialTheme.typography.headlineMedium
            )
            
            ProviderSelector(
                selectedProvider = selectedProvider,
                onProviderChange = onProviderChange
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            PromptModeSelector(
                selectedModeId = selectedPromptMode,
                onModeChange = onPromptModeChange,
                availableModes = availableModes,
                locale = locale,
                enabled = !isInRequirementsGathering
            )
        }

        Button(
            onClick = onRequestMcp
        ) {
            Text("Get data from MCP")
        }
        
        IconButton(onClick = onClearChat) {
            Icon(
                Icons.Default.Clear,
                contentDescription = ChatStrings.CLEAR_CHAT_DESCRIPTION
            )
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
    val presentation = AssistantMessagePresentation.from(message.structuredOutput)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AnswerSection(presentation.answerText)
            StepsSection(presentation.steps)
            FactsSection(presentation.factAndPoints)
            SummarySection(presentation.summaryText)
            MetaSection(presentation.confidence, presentation.source)
        }
    }
}

@Composable
fun PlainTextMessage(message: UiChatMessage.PlainTextMessage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = message.presentableContent,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun RequirementsSummaryMessage(message: UiChatMessage.RequirementsSummaryMessage) {
    val summary = message.summary
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Requirements Summary",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Project: ${summary.projectName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Text(
                text = "Category: ${summary.category}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = summary.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Confidence: ${(summary.meta.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}