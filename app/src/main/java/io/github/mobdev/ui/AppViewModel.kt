package io.github.mobdev.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.api.RetrofitClient
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.Result
import io.github.mobdev.data.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Checking : AuthState()
    data object NotLoggedIn : AuthState()
    data class LoggedIn(val token: String, val username: String) : AuthState()
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = TokenStorage(app)
    private val repository = ChatRepository(RetrofitClient.apiService)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _selectedChannel = MutableStateFlow<String?>(null)
    val selectedChannel: StateFlow<String?> = _selectedChannel.asStateFlow()

    init {
        val username = storage.username
        val password = storage.password
        if (username != null && password != null) {
            tryAutoLogin(username, password)
        } else {
            _authState.value = AuthState.NotLoggedIn
        }
    }

    private fun tryAutoLogin(username: String, password: String) {
        viewModelScope.launch {
            when (val result = repository.login(username, password)) {
                is Result.Success -> {
                    RetrofitClient.authInterceptor.token = result.data
                    _authState.value = AuthState.LoggedIn(result.data, username)
                }
                is Result.Error -> {
                    _authState.value = AuthState.NotLoggedIn
                }
            }
        }
    }

    fun login(username: String, password: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.login(username, password)) {
                is Result.Success -> {
                    storage.username = username
                    storage.password = password
                    RetrofitClient.authInterceptor.token = result.data
                    _authState.value = AuthState.LoggedIn(result.data, username)
                }
                is Result.Error -> onError(result.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
        RetrofitClient.authInterceptor.token = null
        storage.clear()
        _authState.value = AuthState.NotLoggedIn
        _selectedChannel.value = null
    }

    fun selectChannel(channelName: String?) {
        _selectedChannel.value = channelName
    }

    fun handleUnauthorized() {
        RetrofitClient.authInterceptor.token = null
        val username = storage.username
        val password = storage.password
        if (username != null && password != null) {
            tryAutoLogin(username, password)
        } else {
            storage.clear()
            _authState.value = AuthState.NotLoggedIn
        }
    }
}
