package com.example.mindweaverstudio.ui.codeeditor

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mindweaverstudio.components.codeeditor.models.ChatMessage
import com.example.mindweaverstudio.components.codeeditor.CodeEditorComponent
import com.example.mindweaverstudio.components.codeeditor.CodeEditorStore
import com.example.mindweaverstudio.components.codeeditor.models.LogEntry
import com.example.mindweaverstudio.components.codeeditor.models.LogLevel
import com.example.mindweaverstudio.components.codeeditor.models.Panel
import com.example.mindweaverstudio.components.codeeditor.models.FileNode
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CodeEditorScreen(component: CodeEditorComponent) {
    val state by component.state.collectAsStateWithLifecycle()
    
    CodeEditorScreen(
        state = state,
        intentHandler = component::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeEditorScreen(
    state: CodeEditorStore.State,
    intentHandler: (CodeEditorStore.Intent) -> Unit
) {
    val density = LocalDensity.current
    
    // Panel sizes
    var leftPanelWidth by rememberSaveable { mutableFloatStateOf(state.leftPanelWidth) }
    var rightPanelWidth by rememberSaveable { mutableFloatStateOf(state.rightPanelWidth) }
    var bottomPanelHeight by rememberSaveable { mutableFloatStateOf(state.bottomPanelHeight) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top workspace section
        BoxWithConstraints(
            modifier = Modifier.weight(1f - bottomPanelHeight)
        ) {
            val totalWidth = maxWidth
            val leftWidth = (totalWidth * leftPanelWidth).coerceIn(100.dp, totalWidth - 200.dp)
            val rightWidth = (totalWidth * rightPanelWidth).coerceIn(100.dp, totalWidth - leftWidth - 100.dp)
            val centerWidth = totalWidth - leftWidth - rightWidth - 8.dp // 8.dp for dividers
            
            Row(modifier = Modifier.fillMaxSize()) {
                // Left panel - Project Tree
                ProjectTreePanel(
                    modifier = Modifier.width(leftWidth),
                    projectTree = state.projectTree,
                    selectedFile = state.selectedFile,
                    onFileSelected = { intentHandler(CodeEditorStore.Intent.SelectFile(it)) }
                )
                
                // Left divider
                VerticalDivider(
                    onDrag = { delta ->
                        val deltaWidth = with(density) { delta.x.toDp() }
                        val newWidth = ((leftWidth + deltaWidth) / totalWidth).coerceIn(0.1f, 0.8f)
                        leftPanelWidth = newWidth
                        intentHandler(CodeEditorStore.Intent.UpdatePanelWidth(Panel.LEFT, newWidth))
                    }
                )
                
                // Center panel - Editor
                EditorPanel(
                    modifier = Modifier.width(centerWidth),
                    selectedFile = state.selectedFile,
                    content = state.editorContent,
                    onContentChanged = { intentHandler(CodeEditorStore.Intent.UpdateEditorContent(it)) }
                )
                
                // Right divider
                VerticalDivider(
                    onDrag = { delta ->
                        val deltaWidth = with(density) { delta.x.toDp() }
                        val newWidth = ((rightWidth - deltaWidth) / totalWidth).coerceIn(0.1f, 0.8f)
                        rightPanelWidth = newWidth
                        intentHandler(CodeEditorStore.Intent.UpdatePanelWidth(Panel.RIGHT, newWidth))
                    }
                )
                
                // Right panel - Chat
                ChatPanel(
                    modifier = Modifier.width(rightWidth),
                    messages = state.chatMessages,
                    chatInput = state.chatInput,
                    onInputChanged = { intentHandler(CodeEditorStore.Intent.UpdateChatInput(it)) },
                    onSendMessage = { intentHandler(CodeEditorStore.Intent.SendChatMessage) }
                )
            }
        }
        
        // Bottom divider
        HorizontalDivider(
            onDrag = { delta ->
                val deltaRatio = delta.y / 1000f // Simple ratio calculation
                val newHeight = (bottomPanelHeight - deltaRatio).coerceIn(0.1f, 0.7f)
                bottomPanelHeight = newHeight
                intentHandler(CodeEditorStore.Intent.UpdateBottomPanelHeight(newHeight))
            }
        )
        
        // Bottom panel - Logs
        LogsPanel(
            modifier = Modifier.fillMaxWidth().weight(bottomPanelHeight),
            logs = state.logs
        )
    }
}

@Composable
private fun ProjectTreePanel(
    modifier: Modifier = Modifier,
    projectTree: List<FileNode>,
    selectedFile: FileNode?,
    onFileSelected: (FileNode) -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Project Tree",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(projectTree) { node ->
                    FileNodeItem(
                        node = node,
                        isSelected = selectedFile?.path == node.path,
                        onFileSelected = onFileSelected,
                        level = 0,
                        selectedFile = selectedFile
                    )
                }
            }
        }
    }
}

@Composable
private fun FileNodeItem(
    node: FileNode,
    isSelected: Boolean,
    onFileSelected: (FileNode) -> Unit,
    level: Int,
    selectedFile: FileNode?
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 16).dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                )
                .clickable { onFileSelected(node) }
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (node.isDirectory) "📁 ${node.name}" else "📄 ${node.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurface
            )
        }
        
        if (node.isDirectory) {
            node.children.forEach { child ->
                FileNodeItem(
                    node = child,
                    isSelected = selectedFile?.path == child.path,
                    onFileSelected = onFileSelected,
                    level = level + 1,
                    selectedFile = selectedFile
                )
            }
        }
    }
}

@Composable
private fun EditorPanel(
    modifier: Modifier = Modifier,
    selectedFile: FileNode?,
    content: String,
    onContentChanged: (String) -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = selectedFile?.name ?: "No file selected",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            BasicTextField(
                value = content,
                onValueChange = onContentChanged,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun ChatPanel(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>,
    chatInput: String,
    onInputChanged: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Chat",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val listState = rememberLazyListState()
            
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    ChatMessageItem(message = message)
                }
            }
            
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = onInputChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = onSendMessage,
                    enabled = chatInput.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) 
                    MaterialTheme.colorScheme.primaryContainer
                else 
                    MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.isUser) 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else 
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isUser) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else 
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun LogsPanel(
    modifier: Modifier = Modifier,
    logs: List<LogEntry>
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Logs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val listState = rememberLazyListState()
            
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(logs) { log ->
                    LogEntryItem(log = log)
                }
            }
            
            LaunchedEffect(logs.size) {
                if (logs.isNotEmpty()) {
                    listState.animateScrollToItem(logs.size - 1)
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(log: LogEntry) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = timeFormat.format(Date(log.timestamp)),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        
        Text(
            text = when (log.level) {
                LogLevel.INFO -> "ℹ️"
                LogLevel.WARNING -> "⚠️"
                LogLevel.ERROR -> "❌"
                LogLevel.DEBUG -> "🐛"
            },
            modifier = Modifier.width(24.dp)
        )
        
        SelectionContainer {
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = when (log.level) {
                    LogLevel.ERROR -> MaterialTheme.colorScheme.error
                    LogLevel.WARNING -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun VerticalDivider(
    onDrag: (delta: androidx.compose.ui.geometry.Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    onDrag(dragAmount)
                }
            }
    )
}

@Composable
private fun HorizontalDivider(
    onDrag: (delta: androidx.compose.ui.geometry.Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .height(4.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    onDrag(dragAmount)
                }
            }
    )
}