package com.example.mindweaverstudio.ui.components.toolbar.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class ToolbarMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector? = null,
    val shortcut: String? = null,
    val enabled: Boolean = true,
    val separator: Boolean = false
)

enum class ToolbarMenu(
    val title: String,
    val items: List<ToolbarMenuItem>
) {
    FILE(
        title = "File",
        items = listOf(
            ToolbarMenuItem("new_file", "New File", Icons.Default.Add, "Ctrl+N"),
            ToolbarMenuItem("open_file", "Open File", Icons.Default.FolderOpen, "Ctrl+O"),
            ToolbarMenuItem("separator_1", "", separator = true),
            ToolbarMenuItem("save", "Save", Icons.Default.Save, "Ctrl+S"),
            ToolbarMenuItem("save_as", "Save As...", Icons.Default.SaveAs, "Ctrl+Shift+S"),
            ToolbarMenuItem("save_all", "Save All", Icons.Default.Save, "Ctrl+Alt+S"),
            ToolbarMenuItem("separator_2", "", separator = true),
            ToolbarMenuItem("close_file", "Close File", Icons.Default.Close, "Ctrl+W"),
            ToolbarMenuItem("close_all", "Close All", Icons.Default.CloseFullscreen, "Ctrl+Shift+W"),
            ToolbarMenuItem("separator_3", "", separator = true),
            ToolbarMenuItem("recent_files", "Recent Files", Icons.Default.History),
            ToolbarMenuItem("separator_4", "", separator = true),
            ToolbarMenuItem("exit", "Exit", Icons.Default.ExitToApp, "Ctrl+Q")
        )
    ),
    
    EDIT(
        title = "Edit",
        items = listOf(
            ToolbarMenuItem("undo", "Undo", Icons.Default.Undo, "Ctrl+Z"),
            ToolbarMenuItem("redo", "Redo", Icons.Default.Redo, "Ctrl+Y"),
            ToolbarMenuItem("separator_1", "", separator = true),
            ToolbarMenuItem("cut", "Cut", Icons.Default.ContentCut, "Ctrl+X"),
            ToolbarMenuItem("copy", "Copy", Icons.Default.ContentCopy, "Ctrl+C"),
            ToolbarMenuItem("paste", "Paste", Icons.Default.ContentPaste, "Ctrl+V"),
            ToolbarMenuItem("separator_2", "", separator = true),
            ToolbarMenuItem("select_all", "Select All", Icons.Default.SelectAll, "Ctrl+A"),
            ToolbarMenuItem("separator_3", "", separator = true),
            ToolbarMenuItem("find", "Find", Icons.Default.Search, "Ctrl+F"),
            ToolbarMenuItem("replace", "Replace", Icons.Default.FindReplace, "Ctrl+H"),
            ToolbarMenuItem("find_in_files", "Find in Files", Icons.Default.FolderOpen, "Ctrl+Shift+F")
        )
    ),
    
    VIEW(
        title = "View",
        items = listOf(
            ToolbarMenuItem("zoom_in", "Zoom In", Icons.Default.ZoomIn, "Ctrl++"),
            ToolbarMenuItem("zoom_out", "Zoom Out", Icons.Default.ZoomOut, "Ctrl+-"),
            ToolbarMenuItem("reset_zoom", "Reset Zoom", Icons.Default.CenterFocusStrong, "Ctrl+0"),
            ToolbarMenuItem("separator_1", "", separator = true),
            ToolbarMenuItem("fullscreen", "Toggle Full Screen", Icons.Default.Fullscreen, "F11"),
            ToolbarMenuItem("separator_2", "", separator = true),
            ToolbarMenuItem("toggle_project", "Toggle Project Tree", Icons.Default.AccountTree, "Alt+1"),
            ToolbarMenuItem("toggle_chat", "Toggle Chat Panel", Icons.Default.Chat, "Alt+2"),
            ToolbarMenuItem("toggle_logs", "Toggle Logs Panel", Icons.Default.Article, "Alt+3"),
            ToolbarMenuItem("separator_3", "", separator = true),
            ToolbarMenuItem("appearance", "Appearance", Icons.Default.Palette)
        )
    ),
    
    TOOLS(
        title = "Tools",
        items = listOf(
            ToolbarMenuItem("terminal", "Terminal", Icons.Default.Terminal, "Alt+F12"),
            ToolbarMenuItem("separator_1", "", separator = true),
            ToolbarMenuItem("version_control", "Version Control", Icons.Default.Source, "Alt+9"),
            ToolbarMenuItem("separator_2", "", separator = true),
            ToolbarMenuItem("build", "Build Project", Icons.Default.Build, "Ctrl+F9"),
            ToolbarMenuItem("run", "Run", Icons.Default.PlayArrow, "Shift+F10"),
            ToolbarMenuItem("debug", "Debug", Icons.Default.BugReport, "Shift+F9"),
            ToolbarMenuItem("separator_3", "", separator = true),
            ToolbarMenuItem("generate", "Generate Code", Icons.Default.AutoFixHigh, "Alt+Insert"),
            ToolbarMenuItem("database", "Database Tools", Icons.Default.Storage)
        )
    ),
    
    CONFIGURATION(
        title = "Configuration",
        items = listOf(
            ToolbarMenuItem("user_configuration", "User configuration", Icons.Default.Settings, "Ctrl+Alt+S"),
            ToolbarMenuItem("separator_1", "", separator = true),
            ToolbarMenuItem("plugins", "Plugins", Icons.Default.Extension),
            ToolbarMenuItem("appearance_behavior", "Appearance & Behavior", Icons.Default.Tune),
            ToolbarMenuItem("keymap", "Keymap", Icons.Default.Keyboard),
            ToolbarMenuItem("separator_2", "", separator = true),
            ToolbarMenuItem("import_settings", "Import Settings", Icons.Default.GetApp),
            ToolbarMenuItem("export_settings", "Export Settings", Icons.Default.Publish),
            ToolbarMenuItem("separator_3", "", separator = true),
            ToolbarMenuItem("about", "About", Icons.Default.Info)
        )
    )
}

sealed class ToolbarAction {
    data class MenuItemClicked(val menuId: String, val itemId: String) : ToolbarAction()
    data class MenuToggled(val menuId: String, val isOpen: Boolean) : ToolbarAction()
}