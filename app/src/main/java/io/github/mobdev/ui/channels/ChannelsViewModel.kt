package io.github.mobdev.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.Graph
import io.github.mobdev.data.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChannelsUiState(
    val channels: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val error: String? = null
)

class ChannelsViewModel : ViewModel() {
    private val repository = Graph.repository
    private val connectivity = Graph.connectivity

    private val _uiState = MutableStateFlow(ChannelsUiState())
    val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()

    init {
        // Кэш — единственный источник правды для отображения: показываем сразу.
        viewModelScope.launch {
            repository.channelsStream().collect { cached ->
                _uiState.value = _uiState.value.copy(
                    channels = cached,
                    isLoading = _uiState.value.isLoading && cached.isEmpty()
                )
            }
        }
    }

    fun start(onUnauthorized: () -> Unit) {
        refresh(onUnauthorized)
        // При восстановлении сети автоматически дозагружаем актуальный список.
        viewModelScope.launch {
            connectivity.online.collect { online ->
                _uiState.value = _uiState.value.copy(isOffline = !online)
                if (online) refresh(onUnauthorized)
            }
        }
    }

    private fun refresh(onUnauthorized: () -> Unit) {
        viewModelScope.launch {
            when (val result = repository.refreshChannels()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                is Result.Error -> {
                    if (result.isUnauthorized) onUnauthorized()
                    // Есть кэш — продолжаем показывать его, ошибку не навязываем.
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (_uiState.value.channels.isEmpty()) result.message else null
                    )
                }
            }
        }
    }
}
