package com.example.mindweaverstudio.ui.chat.utils

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mindweaverstudio.data.model.AppLocale
import com.example.mindweaverstudio.ui.model.PromptModePresentation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptModeSelector(
    selectedModeId: String,
    onModeChange: (String) -> Unit,
    availableModes: List<PromptModePresentation>,
    locale: AppLocale = AppLocale.getDefault(),
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedMode = availableModes.find { it.id == selectedModeId } 
        ?: PromptModePresentation.getAllLocalizedModes(locale).first()

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = ChatStrings.PROMPT_MODE_LABEL,
            style = MaterialTheme.typography.bodySmall
        )
        
        Box {
            FilterChip(
                onClick = { if (enabled) expanded = true },
                enabled = enabled,
                label = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(selectedMode.displayName)
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                selected = false
            )
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableModes.forEach { mode ->
                    DropdownMenuItem(
                        text = { 
                            Column {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onModeChange(mode.id)
                            expanded = false
                        },
                        leadingIcon = if (mode.id == selectedModeId) {
                            { 
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
}