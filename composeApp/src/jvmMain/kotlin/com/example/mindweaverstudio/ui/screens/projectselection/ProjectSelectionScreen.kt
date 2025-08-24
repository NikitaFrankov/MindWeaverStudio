package com.example.mindweaverstudio.ui.screens.projectselection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mindweaverstudio.components.projectselection.ProjectSelectionComponent
import com.example.mindweaverstudio.components.projectselection.ProjectSelectionStore
import com.example.mindweaverstudio.components.projectselection.Project
import com.example.mindweaverstudio.ui.theme.MindWeaverTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectSelectionScreen(component: ProjectSelectionComponent) {
    val state by component.state.collectAsStateWithLifecycle()
    
    ProjectSelectionScreen(
        state = state,
        intentHandler = component::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectSelectionScreen(
    state: ProjectSelectionStore.State,
    intentHandler: (ProjectSelectionStore.Intent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Recent projects",
            style = MaterialTheme.typography.titleLarge,
            color = MindWeaverTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // New Project Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MindWeaverTheme.colors.surface2
            ),
            onClick = { intentHandler(ProjectSelectionStore.Intent.SelectNewProject) },
        ) {
            ListItem(
                headlineContent = { 
                    Text(
                        text = "Open Project",
                        color = MindWeaverTheme.colors.textPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                supportingContent = { 
                    Text(
                        text = "Browse for a project folder",
                        color = MindWeaverTheme.colors.textSecondary,
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Open Project",
                        tint = MindWeaverTheme.colors.accent500
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ListItemDefaults.colors(
                    containerColor = MindWeaverTheme.colors.surface2
                )
            )
        }
        
        // Loading Indicator
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Recent Projects List
        if (state.projects.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.projects) { project ->
                    RecentProjectCard(
                        project = project,
                        onProjectClick = { intentHandler(ProjectSelectionStore.Intent.OpenProject(project)) },
                        onRemoveClick = { intentHandler(ProjectSelectionStore.Intent.RemoveProject(project.path)) }
                    )
                }
            }
        } else if (!state.isLoading) {
            // Empty State
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MindWeaverTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No recent projects",
                        style = MaterialTheme.typography.titleMedium,
                        color = MindWeaverTheme.colors.textSecondary
                    )
                    Text(
                        "Open a project to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MindWeaverTheme.colors.textSecondary
                    )
                }
            }
        }
        
        // Error Display
        state.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MindWeaverTheme.colors.errorSurface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                        onClick = { /* Clear error if needed */ }
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentProjectCard(
    project: Project,
    onProjectClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val formattedDate = remember(project.lastOpened) { 
        dateFormat.format(Date(project.lastOpened)) 
    }
    
    Card(
        onClick = onProjectClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MindWeaverTheme.colors.surface2
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MindWeaverTheme.colors.accent500,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    color = MindWeaverTheme.colors.textPrimary,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = project.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MindWeaverTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "Last opened: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MindWeaverTheme.colors.textSecondary
                )
            }

            IconButton(
                onClick = onRemoveClick
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove from recent projects",
                    tint = MindWeaverTheme.colors.textSecondary
                )
            }
        }
    }
}