package com.mobitech.inventario.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mobitech.inventario.ui.viewmodel.LoginEvent
import com.mobitech.inventario.ui.viewmodel.LoginState
import androidx.compose.ui.tooling.preview.Preview
import com.mobitech.inventario.ui.theme.InventarioTheme

@Composable
fun LoginScreen(state: LoginState, onEvent: (LoginEvent) -> Unit, navigateHome: () -> Unit) {
    LaunchedEffect(state.logged) { if (state.logged) navigateHome() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Inventario", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.username,
                onValueChange = { onEvent(LoginEvent.OnUsername(it)) },
                label = { Text("Usuário") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = { onEvent(LoginEvent.OnPassword(it)) },
                label = { Text("Senha") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onEvent(LoginEvent.Submit) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Entrar")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Login")
@Composable
private fun LoginScreenPreview() {
    InventarioTheme {
        LoginScreen(state = LoginState(username = "admin"), onEvent = {}, navigateHome = {})
    }
}
