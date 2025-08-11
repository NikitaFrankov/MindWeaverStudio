package com.example.mindweaverstudio.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun ProviderSelector(
    selectedProvider: String,
    onProviderChange: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = ChatStrings.PROVIDER_LABEL,
            style = MaterialTheme.typography.bodySmall
        )
        
        ProviderChip(
            name = ChatStrings.PROVIDER_DEEPSEEK,
            selected = selectedProvider == ChatStrings.PROVIDER_DEEPSEEK,
            onSelect = { onProviderChange(ChatStrings.PROVIDER_DEEPSEEK) }
        )
        
        ProviderChip(
            name = ChatStrings.PROVIDER_CHATGPT,
            selected = selectedProvider == ChatStrings.PROVIDER_CHATGPT,
            onSelect = { onProviderChange(ChatStrings.PROVIDER_CHATGPT) }
        )
        
        ProviderChip(
            name = ChatStrings.PROVIDER_GEMINI,
            selected = selectedProvider == ChatStrings.PROVIDER_GEMINI,
            onSelect = { onProviderChange(ChatStrings.PROVIDER_GEMINI) }
        )
    }
}

@Composable
private fun ProviderChip(
    name: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    FilterChip(
        onClick = onSelect,
        label = { Text(name) },
        selected = selected
    )
}