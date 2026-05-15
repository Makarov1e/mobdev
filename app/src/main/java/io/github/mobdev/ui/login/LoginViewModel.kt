package io.github.mobdev.ui.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun setLoading() { _uiState.value = LoginUiState.Loading }
    fun setError(message: String) { _uiState.value = LoginUiState.Error(message) }
    fun resetError() { _uiState.value = LoginUiState.Idle }
}
