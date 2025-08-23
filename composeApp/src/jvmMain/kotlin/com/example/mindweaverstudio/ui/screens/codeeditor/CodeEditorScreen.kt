package com.example.mindweaverstudio.ui.screens.codeeditor

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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mindweaverstudio.components.codeeditor.models.UiChatMessage
import com.example.mindweaverstudio.components.codeeditor.CodeEditorComponent
import com.example.mindweaverstudio.components.codeeditor.CodeEditorStore
import com.example.mindweaverstudio.components.codeeditor.models.LogEntry
import com.example.mindweaverstudio.components.codeeditor.models.UiLogLevel
import com.example.mindweaverstudio.components.codeeditor.models.UiPanel
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

    var leftPanelWidth by remember { mutableStateOf(240.dp) }
    var rightPanelWidth by remember { mutableStateOf(300.dp) }
    var bottomPanelHeight by remember { mutableStateOf(200.dp) }

    Box(Modifier.fillMaxSize()) {

        // ==== Слой 1: Project Tree ====
        ProjectTreePanel(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 0.dp),
            projectTree = state.projectTree,
            selectedFile = state.selectedFile,
            onFileSelected = { intentHandler(CodeEditorStore.Intent.SelectFile(it)) },
            onFolderToggle = { folderPath -> intentHandler(CodeEditorStore.Intent.ToggleFolderExpanded(folderPath)) }
        )

        // draggable граница дерева
        Box(
            Modifier
                .offset { IntOffset(leftPanelWidth.roundToPx(), 0) }
                .width(6.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        val dx = with(density) { drag.x.toDp() }
                        leftPanelWidth = (leftPanelWidth + dx).coerceIn(120.dp, 600.dp)
                        intentHandler(CodeEditorStore.Intent.UpdatePanelWidth(UiPanel.LEFT, leftPanelWidth.value))
                    }
                }
        )

        // ==== Слой 2: Editor ====
        EditorPanel(
            modifier = Modifier
                .matchParentSize()
                .padding(start = leftPanelWidth + 6.dp, bottom = bottomPanelHeight),
            selectedFile = state.selectedFile,
            content = state.editorContent,
            onContentChanged = { intentHandler(CodeEditorStore.Intent.UpdateEditorContent(it)) },
            onTestCreateClick = { intentHandler(CodeEditorStore.Intent.OnCreateTestClick) }
        )

        // draggable граница чата (ставим левее ChatPanel)
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .offset { IntOffset(-rightPanelWidth.roundToPx(), 0) }
                .width(6.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        val dx = with(density) { drag.x.toDp() }
                        rightPanelWidth = (rightPanelWidth - dx).coerceIn(200.dp, 800.dp)
                        intentHandler(CodeEditorStore.Intent.UpdatePanelWidth(UiPanel.RIGHT, rightPanelWidth.value))
                    }
                }
        )

        // ==== Слой 3: Chat ====
        ChatPanel(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(rightPanelWidth)
                .padding(bottom = bottomPanelHeight),
            messages = state.chatMessages,
            chatInput = state.chatInput,
            isLoading = state.isLoading,
            error = state.error,
            onInputChanged = { intentHandler(CodeEditorStore.Intent.UpdateChatInput(it)) },
            onSendMessage = { intentHandler(CodeEditorStore.Intent.SendChatMessage) },
            onClearError = { intentHandler(CodeEditorStore.Intent.ClearError) }
        )

        // draggable граница логов (над LogsPanel)
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, -bottomPanelHeight.roundToPx()) }
                .fillMaxWidth()
                .height(6.dp)
                .background(MaterialTheme.colorScheme.outline)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        val dy = with(density) { drag.y.toDp() }
                        bottomPanelHeight = (bottomPanelHeight - dy).coerceIn(120.dp, 600.dp)
                        intentHandler(CodeEditorStore.Intent.UpdateBottomPanelHeight(bottomPanelHeight.value / 1000f))
                    }
                }
        )

        // ==== Слой 4: Logs ====
        LogsPanel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomPanelHeight),
            logs = state.logs
        )
    }
}

@Composable
private fun ProjectTreePanel(
    modifier: Modifier = Modifier,
    projectTree: List<FileNode>,
    selectedFile: FileNode?,
    onFileSelected: (FileNode) -> Unit,
    onFolderToggle: (String) -> Unit
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
                        onFolderToggle = onFolderToggle,
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
    onFolderToggle: (String) -> Unit,
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
                .clickable {
                    if (node.isDirectory) {
                        onFolderToggle(node.path)
                    } else {
                        onFileSelected(node)
                    }
                }
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand/collapse icon for directories
            if (node.isDirectory) {
                Icon(
                    imageVector = if (node.expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = if (node.expanded) "Collapse folder" else "Expand folder",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                // Spacer for files to align with folder content
                Spacer(modifier = Modifier.width(20.dp))
            }

            // File/folder icon
            Icon(
                imageVector = when {
                    node.isDirectory && node.expanded -> Icons.Default.FolderOpen
                    node.isDirectory -> Icons.Default.Folder
                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when {
                    node.isDirectory -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // File/folder name
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurface
            )
        }

        // Render children only if directory is expanded
        if (node.isDirectory && node.expanded && node.children.isNotEmpty()) {
            node.children.forEach { child ->
                FileNodeItem(
                    node = child,
                    isSelected = selectedFile?.path == child.path,
                    onFileSelected = onFileSelected,
                    onFolderToggle = onFolderToggle,
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
    onContentChanged: (String) -> Unit,
    onTestCreateClick: () -> Unit,
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
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = selectedFile?.name ?: "No file selected",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterStart)
                )

                Button(
                    onClick = onTestCreateClick,
                    enabled = selectedFile != null,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(text = "Create tests for this code")
                }
            }

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
    messages: List<UiChatMessage>,
    chatInput: String,
    isLoading: Boolean,
    error: String?,
    onInputChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onClearError: () -> Unit
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
                    when (message) {
                        is UiChatMessage.UserMessage -> UserChatMessageItem(message = message)
                        is UiChatMessage.AssistantMessage -> AssistantChatMessageItem(message = message)
                        is UiChatMessage.ThinkingMessage -> ThinkingChatMessageItem(message = message)
                    }
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
                    enabled = !isLoading,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSendMessage,
                    enabled = chatInput.isNotBlank() && !isLoading
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }

        // Error handling
        error?.let { errorMessage ->
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
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = onClearError
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
private fun UserChatMessageItem(message: UiChatMessage.UserMessage) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AssistantChatMessageItem(message: UiChatMessage.AssistantMessage) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ThinkingChatMessageItem(message: UiChatMessage.ThinkingMessage) {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                UiLogLevel.INFO -> "ℹ️"
                UiLogLevel.WARNING -> "⚠️"
                UiLogLevel.ERROR -> "❌"
                UiLogLevel.DEBUG -> "🐛"
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
                    UiLogLevel.ERROR -> MaterialTheme.colorScheme.error
                    UiLogLevel.WARNING -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun VerticalDivider(
    onDrag: (delta: Offset) -> Unit
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
    onDrag: (delta: Offset) -> Unit
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