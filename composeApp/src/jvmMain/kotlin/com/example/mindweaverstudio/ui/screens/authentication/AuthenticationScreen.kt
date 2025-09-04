package com.example.mindweaverstudio.ui.screens.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mindweaverstudio.components.authentication.AuthenticationComponent
import com.example.mindweaverstudio.components.authentication.AuthenticationStore
import com.example.mindweaverstudio.ui.theme.MindWeaverTheme

@Composable
fun AuthenticationScreen(component: AuthenticationComponent) {
    val state by component.state.collectAsStateWithLifecycle()
    
    AuthenticationScreen(
        state = state,
        intentHandler = component::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticationScreen(
    state: AuthenticationStore.State,
    intentHandler: (AuthenticationStore.Intent) -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = "MindWeaver Studio",
            style = MaterialTheme.typography.headlineLarge,
            color = MindWeaverTheme.colors.accent500,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Neural Network Chat Interface",
            style = MaterialTheme.typography.titleMedium,
            color = MindWeaverTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Authentication Form
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp),
            colors = CardDefaults.cardColors(
                containerColor = MindWeaverTheme.colors.surface2
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MindWeaverTheme.colors.textPrimary,
                    modifier = Modifier.padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )

                // Email Field
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { email ->
                        intentHandler(AuthenticationStore.Intent.UpdateEmail(email))
                    },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Email"
                        )
                    },
                    isError = !state.isEmailValid && state.email.isNotBlank(),
                    supportingText = {
                        if (!state.isEmailValid && state.email.isNotBlank()) {
                            Text(
                                text = "Please enter a valid email address",
                                color = MindWeaverTheme.colors.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Password Field
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { password ->
                        intentHandler(AuthenticationStore.Intent.UpdatePassword(password))
                    },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Password"
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = !state.isPasswordValid && state.password.isNotBlank(),
                    supportingText = {
                        if (!state.isPasswordValid && state.password.isNotBlank()) {
                            Text(
                                text = "Password must be at least 6 characters long",
                                color = MindWeaverTheme.colors.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Error Display
                if (state.error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MindWeaverTheme.colors.errorSurface
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = CardDefaults.outlinedCardBorder().brush
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.error,
                                color = MindWeaverTheme.colors.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { intentHandler(AuthenticationStore.Intent.ClearError) }
                            ) {
                                Text(
                                    text = "Dismiss",
                                    color = MindWeaverTheme.colors.error
                                )
                            }
                        }
                    }
                }

                // Sign In Button
                Button(
                    onClick = { intentHandler(AuthenticationStore.Intent.SignIn) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && state.email.isNotBlank() && state.password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MindWeaverTheme.colors.accent500,
                        contentColor = MindWeaverTheme.colors.textInvert,
                        disabledContainerColor = MindWeaverTheme.colors.surface4,
                        disabledContentColor = MindWeaverTheme.colors.textDisabled
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MindWeaverTheme.colors.textInvert
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Signing In...")
                    } else {
                        Text("Sign In")
                    }
                }
            }
        }

        // Footer
        Text(
            text = "Enter any valid email and password (min 6 chars) to continue",
            style = MaterialTheme.typography.bodySmall,
            color = MindWeaverTheme.colors.textMuted,
            modifier = Modifier.padding(top = 24.dp),
            textAlign = TextAlign.Center
        )
    }
}