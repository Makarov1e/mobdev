package io.github.mobdev.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mobdev.R
import io.github.mobdev.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    appViewModel: AppViewModel,
    loginViewModel: LoginViewModel = viewModel()
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val uiState by loginViewModel.uiState.collectAsState()

    val isLoading = uiState is LoginUiState.Loading

    if (uiState is LoginUiState.Error) {
        AlertDialog(
            onDismissRequest = { loginViewModel.resetError() },
            title = { Text(stringResource(R.string.login_error_title)) },
            text = { Text((uiState as LoginUiState.Error).message) },
            confirmButton = {
                TextButton(onClick = { loginViewModel.resetError() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.login_title)) }) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.login_username)) },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.login_password)) },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { doLogin(username, password, loginViewModel, appViewModel) }),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { doLogin(username, password, loginViewModel, appViewModel) },
                    enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text(stringResource(R.string.login_button))
                    }
                }
            }
        }
    }
}

private fun doLogin(
    username: String,
    password: String,
    loginViewModel: LoginViewModel,
    appViewModel: AppViewModel
) {
    if (username.isBlank() || password.isBlank()) return
    loginViewModel.setLoading()
    appViewModel.login(username, password) { error ->
        loginViewModel.setError(error)
    }
}
