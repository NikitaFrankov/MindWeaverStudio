package com.example.mindweaverstudio.ui.components.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.mindweaverstudio.ui.components.toolbar.models.ToolbarAction
import com.example.mindweaverstudio.ui.components.toolbar.models.ToolbarMenu
import com.example.mindweaverstudio.ui.components.toolbar.models.ToolbarMenuItem
import com.example.mindweaverstudio.ui.theme.MindWeaverTheme

@Composable
fun EditorMenuBar(
    modifier: Modifier = Modifier,
    onAction: (ToolbarAction) -> Unit = {}
) {
    var openMenuId by remember { mutableStateOf<String?>(null) }
    
    Surface(
        modifier = modifier,
        color = MindWeaverTheme.colors.surface1,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarMenu.entries.forEach { menu ->
                MenuBarItem(
                    menu = menu,
                    isOpen = openMenuId == menu.name,
                    onMenuToggle = { isOpen ->
                        openMenuId = if (isOpen) menu.name else null
                        onAction(ToolbarAction.MenuToggled(menu.name, isOpen))
                    },
                    onMenuItemClick = { item ->
                        openMenuId = null
                        onAction(ToolbarAction.MenuItemClicked(menu.name, item.id))
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuBarItem(
    menu: ToolbarMenu,
    isOpen: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onMenuItemClick: (ToolbarMenuItem) -> Unit
) {
    Box {
        // Menu button
        Text(
            text = menu.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isOpen) MindWeaverTheme.colors.textPrimary 
                   else MindWeaverTheme.colors.textSecondary,
            fontWeight = if (isOpen) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isOpen) MindWeaverTheme.colors.selection
                    else Color.Transparent
                )
                .clickable { onMenuToggle(!isOpen) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
        
        // Dropdown menu
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { onMenuToggle(false) },
            modifier = Modifier.zIndex(1000f),
            containerColor = MindWeaverTheme.colors.surface2
        ) {
            menu.items.forEach { item ->
                if (item.separator) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MindWeaverTheme.colors.borderNeutral
                    )
                } else {
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth().background(color = MindWeaverTheme.colors.surface2),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    item.icon?.let { icon ->
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(end = 8.dp),
                                            tint = MindWeaverTheme.colors.textSecondary
                                        )
                                    }
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (item.enabled) MindWeaverTheme.colors.textPrimary
                                               else MindWeaverTheme.colors.textSecondary.copy(alpha = 0.5f)
                                    )
                                }
                                
                                item.shortcut?.let { shortcut ->
                                    Text(
                                        text = shortcut,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MindWeaverTheme.colors.textSecondary,
                                        fontWeight = FontWeight.Light
                                    )
                                }
                            }
                        },
                        onClick = { 
                            if (item.enabled) {
                                onMenuItemClick(item)
                            }
                        },
                        enabled = item.enabled,
                        colors = MenuDefaults.itemColors(
                            textColor = if (item.enabled) MindWeaverTheme.colors.textPrimary else MindWeaverTheme.colors.textSecondary.copy(alpha = 0.5f),
                        )
                    )
                }
            }
        }
    }
}