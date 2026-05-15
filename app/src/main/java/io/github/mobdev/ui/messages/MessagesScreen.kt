package io.github.mobdev.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import io.github.mobdev.R
import io.github.mobdev.api.RetrofitClient
import io.github.mobdev.api.models.Message
import io.github.mobdev.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    channelId: String,
    appViewModel: AppViewModel,
    onImageClick: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val authState by appViewModel.authState.collectAsState()
    val username = (authState as? io.github.mobdev.ui.AuthState.LoggedIn)?.username ?: ""

    val messagesViewModel: MessagesViewModel = viewModel(
        key = channelId,
        factory = MessagesViewModel.Factory(channelId, username)
    )

    val uiState by messagesViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var inputText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(channelId) {
        messagesViewModel.loadIfNeeded { appViewModel.handleUnauthorized() }
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is MessagesUiState.Loaded && state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index == 0 && uiState is MessagesUiState.Loaded) {
                    messagesViewModel.loadMore { appViewModel.handleUnauthorized() }
                }
            }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(channelId) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            MessageInput(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    messagesViewModel.sendMessage(inputText) { appViewModel.handleUnauthorized() }
                    inputText = ""
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is MessagesUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is MessagesUiState.Error -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { Text(state.message) }

            is MessagesUiState.Loaded -> Column(Modifier.fillMaxSize().padding(paddingValues)) {
                if (state.isLoadingMore) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageItem(message, onImageClick)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageItem(message: Message, onImageClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            text = message.from,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(2.dp))
        val textContent = message.data.text
        val imageContent = message.data.image
        when {
            textContent != null -> Text(text = textContent.text, style = MaterialTheme.typography.bodyMedium)
            imageContent != null -> {
                val link = imageContent.link ?: ""
                AsyncImage(
                    model = "${RetrofitClient.BASE_URL}thumb/$link",
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .clickable { onImageClick(link) }
                )
            }
        }
    }
}

@Composable
private fun MessageInput(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(stringResource(R.string.message_hint)) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onSend, enabled = text.isNotBlank()) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
        }
    }
}
