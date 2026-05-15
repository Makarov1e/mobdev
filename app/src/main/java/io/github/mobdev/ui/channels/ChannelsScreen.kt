package io.github.mobdev.ui.channels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mobdev.R
import io.github.mobdev.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    appViewModel: AppViewModel,
    selectedChannel: String?,
    onChannelClick: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    channelsViewModel: ChannelsViewModel = viewModel()
) {
    val uiState by channelsViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        channelsViewModel.loadIfNeeded { appViewModel.handleUnauthorized() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.channels_title)) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.logout)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ChannelsUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is ChannelsUiState.Error -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { Text(state.message) }

            is ChannelsUiState.Loaded -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                items(state.channels) { channel ->
                    val isSelected = channel == selectedChannel
                    ListItem(
                        headlineContent = { Text(channel) },
                        leadingContent = {
                            Icon(Icons.Default.Tag, contentDescription = null)
                        },
                        colors = if (isSelected)
                            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        else
                            ListItemDefaults.colors(),
                        modifier = Modifier.clickable { onChannelClick(channel) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
