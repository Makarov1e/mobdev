package io.github.mobdev.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.api.RetrofitClient
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ChannelsUiState {
    data object Loading : ChannelsUiState()
    data class Loaded(val channels: List<String>) : ChannelsUiState()
    data class Error(val message: String) : ChannelsUiState()
}

class ChannelsViewModel : ViewModel() {
    private val repository = ChatRepository(RetrofitClient.apiService)

    private val _uiState = MutableStateFlow<ChannelsUiState>(ChannelsUiState.Loading)
    val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()

    private var loaded = false

    fun loadIfNeeded(onUnauthorized: () -> Unit) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            when (val result = repository.getChannels()) {
                is Result.Success -> _uiState.value = ChannelsUiState.Loaded(result.data)
                is Result.Error -> {
                    loaded = false
                    if (result.isUnauthorized) onUnauthorized()
                    else _uiState.value = ChannelsUiState.Error(result.message)
                }
            }
        }
    }
}
