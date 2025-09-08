package com.example.mindweaverstudio.ui.screens.codeeditor

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.SpeakerNotes
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mindweaverstudio.components.codeeditor.models.UiChatMessage
import com.example.mindweaverstudio.components.codeeditor.CodeEditorComponent
import com.example.mindweaverstudio.components.codeeditor.CodeEditorStore
import com.example.mindweaverstudio.components.codeeditor.models.LogEntry
import com.example.mindweaverstudio.components.codeeditor.models.UiLogLevel
import com.example.mindweaverstudio.components.codeeditor.models.UiPanel
import com.example.mindweaverstudio.components.codeeditor.models.FileNode
import com.example.mindweaverstudio.ui.theme.MindWeaverTheme
import com.example.mindweaverstudio.ui.components.toolbar.EditorMenuBar
import com.example.mindweaverstudio.ui.components.toolbar.MenuActionHandler
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CodeEditorScreen(component: CodeEditorComponent) {
    val state by component.state.collectAsStateWithLifecycle()

    CodeEditorScreen(
        state = state,
        intentHandler = component::onIntent,
        onNavigateToUserConfiguration = component::onNavigateToUserConfiguration
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeEditorScreen(
    state: CodeEditorStore.State,
    intentHandler: (CodeEditorStore.Intent) -> Unit,
    onNavigateToUserConfiguration: () -> Unit = {}
) {
    val density = LocalDensity.current
    
    // Create menu action handler
    val menuActionHandler = remember {
        MenuActionHandler(
            onEditorIntent = intentHandler,
            onNavigateToUserConfiguration = onNavigateToUserConfiguration
        )
    }

    var leftPanelWidth by remember { mutableStateOf(250.dp) }
    var rightPanelWidth by remember { mutableStateOf(250.dp) }
    var bottomPanelHeight by remember { mutableStateOf(100.dp) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar at the top
        EditorMenuBar(
            onAction = menuActionHandler::handleAction
        )
        
        // Main editor content below
        BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxWidthDp = with(density) { constraints.maxWidth.toDp() }

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
                .background(MindWeaverTheme.colors.borderNeutral)
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
                .offset { IntOffset((leftPanelWidth + 6.dp).roundToPx(), 0) }  // Сдвиг слева
                .width(maxWidthDp - leftPanelWidth - rightPanelWidth - 12.dp)  // Рассчитанная ширина (full - left - right - borders)
                .fillMaxHeight()
                .padding(bottom = bottomPanelHeight),  // Чтобы не перекрывать logs
            selectedFile = state.selectedFile,
            content = state.editorContent,
            onContentChanged = { intentHandler(CodeEditorStore.Intent.UpdateEditorContent(it)) },
        )

        // draggable граница чата (ставим левее ChatPanel)
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .offset { IntOffset(-rightPanelWidth.roundToPx(), 0) }
                .width(6.dp)
                .fillMaxHeight()
                .background(MindWeaverTheme.colors.borderNeutral)
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
            isVoiceRecording = state.isVoiceRecording,
            error = state.error,
            intentHandler = intentHandler,
        )

        // draggable граница логов (над LogsPanel)
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, -bottomPanelHeight.roundToPx()) }
                .fillMaxWidth()
                .height(6.dp)
                .background(MindWeaverTheme.colors.borderNeutral)
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
            containerColor = MindWeaverTheme.colors.surface1
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Project",
                style = MaterialTheme.typography.titleMedium,
                color = MindWeaverTheme.colors.textSecondary,
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
                    if (isSelected) MindWeaverTheme.colors.selection
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
                    tint = MindWeaverTheme.colors.textSecondary
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
                    node.isDirectory -> MindWeaverTheme.colors.accent500
                    else -> MindWeaverTheme.colors.textSecondary
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // File/folder name
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MindWeaverTheme.colors.textPrimary
                       else MindWeaverTheme.colors.textPrimary
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
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MindWeaverTheme.colors.surface1
        )
    ) {

        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = selectedFile?.name ?: "No file selected",
                style = MaterialTheme.typography.titleMedium,
                color = MindWeaverTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            CodeEditor(
                initialText = content,
                onTextChange = onContentChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
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
    isVoiceRecording: Boolean,
    intentHandler: (CodeEditorStore.Intent) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MindWeaverTheme.colors.surface1
        )
    ) {
        Column {
            Text(
                text = "Assistant",
                style = MaterialTheme.typography.titleMedium,
                color = MindWeaverTheme.colors.textSecondary,
                modifier = Modifier.padding(all = 8.dp)
            )

            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    when (message) {
                        is UiChatMessage.UserMessage -> UserChatMessageItem(message = message)
                        is UiChatMessage.AssistantMessage -> AssistantChatMessageItem(
                            message = message,
                            intentHandler = intentHandler
                        )
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
                modifier = Modifier.fillMaxWidth().background(color = MindWeaverTheme.colors.surface2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = chatInput,
                    onValueChange = { intentHandler(CodeEditorStore.Intent.UpdateChatInput(it)) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors()
                        .copy(
                            focusedTextColor = MindWeaverTheme.colors.textPrimary,
                            unfocusedTextColor = MindWeaverTheme.colors.textSecondary,
                            focusedContainerColor = MindWeaverTheme.colors.surface2,
                            unfocusedContainerColor = MindWeaverTheme.colors.surface2,
                            focusedIndicatorColor = MindWeaverTheme.colors.surface2,
                            errorIndicatorColor = MindWeaverTheme.colors.surface2,
                            disabledIndicatorColor = MindWeaverTheme.colors.surface2,
                            unfocusedIndicatorColor = MindWeaverTheme.colors.surface2,
                            disabledContainerColor = MindWeaverTheme.colors.surface2,
                            errorContainerColor = MindWeaverTheme.colors.surface2,
                        ),
                    placeholder = {
                        Text(
                            text = "Type a message...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MindWeaverTheme.colors.textSecondary
                        )
                    },
                    enabled = !isLoading && !isVoiceRecording,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { intentHandler(CodeEditorStore.Intent.RecordVoiceClick) }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.SpeakerNotes,
                        contentDescription = "Recording Voice",
                        tint = MindWeaverTheme.colors.accent400
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { intentHandler(CodeEditorStore.Intent.SendChatMessage) },
                    enabled = chatInput.isNotBlank() && !isLoading,
                    colors = IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send",
                        tint = MindWeaverTheme.colors.accent400
                    )
                }
            }
        }

        // Error handling
        error?.let { errorMessage ->
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
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindWeaverTheme.colors.textInvert,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = { intentHandler(CodeEditorStore.Intent.ClearError) }
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
                containerColor = MindWeaverTheme.colors.surface2
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
                        color = MindWeaverTheme.colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MindWeaverTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun AssistantChatMessageItem(
    message: UiChatMessage.AssistantMessage,
    intentHandler: (CodeEditorStore.Intent) -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MindWeaverTheme.colors.surface2
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindWeaverTheme.colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                IconButton(
                    onClick = { intentHandler(CodeEditorStore.Intent.PlayMessage(message.content)) },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Recording Voice",
                        tint = MindWeaverTheme.colors.accent400
                    )
                }

                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MindWeaverTheme.colors.textSecondary
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
            color = MindWeaverTheme.colors.textSecondary
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
            containerColor = MindWeaverTheme.colors.surface1
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Logs",
                style = MaterialTheme.typography.titleMedium,
                color = MindWeaverTheme.colors.textSecondary,
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = timeFormat.format(Date(log.timestamp)),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = MindWeaverTheme.colors.textSecondary,
            modifier = Modifier.width(60.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = when (log.level) {
                UiLogLevel.INFO -> "ℹ️"
                UiLogLevel.WARNING -> "⚠️"
                UiLogLevel.ERROR -> "❌"
                UiLogLevel.DEBUG -> "🐛"
            },
            modifier = Modifier.width(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))

        SelectionContainer {
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = when (log.level) {
                    UiLogLevel.ERROR -> MindWeaverTheme.colors.error
                    UiLogLevel.WARNING -> MindWeaverTheme.colors.warning
                    else -> MindWeaverTheme.colors.textPrimary
                }
            )
        }
    }
}