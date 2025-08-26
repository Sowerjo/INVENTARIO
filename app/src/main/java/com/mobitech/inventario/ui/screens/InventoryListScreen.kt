@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mobitech.inventario.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobitech.inventario.domain.model.InventoryEntity
import com.mobitech.inventario.domain.model.InventoryStatus
import com.mobitech.inventario.ui.theme.InventarioTheme
import com.mobitech.inventario.ui.viewmodel.InventoryListEvent
import com.mobitech.inventario.ui.viewmodel.InventoryListState

@Composable
fun InventoryListScreen(
    state: InventoryListState,
    onEvent: (InventoryListEvent) -> Unit,
    onOpen: (Long) -> Unit,
    onBack: () -> Unit,
    onOpenConfig: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventários") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenConfig) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configurações")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { if (!state.creating) onEvent(InventoryListEvent.Create) }) {
                if (state.creating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("+")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp)) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { onEvent(InventoryListEvent.OnName(it)) },
                label = { Text("Nome novo inventário") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = { onEvent(InventoryListEvent.OnNote(it)) },
                label = { Text("Observação") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            if (state.inventories.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Vazio") }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.inventories) { inv ->
                        ListItem(
                            headlineContent = { Text(inv.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(inv.status.name) },
                            modifier = Modifier.clickable { onOpen(inv.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Inventários")
@Composable
private fun InventoryListScreenPreview() {
    val sample = listOf(
        InventoryEntity(id=1, name="Inventário Loja 1", note="Turno manhã", status=InventoryStatus.ONGOING, createdByUserId = 1),
        InventoryEntity(id=2, name="Inventário CD", note=null, status=InventoryStatus.FINISHED, createdByUserId = 1)
    )
    InventarioTheme {
        InventoryListScreen(
            state = InventoryListState(inventories = sample, name = "Novo Inventário", note = "Observação"),
            onEvent = {},
            onOpen = {},
            onBack = {},
            onOpenConfig = {}
        )
    }
}
