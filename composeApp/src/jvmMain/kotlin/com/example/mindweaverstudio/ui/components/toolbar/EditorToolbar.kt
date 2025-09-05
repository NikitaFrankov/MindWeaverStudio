package com.example.mindweaverstudio.ui.components.toolbar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.mindweaverstudio.ui.theme.MindWeaverTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorToolbar(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit,
    projectName: String = "MindWeaver Studio"
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MindWeaverTheme.colors.textPrimary
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open menu",
                    tint = MindWeaverTheme.colors.textPrimary
                )
            }
        },
        actions = {
            // Quick access toolbar buttons
            ToolbarButton(
                icon = Icons.Default.Save,
                contentDescription = "Save",
                onClick = { /* TODO: Handle save */ }
            )
            
            ToolbarButton(
                icon = Icons.Default.PlayArrow,
                contentDescription = "Run",
                onClick = { /* TODO: Handle run */ }
            )
            
            ToolbarButton(
                icon = Icons.Default.BugReport,
                contentDescription = "Debug",
                onClick = { /* TODO: Handle debug */ }
            )
            
            ToolbarButton(
                icon = Icons.Default.Search,
                contentDescription = "Search",
                onClick = { /* TODO: Handle search */ }
            )
            
            ToolbarButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                onClick = { /* TODO: Handle settings */ }
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MindWeaverTheme.colors.surface1,
            titleContentColor = MindWeaverTheme.colors.textPrimary,
            navigationIconContentColor = MindWeaverTheme.colors.textPrimary,
            actionIconContentColor = MindWeaverTheme.colors.textPrimary
        )
    )
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) MindWeaverTheme.colors.textSecondary else MindWeaverTheme.colors.textSecondary.copy(alpha = 0.5f)
        )
    }
}