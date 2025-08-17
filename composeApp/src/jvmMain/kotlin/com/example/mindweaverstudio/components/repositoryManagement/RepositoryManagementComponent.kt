package com.example.mindweaverstudio.components.repositoryManagement

import kotlinx.coroutines.flow.StateFlow

interface RepositoryManagementComponent {
    val state: StateFlow<RepositoryManagementStore.State>

    fun onIntent(intent: RepositoryManagementStore.Intent)
}