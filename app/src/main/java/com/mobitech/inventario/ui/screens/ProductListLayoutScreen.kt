@file:OptIn(ExperimentalMaterial3Api::class)
package com.mobitech.inventario.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobitech.inventario.domain.model.ProductListLayoutEntity
import com.mobitech.inventario.ui.theme.InventarioTheme
import com.mobitech.inventario.ui.viewmodel.*
import java.text.NumberFormat
import java.util.*

@Composable
fun ProductListLayoutScreen(
    state: ProductListLayoutState,
    onEvent: (ProductListLayoutEvent) -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(ProductListLayoutEvent.ClearMessage)
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(ProductListLayoutEvent.ClearError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Layout da Lista") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(ProductListLayoutEvent.SuggestDefaults) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Sugerir padrões")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(ProductListLayoutEvent.SaveLayout) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.Save, contentDescription = "Salvar")
                }
            }
        }
    ) { paddingValues ->

        if (state.loading && state.availableKeys.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Preview Card
                PreviewCard(previewItem = state.previewItem)

                // Line 1 Configuration
                Line1ConfigCard(
                    selectedKey = state.layout.line1Key,
                    availableKeys = state.availableKeys,
                    onKeySelected = { onEvent(ProductListLayoutEvent.UpdateLine1Key(it)) }
                )

                // Line 2 Configuration
                Line2ConfigCard(
                    selectedKey = state.layout.line2Keys.split(",").filter { it.isNotBlank() }.firstOrNull() ?: "",
                    availableKeys = state.availableKeys,
                    onKeySelected = { key -> onEvent(ProductListLayoutEvent.UpdateLine2Keys(if (key.isNotEmpty()) listOf(key) else emptyList())) }
                )

                // Line 3 Configuration
                Line3ConfigCard(
                    selectedKey = state.layout.line3Keys.split(",").filter { it.isNotBlank() }.firstOrNull() ?: "",
                    availableKeys = state.availableKeys,
                    onKeySelected = { key -> onEvent(ProductListLayoutEvent.UpdateLine3Keys(if (key.isNotEmpty()) listOf(key) else emptyList())) }
                )

                // Quantity Configuration
                QtyConfigCard(
                    selectedKey = state.layout.qtyKey,
                    availableKeys = state.availableKeys,
                    onKeySelected = { onEvent(ProductListLayoutEvent.UpdateQtyKey(it)) }
                )

                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }
        }
    }
}

@Composable
private fun PreviewCard(previewItem: PreviewItem) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = previewItem.line1,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = previewItem.line2,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = previewItem.line3,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = previewItem.quantity,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FloatingActionButton(
                        onClick = { },
                        modifier = Modifier.size(32.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Adicionar",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Line1ConfigCard(
    selectedKey: String,
    availableKeys: List<String>,
    onKeySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Linha 1 - Título (obrigatório)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Campo principal em negrito",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedKey,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Campo selecionado") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableKeys.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(key) },
                            onClick = {
                                onKeySelected(key)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Line2ConfigCard(
    selectedKey: String,
    availableKeys: List<String>,
    onKeySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Linha 2 - Subtítulo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Campo secundário",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedKey,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Campo selecionado") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableKeys.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(key) },
                            onClick = {
                                onKeySelected(key)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Line3ConfigCard(
    selectedKey: String,
    availableKeys: List<String>,
    onKeySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Linha 3 - Rodapé",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Preços formatados como moeda",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedKey,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Campo selecionado") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableKeys.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(key) },
                            onClick = {
                                onKeySelected(key)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QtyConfigCard(
    selectedKey: String,
    availableKeys: List<String>,
    onKeySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Quantidade - Coluna direita",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Campo numérico para incremento",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedKey,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Campo selecionado") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableKeys.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(key) },
                            onClick = {
                                onKeySelected(key)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Layout Config")
@Composable
private fun ProductListLayoutScreenPreview() {
    val sampleState = ProductListLayoutState(
        layout = ProductListLayoutEntity(
            line1Key = "nome",
            line2Keys = "ean",
            line3Keys = "preco",
            qtyKey = "quantidade"
        ),
        availableKeys = listOf("nome", "sku", "ean", "preco", "categoria", "quantidade", "marca"),
        previewItem = PreviewItem(
            line1 = "Produto Exemplo",
            line2 = "1234567890123",
            line3 = "R$ 25,90",
            quantity = "10"
        )
    )

    InventarioTheme {
        ProductListLayoutScreen(
            state = sampleState,
            onEvent = {},
            onBack = {}
        )
    }
}
