package io.github.mobdev.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.Graph
import io.github.mobdev.data.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Элемент списка чата: либо доставленное сообщение, либо ожидающее отправки (pending). */
data class ChatItem(
    val key: String,
    val from: String,
    val text: String?,
    val imageLink: String?,
    val isPending: Boolean
)

data class MessagesUiState(
    val items: List<ChatItem> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isOffline: Boolean = false
)

class MessagesViewModel(
    private val channelId: String,
    private val username: String
) : ViewModel() {

    private val repository = Graph.repository
    private val connectivity = Graph.connectivity

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    private var canLoadMore = true
    private val flushMutex = Mutex()

    init {
        // Отображаем из кэша: доставленные сообщения + ожидающие отправки.
        viewModelScope.launch {
            combine(
                repository.messagesStream(channelId),
                repository.outboxStream(channelId)
            ) { messages, outbox ->
                val delivered = messages.map { m ->
                    ChatItem(
                        key = m.id,
                        from = m.from,
                        text = m.data.text?.text,
                        imageLink = m.data.image?.link,
                        isPending = false
                    )
                }
                val pending = outbox.map { o ->
                    ChatItem(
                        key = "local:${o.localId}",
                        from = o.from,
                        text = o.text,
                        imageLink = null,
                        isPending = true
                    )
                }
                delivered + pending
            }.collect { items ->
                _uiState.value = _uiState.value.copy(
                    items = items,
                    isLoading = _uiState.value.isLoading && items.isEmpty()
                )
            }
        }
    }

    fun start(onUnauthorized: () -> Unit) {
        refreshLatest(onUnauthorized)
        viewModelScope.launch {
            connectivity.online.collect { online ->
                _uiState.value = _uiState.value.copy(isOffline = !online)
                if (online) {
                    flush(onUnauthorized)
                    refreshLatest(onUnauthorized)
                }
            }
        }
    }

    private fun refreshLatest(onUnauthorized: () -> Unit) {
        viewModelScope.launch {
            when (val result = repository.refreshLatest(channelId)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isLoading = false)
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    if (result.isUnauthorized) onUnauthorized()
                }
            }
        }
    }

    fun loadMore(onUnauthorized: () -> Unit) {
        if (!canLoadMore || _uiState.value.isLoadingMore) return
        _uiState.value = _uiState.value.copy(isLoadingMore = true)
        viewModelScope.launch {
            when (val result = repository.loadOlder(channelId)) {
                is Result.Success -> canLoadMore = result.data == 20
                is Result.Error -> if (result.isUnauthorized) onUnauthorized()
            }
            _uiState.value = _uiState.value.copy(isLoadingMore = false)
        }
    }

    /** Сообщение всегда кладётся в очередь и тут же отправляется, если есть сеть. */
    fun sendMessage(text: String, onUnauthorized: () -> Unit) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.enqueue(username, channelId, text.trim())
            flush(onUnauthorized)
            refreshLatest(onUnauthorized)
        }
    }

    private suspend fun flush(onUnauthorized: () -> Unit) {
        flushMutex.withLock {
            val result = repository.flushOutbox()
            if (result is Result.Error && result.isUnauthorized) onUnauthorized()
        }
    }

    class Factory(private val channelId: String, private val username: String) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MessagesViewModel(channelId, username) as T
    }
}
