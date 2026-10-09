package com.example.intellipatassignment.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.intellipatassignment.R
import com.example.intellipatassignment.domain.CredentialsValidator
import com.example.intellipatassignment.domain.EmailError
import com.example.intellipatassignment.domain.PasswordError
import com.example.intellipatassignment.ui.common.BrandMark
import com.example.intellipatassignment.ui.common.message
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoute(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LoginEvent.LoggedIn -> onLoggedIn()
            }
        }
    }

    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::onLoginClick,
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrandMark()
                Text(stringResource(R.string.login_brand), style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(72.dp))

            Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = { Text(stringResource(R.string.email)) },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    enabled = !state.isLoading,
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { { Text(it.text()) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.password)) },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                stringResource(if (passwordVisible) R.string.hide_password else R.string.show_password),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    enabled = !state.isLoading,
                    isError = state.passwordError != null,
                    supportingText = state.passwordError?.let { { Text(it.text()) } },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.error?.let { error ->
                Text(
                    error.text(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onLoginClick,
                enabled = !state.isLoading,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.login), style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.login_demo_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmailError.text() = stringResource(
    when (this) {
        EmailError.Empty -> R.string.error_email_empty
        EmailError.Invalid -> R.string.error_email_invalid
    },
)

@Composable
private fun PasswordError.text() = when (this) {
    PasswordError.Empty -> stringResource(R.string.error_password_empty)
    PasswordError.TooShort -> stringResource(R.string.error_password_short, CredentialsValidator.MIN_PASSWORD_LENGTH)
}

@Composable
private fun LoginError.text() = when (this) {
    LoginError.InvalidCredentials -> stringResource(R.string.error_invalid_credentials)
    is LoginError.Failure -> kind.message()
}
