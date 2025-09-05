package com.example.mindweaverstudio.components.codeeditor.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class SidebarMenuItem(
    val title: String,
    val icon: ImageVector,
    val items: List<SidebarSubMenuItem> = emptyList()
) {
    FILE(
        title = "File",
        icon = Icons.Default.Folder,
        items = listOf(
            SidebarSubMenuItem("New File", Icons.Default.Add, "new_file"),
            SidebarSubMenuItem("Open File", Icons.Default.FolderOpen, "open_file"),
            SidebarSubMenuItem("Save", Icons.Default.Save, "save"),
            SidebarSubMenuItem("Save As...", Icons.Default.SaveAs, "save_as"),
            SidebarSubMenuItem("Close", Icons.Default.Close, "close")
        )
    ),
    EDIT(
        title = "Edit",
        icon = Icons.Default.Edit,
        items = listOf(
            SidebarSubMenuItem("Cut", Icons.Default.ContentCut, "cut"),
            SidebarSubMenuItem("Copy", Icons.Default.ContentCopy, "copy"),
            SidebarSubMenuItem("Paste", Icons.Default.ContentPaste, "paste"),
            SidebarSubMenuItem("Find", Icons.Default.Search, "find"),
            SidebarSubMenuItem("Replace", Icons.Default.FindReplace, "replace")
        )
    ),
    VIEW(
        title = "View",
        icon = Icons.Default.Visibility,
        items = listOf(
            SidebarSubMenuItem("Zoom In", Icons.Default.ZoomIn, "zoom_in"),
            SidebarSubMenuItem("Zoom Out", Icons.Default.ZoomOut, "zoom_out"),
            SidebarSubMenuItem("Full Screen", Icons.Default.Fullscreen, "full_screen"),
            SidebarSubMenuItem("Toggle Sidebar", Icons.Default.Menu, "toggle_sidebar")
        )
    ),
    TOOLS(
        title = "Tools",
        icon = Icons.Default.Build,
        items = listOf(
            SidebarSubMenuItem("Terminal", Icons.Default.Terminal, "terminal"),
            SidebarSubMenuItem("Git", Icons.Default.Storage, "git"),
            SidebarSubMenuItem("Build", Icons.Default.PlayArrow, "build"),
            SidebarSubMenuItem("Debug", Icons.Default.BugReport, "debug")
        )
    ),
    CONFIGURATION(
        title = "Configuration",
        icon = Icons.Default.Settings,
        items = listOf(
            SidebarSubMenuItem("Preferences", Icons.Default.Tune, "preferences"),
            SidebarSubMenuItem("Plugins", Icons.Default.Extension, "plugins"),
            SidebarSubMenuItem("Themes", Icons.Default.Palette, "themes"),
            SidebarSubMenuItem("Shortcuts", Icons.Default.Keyboard, "shortcuts")
        )
    )
}

data class SidebarSubMenuItem(
    val title: String,
    val icon: ImageVector,
    val action: String
)