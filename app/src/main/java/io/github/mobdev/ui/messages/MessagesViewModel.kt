package io.github.mobdev.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mobdev.api.RetrofitClient
import io.github.mobdev.api.models.Message
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MessagesUiState {
    data object Loading : MessagesUiState()
    data class Loaded(val messages: List<Message>, val isLoadingMore: Boolean = false) : MessagesUiState()
    data class Error(val message: String) : MessagesUiState()
}

class MessagesViewModel(
    val channelId: String,
    val username: String
) : ViewModel() {

    private val repository = ChatRepository(RetrofitClient.apiService)

    private val _uiState = MutableStateFlow<MessagesUiState>(MessagesUiState.Loading)
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    private var oldestId: String? = null
    private var canLoadMore = true
    private var initialLoaded = false

    fun loadIfNeeded(onUnauthorized: () -> Unit) {
        if (initialLoaded) return
        initialLoaded = true
        viewModelScope.launch { loadInitial(onUnauthorized) }
    }

    private suspend fun loadInitial(onUnauthorized: () -> Unit) {
        when (val result = repository.getMessages(channelId, limit = 20, reverse = true)) {
            is Result.Success -> {
                val msgs = result.data.reversed()
                oldestId = msgs.firstOrNull()?.id
                canLoadMore = result.data.size == 20
                _uiState.value = MessagesUiState.Loaded(msgs)
            }
            is Result.Error -> {
                initialLoaded = false
                if (result.isUnauthorized) onUnauthorized()
                else _uiState.value = MessagesUiState.Error(result.message)
            }
        }
    }

    fun loadMore(onUnauthorized: () -> Unit) {
        if (!canLoadMore) return
        val current = _uiState.value as? MessagesUiState.Loaded ?: return
        if (current.isLoadingMore) return
        _uiState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            when (val result = repository.getMessages(channelId, limit = 20, lastKnownId = oldestId, reverse = true)) {
                is Result.Success -> {
                    val older = result.data.reversed()
                    canLoadMore = result.data.size == 20
                    oldestId = older.firstOrNull()?.id ?: oldestId
                    _uiState.value = MessagesUiState.Loaded(older + current.messages)
                }
                is Result.Error -> {
                    _uiState.value = current.copy(isLoadingMore = false)
                    if (result.isUnauthorized) onUnauthorized()
                }
            }
        }
    }

    fun sendMessage(text: String, onUnauthorized: () -> Unit) {
        if (text.isBlank()) return
        viewModelScope.launch {
            when (val result = repository.sendMessage(username, channelId, text)) {
                is Result.Success -> {
                    val current = _uiState.value as? MessagesUiState.Loaded ?: return@launch
                    when (val fresh = repository.getMessages(channelId, limit = 20, reverse = true)) {
                        is Result.Success -> {
                            val msgs = fresh.data.reversed()
                            oldestId = current.messages.firstOrNull()?.id ?: oldestId
                            _uiState.value = MessagesUiState.Loaded(msgs)
                        }
                        is Result.Error -> if (fresh.isUnauthorized) onUnauthorized()
                    }
                }
                is Result.Error -> if (result.isUnauthorized) onUnauthorized()
            }
        }
    }

    class Factory(private val channelId: String, private val username: String) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MessagesViewModel(channelId, username) as T
    }
}
