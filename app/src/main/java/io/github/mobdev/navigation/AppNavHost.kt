package io.github.mobdev.navigation

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.mobdev.R
import io.github.mobdev.ui.AppViewModel
import io.github.mobdev.ui.AuthState
import io.github.mobdev.ui.channels.ChannelsScreen
import io.github.mobdev.ui.image.ImageScreen
import io.github.mobdev.ui.login.LoginScreen
import io.github.mobdev.ui.messages.MessagesScreen

private const val ROUTE_LOGIN = "login"
private const val ROUTE_CHANNELS = "channels"
private const val ROUTE_MESSAGES = "messages/{channelId}"
private const val ROUTE_IMAGE = "image/{imagePath}"

@Composable
fun AppNavHost(navController: NavHostController, appViewModel: AppViewModel) {
    val authState by appViewModel.authState.collectAsState()
    val selectedChannel by appViewModel.selectedChannel.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.NotLoggedIn -> navController.navigate(ROUTE_LOGIN) {
                popUpTo(0) { inclusive = true }
            }
            is AuthState.LoggedIn -> if (currentRoute == ROUTE_LOGIN || currentRoute == null) {
                navController.navigate(ROUTE_CHANNELS) {
                    popUpTo(0) { inclusive = true }
                }
            }
            AuthState.Checking -> Unit
        }
    }

    LaunchedEffect(isLandscape, currentRoute) {
        if (isLandscape && currentRoute?.startsWith("messages/") == true) {
            navController.popBackStack(ROUTE_CHANNELS, inclusive = false)
        }
    }

    NavHost(navController = navController, startDestination = ROUTE_LOGIN) {
        composable(ROUTE_LOGIN) {
            LoginScreen(appViewModel = appViewModel)
        }

        composable(ROUTE_CHANNELS) {
            if (isLandscape) {
                LandscapeLayout(
                    appViewModel = appViewModel,
                    selectedChannel = selectedChannel,
                    onChannelClick = { appViewModel.selectChannel(it) },
                    onImageClick = { path ->
                        navController.navigate("image/${Uri.encode(path)}")
                    },
                    onLogout = {
                        appViewModel.logout()
                    }
                )
            } else {
                ChannelsScreen(
                    appViewModel = appViewModel,
                    selectedChannel = selectedChannel,
                    onChannelClick = { channel ->
                        appViewModel.selectChannel(channel)
                        navController.navigate("messages/${Uri.encode(channel)}")
                    },
                    onLogout = { appViewModel.logout() }
                )
            }
        }

        composable(ROUTE_MESSAGES) { backStackEntry ->
            val channelId = Uri.decode(backStackEntry.arguments?.getString("channelId") ?: "")
            MessagesScreen(
                channelId = channelId,
                appViewModel = appViewModel,
                onImageClick = { path ->
                    navController.navigate("image/${Uri.encode(path)}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_IMAGE) { backStackEntry ->
            val imagePath = Uri.decode(backStackEntry.arguments?.getString("imagePath") ?: "")
            ImageScreen(
                imagePath = imagePath,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun LandscapeLayout(
    appViewModel: AppViewModel,
    selectedChannel: String?,
    onChannelClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    BackHandler(enabled = selectedChannel != null) {
        appViewModel.selectChannel(null)
    }

    Row(modifier = Modifier.fillMaxSize()) {
        ChannelsScreen(
            appViewModel = appViewModel,
            selectedChannel = selectedChannel,
            onChannelClick = onChannelClick,
            onLogout = onLogout,
            modifier = Modifier.width(280.dp)
        )
        VerticalDivider()
        if (selectedChannel != null) {
            MessagesScreen(
                channelId = selectedChannel,
                appViewModel = appViewModel,
                onImageClick = onImageClick,
                onBack = null,
                modifier = Modifier.weight(1f)
            )
        } else {
            NoChannelSelected(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NoChannelSelected(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(stringResource(R.string.select_chat))
    }
}
