@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.mobitech.inventario.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobitech.inventario.domain.model.CategoryEntity
import com.mobitech.inventario.ui.theme.InventarioTheme
import com.mobitech.inventario.ui.viewmodel.CategorySettingsEvent
import com.mobitech.inventario.ui.viewmodel.CategorySettingsState

@Composable
fun CategorySettingsScreen(
    state: CategorySettingsState,
    onEvent: (CategorySettingsEvent) -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbarHostState.showSnackbar(it); onEvent(CategorySettingsEvent.ClearMessage) } }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it); onEvent(CategorySettingsEvent.ClearError) } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onEvent(CategorySettingsEvent.EnableAll) }, enabled = !state.loading) {
                    if (state.loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Habilitar todas")
                }
                OutlinedButton(onClick = { onEvent(CategorySettingsEvent.DisableAll) }, enabled = !state.loading) {
                    Text("Desabilitar todas")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = { onEvent(CategorySettingsEvent.UpdateQuery(it)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar categoria...") },
                label = { Text("Busca") }
            )
            Spacer(Modifier.height(8.dp))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(8.dp))
            val listToShow = state.filtered
            if (listToShow.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sem categorias") }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(listToShow) { cat ->
                        ListItem(
                            headlineContent = { Text(cat.name.ifBlank { "(vazia)" }) },
                            trailingContent = {
                                Switch(checked = cat.enabled, onCheckedChange = { onEvent(CategorySettingsEvent.Toggle(cat.id, it)) }, enabled = !state.loading)
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Categorias")
@Composable
private fun CategorySettingsScreenPreview() {
    val cats = listOf(
        CategoryEntity(id=1, name="Preço", enabled = true),
        CategoryEntity(id=2, name="Quantidade", enabled = true),
        CategoryEntity(id=3, name="Marca", enabled = false)
    )
    InventarioTheme {
        CategorySettingsScreen(
            state = CategorySettingsState(categories = cats, filtered = cats),
            onEvent = {},
            onBack = {}
        )
    }
}
