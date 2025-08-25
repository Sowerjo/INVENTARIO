@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.mobitech.inventario.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobitech.inventario.domain.model.InventoryStatus
import com.mobitech.inventario.ui.viewmodel.InventoryDetailEvent
import com.mobitech.inventario.ui.viewmodel.InventoryDetailState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.tooling.preview.Preview
import com.mobitech.inventario.domain.model.InventoryEntity
import com.mobitech.inventario.domain.model.InventoryItemEntity
import com.mobitech.inventario.ui.theme.InventarioTheme

@Composable
fun InventoryDetailScreen(
    state: InventoryDetailState,
    onEvent: (InventoryDetailEvent) -> Unit,
    onBack: () -> Unit
) {
    val inv = state.inventory
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(inv?.name ?: "Detalhe") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        },
        floatingActionButton = {
            if (inv != null && inv.status == InventoryStatus.ONGOING) {
                ExtendedFloatingActionButton(onClick = { onEvent(InventoryDetailEvent.Finish) }, containerColor = MaterialTheme.colorScheme.tertiary) {
                    if (state.finishing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Finalizar")
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp)) {
            if (inv == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            Text("Status: ${inv.status}", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.productCode,
                    onValueChange = { onEvent(InventoryDetailEvent.OnCode(it)) },
                    label = { Text("Código") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = state.qty,
                    onValueChange = { onEvent(InventoryDetailEvent.OnQty(it)) },
                    label = { Text("Qtd") },
                    singleLine = true,
                    modifier = Modifier.width(100.dp)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onEvent(InventoryDetailEvent.AddItem) }, enabled = !state.adding && inv.status == InventoryStatus.ONGOING) {
                    if (state.adding) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Add")
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            if (state.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sem itens") }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.items) { item ->
                        ListItem(
                            headlineContent = { Text(item.productCode) },
                            supportingContent = { Text("Qtd: ${item.qtyCounted}") }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Inventário Detalhe")
@Composable
private fun InventoryDetailScreenPreview() {
    val inv = InventoryEntity(id=1, name="Inventário Loja 1", note="Turno manhã", status = InventoryStatus.ONGOING, createdByUserId = 1)
    val items = listOf(
        InventoryItemEntity(id=1, inventoryId=1, productCode="P1", qtyCounted=10.0, countedByUserId = 1),
        InventoryItemEntity(id=2, inventoryId=1, productCode="P2", qtyCounted=5.0, countedByUserId = 1)
    )
    InventarioTheme {
        InventoryDetailScreen(
            state = InventoryDetailState(inventory = inv, items = items, productCode = "P3", qty = "1"),
            onEvent = {},
            onBack = {}
        )
    }
}
