package com.example.mindweaverstudio.ui.screens.userconfiguration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mindweaverstudio.components.userconfiguration.UserConfigurationComponent
import com.example.mindweaverstudio.components.userconfiguration.UserConfigurationStore
import com.example.mindweaverstudio.data.models.profile.WorkRole
import com.example.mindweaverstudio.data.models.profile.ResponseFormat
import com.example.mindweaverstudio.data.models.profile.ExperienceLevel
import com.example.mindweaverstudio.ui.theme.MindWeaverTheme

@Composable
fun UserConfigurationScreen(component: UserConfigurationComponent) {
    val state by component.state.collectAsStateWithLifecycle()

    UserConfigurationScreen(
        state = state,
        intentHandler = component::onIntent,
        onBackPressed = component::onBackPressed
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserConfigurationScreen(
    state: UserConfigurationStore.State,
    intentHandler: (UserConfigurationStore.Intent) -> Unit,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MindWeaverTheme.colors.surface1)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "User Configuration",
                    style = MaterialTheme.typography.titleLarge,
                    color = MindWeaverTheme.colors.textPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MindWeaverTheme.colors.textPrimary
                    )
                }
            },
            actions = {
                // Save button
                Button(
                    onClick = { intentHandler(UserConfigurationStore.Intent.SaveConfiguration) },
                    enabled = !state.isSaving && state.hasUnsavedChanges,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MindWeaverTheme.colors.accent500,
                        disabledContainerColor = MindWeaverTheme.colors.accent500.copy(alpha = 0.5f)
                    )
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MindWeaverTheme.colors.textPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Save")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Reset button
                TextButton(
                    onClick = { intentHandler(UserConfigurationStore.Intent.ResetConfiguration) },
                    enabled = !state.isSaving
                ) {
                    Text(
                        "Reset to Defaults",
                        color = MindWeaverTheme.colors.textSecondary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MindWeaverTheme.colors.surface1,
                titleContentColor = MindWeaverTheme.colors.textPrimary
            )
        )

        // Content
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MindWeaverTheme.colors.accent500
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Error message
                state.error?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MindWeaverTheme.colors.error.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MindWeaverTheme.colors.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = MindWeaverTheme.colors.error,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { intentHandler(UserConfigurationStore.Intent.ClearError) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear error",
                                    tint = MindWeaverTheme.colors.error
                                )
                            }
                        }
                    }
                }

                // Personal Information Section
                ConfigurationSection(title = "Personal Information") {
                    // Name field
                    OutlinedTextField(
                        value = state.userPersonalization.name,
                        onValueChange = { intentHandler(UserConfigurationStore.Intent.UpdateName(it)) },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MindWeaverTheme.colors.textPrimary,
                            unfocusedTextColor = MindWeaverTheme.colors.textPrimary,
                            focusedLabelColor = MindWeaverTheme.colors.accent500,
                            unfocusedLabelColor = MindWeaverTheme.colors.textSecondary
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Role selection
                    DropdownField(
                        label = "Work Role",
                        value = state.userPersonalization.role.displayName,
                        options = WorkRole.entries.map { it.displayName },
                        onSelectionChanged = { selectedDisplayName ->
                            val role = WorkRole.entries.find { it.displayName == selectedDisplayName }
                            role?.let { intentHandler(UserConfigurationStore.Intent.UpdateRole(it)) }
                        }
                    )
                }

                // Professional Settings Section
                ConfigurationSection(title = "Professional Settings") {
                    // Preferred Programming Language
                    OutlinedTextField(
                        value = state.userPersonalization.preferredLanguage,
                        onValueChange = { intentHandler(UserConfigurationStore.Intent.UpdatePreferredLanguage(it)) },
                        label = { Text("Preferred Programming Language") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MindWeaverTheme.colors.textPrimary,
                            unfocusedTextColor = MindWeaverTheme.colors.textPrimary,
                            focusedLabelColor = MindWeaverTheme.colors.accent500,
                            unfocusedLabelColor = MindWeaverTheme.colors.textSecondary
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Experience Level
                    DropdownField(
                        label = "Experience Level",
                        value = state.userPersonalization.experienceLevel.displayName,
                        options = ExperienceLevel.entries.map { it.displayName },
                        onSelectionChanged = { selectedDisplayName ->
                            val level = ExperienceLevel.entries.find { it.displayName == selectedDisplayName }
                            level?.let { intentHandler(UserConfigurationStore.Intent.UpdateExperienceLevel(it)) }
                        }
                    )
                }

                // AI Preferences Section
                ConfigurationSection(title = "AI Assistant Preferences") {
                    // Response Format
                    DropdownField(
                        label = "Preferred Response Format",
                        value = state.userPersonalization.responseFormat.displayName,
                        options = ResponseFormat.entries.map { it.displayName },
                        onSelectionChanged = { selectedDisplayName ->
                            val format = ResponseFormat.entries.find { it.displayName == selectedDisplayName }
                            format?.let { intentHandler(UserConfigurationStore.Intent.UpdateResponseFormat(it)) }
                        }
                    )
                }

                // Other Settings Section
                ConfigurationSection(title = "Other Settings") {
                    // Time Zone
                    OutlinedTextField(
                        value = state.userPersonalization.timeZone,
                        onValueChange = { intentHandler(UserConfigurationStore.Intent.UpdateTimeZone(it)) },
                        label = { Text("Time Zone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MindWeaverTheme.colors.textPrimary,
                            unfocusedTextColor = MindWeaverTheme.colors.textPrimary,
                            focusedLabelColor = MindWeaverTheme.colors.accent500,
                            unfocusedLabelColor = MindWeaverTheme.colors.textSecondary
                        )
                    )
                }

                // Unsaved changes indicator
                if (state.hasUnsavedChanges) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MindWeaverTheme.colors.warning.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MindWeaverTheme.colors.warning
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "You have unsaved changes",
                                color = MindWeaverTheme.colors.warning
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigurationSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MindWeaverTheme.colors.surface2
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MindWeaverTheme.colors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelectionChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MindWeaverTheme.colors.textPrimary,
                unfocusedTextColor = MindWeaverTheme.colors.textPrimary,
                focusedLabelColor = MindWeaverTheme.colors.accent500,
                unfocusedLabelColor = MindWeaverTheme.colors.textSecondary
            )
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MindWeaverTheme.colors.surface2
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            option,
                            color = MindWeaverTheme.colors.textPrimary
                        )
                    },
                    onClick = {
                        onSelectionChanged(option)
                        expanded = false
                    }
                )
            }
        }
    }
}