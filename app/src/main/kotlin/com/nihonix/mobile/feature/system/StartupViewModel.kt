package com.nihonix.mobile.feature.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StartupViewModel(private val repository: StartupRepository, private val version: Long) : ViewModel() {
    private val state = MutableStateFlow(StartupState())
    val uiState = state.asStateFlow()
    private val health = MutableStateFlow<String?>(null)
    val healthStatus = health.asStateFlow()
    fun checkHealth() {
        if (health.value == "Checking service…") return
        health.value = "Checking service…"
        viewModelScope.launch {
            health.value = if (repository.health()) "Service is reachable." else "Service could not be reached."
        }
    }
    private var job: Job? = null
    init { refresh() }
    fun refresh() {
        job?.cancel()
        state.value = StartupState()
        health.value = null
        job = viewModelScope.launch { state.value = repository.load(version) }
    }
    fun continueWithoutCheck() {
        if (state.value.status == StartupStatus.UNAVAILABLE)
            state.value = StartupState(StartupStatus.READY, warning = "Availability could not be checked. Online features may be unavailable.")
    }
}
