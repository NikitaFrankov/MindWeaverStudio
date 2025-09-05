package com.example.mindweaverstudio.ui.screens.sidebar

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mindweaverstudio.components.codeeditor.models.SidebarMenuItem
import com.example.mindweaverstudio.components.sidebar.SidebarComponent
import com.example.mindweaverstudio.components.sidebar.SidebarStore
import com.example.mindweaverstudio.ui.theme.MindWeaverTheme

@Composable
fun SidebarScreen(component: SidebarComponent) {
    val state by component.state.collectAsStateWithLifecycle()

    SidebarScreen(
        state = state,
        intentHandler = component::onIntent
    )
}

@Composable
private fun SidebarScreen(
    state: SidebarStore.State,
    intentHandler: (SidebarStore.Intent) -> Unit
) {
    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInHorizontally() + fadeIn(),
        exit = slideOutHorizontally() + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MindWeaverTheme.colors.surface1
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                // Header with close button
                SidebarHeader(
                    onClose = { intentHandler(SidebarStore.Intent.CloseSidebar) }
                )
                
                Divider(color = MindWeaverTheme.colors.borderNeutral)
                
                // Menu items
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(SidebarMenuItem.entries) { menuItem ->
                        SidebarMenuItemRow(
                            menuItem = menuItem,
                            isExpanded = state.expandedMenuItems.contains(menuItem),
                            isSelected = state.selectedMenuItem == menuItem,
                            onMenuItemClick = { intentHandler(SidebarStore.Intent.SelectMenuItem(menuItem)) },
                            onToggleExpansion = { intentHandler(SidebarStore.Intent.ToggleMenuExpansion(menuItem)) },
                            onSubMenuClick = { action -> 
                                intentHandler(SidebarStore.Intent.ExecuteSubMenuAction(action, menuItem))
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarHeader(
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Menu",
                style = MaterialTheme.typography.headlineSmall,
                color = MindWeaverTheme.colors.textPrimary
            )
        },
        actions = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close sidebar",
                    tint = MindWeaverTheme.colors.textSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MindWeaverTheme.colors.surface1,
            titleContentColor = MindWeaverTheme.colors.textPrimary
        )
    )
}

@Composable
private fun SidebarMenuItemRow(
    menuItem: SidebarMenuItem,
    isExpanded: Boolean,
    isSelected: Boolean,
    onMenuItemClick: () -> Unit,
    onToggleExpansion: () -> Unit,
    onSubMenuClick: (String) -> Unit
) {
    Column {
        // Main menu item
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (isSelected) MindWeaverTheme.colors.selection
                    else MindWeaverTheme.colors.surface1
                )
                .clickable {
                    onMenuItemClick()
                    if (menuItem.items.isNotEmpty()) {
                        onToggleExpansion()
                    }
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = menuItem.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MindWeaverTheme.colors.accent500
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = menuItem.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MindWeaverTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            
            // Expansion arrow for items with subitems
            if (menuItem.items.isNotEmpty()) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(16.dp),
                    tint = MindWeaverTheme.colors.textSecondary
                )
            }
        }
        
        // Sub menu items
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(start = 32.dp)
            ) {
                menuItem.items.forEach { subMenuItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onSubMenuClick(subMenuItem.action) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = subMenuItem.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MindWeaverTheme.colors.textSecondary
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = subMenuItem.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MindWeaverTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}