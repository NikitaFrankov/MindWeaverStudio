package com.example.mindweaverstudio.ui.screens.repoInfoInput

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mindweaverstudio.components.repoInfoInput.RepoInfoInputComponent
import com.example.mindweaverstudio.components.repoInfoInput.RepoInfoInputStore
import com.example.mindweaverstudio.components.repoInfoInput.RepoInfoInputStore.Intent.*
import com.example.mindweaverstudio.ui.theme.MindWeaverTheme

@Composable
fun RepoInfoInputDialog(component: RepoInfoInputComponent) {
    val state by component.state.collectAsStateWithLifecycle()

    RepoInfoInputDialog(
        state = state,
        intentHandler = component::onIntent
    )
}

@Composable
private fun RepoInfoInputDialog(
    state: RepoInfoInputStore.State,
    intentHandler: (RepoInfoInputStore.Intent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(color = Color.Transparent)) {
        AlertDialog(
            onDismissRequest = { intentHandler.invoke(OnConfirmChanges) },
            title = {
                Text(
                    text = "Input current repository data",
                    color = MindWeaverTheme.colors.textPrimary
                )
            },
            text = {
                Column {
                    Spacer(modifier = Modifier.size(16.dp))
                    TextField(
                        value = state.repoName,
                        onValueChange = { intentHandler(OnRepoNameChange(it)) },
                        modifier = Modifier.fillMaxWidth(),
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
                        label = {
                            Text(
                                text = "Repository name",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MindWeaverTheme.colors.textSecondary
                            )
                        },
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    TextField(
                        value = state.repoOwner,
                        onValueChange = { intentHandler(OnRepoOwnerChange(it)) },
                        modifier = Modifier.fillMaxWidth(),
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
                        label = {
                            Text(
                                text = "Repository owner",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MindWeaverTheme.colors.textSecondary
                            )
                        },
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MindWeaverTheme.colors.accent500
                    ),
                    onClick = { intentHandler.invoke(OnConfirmChanges) }
                ) {
                    Text(
                        text = "OK",
                        color = MindWeaverTheme.colors.textPrimary,
                    )
                }
            },
            backgroundColor = MindWeaverTheme.colors.surface1
        )
    }
}