@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.mobitech.inventario.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobitech.inventario.domain.model.ProductEntity
import com.mobitech.inventario.domain.model.ProductListLayoutEntity
import com.mobitech.inventario.ui.theme.InventarioTheme
import com.mobitech.inventario.ui.viewmodel.ProductListEvent
import com.mobitech.inventario.ui.viewmodel.ProductListState

@Composable
fun ProductListScreen(
    state: ProductListState,
    onEvent: (ProductListEvent) -> Unit,
    onBack: () -> Unit,
    onOpenLayoutSettings: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            try {
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "produtos.xlsx"
                context.contentResolver.openInputStream(uri)?.use { ins ->
                    val bytes = ins.readBytes()
                    onEvent(ProductListEvent.OnXlsxLoaded(name, bytes))
                }
            } catch (e: Exception) {
                onEvent(ProductListEvent.SetError("Falha ao ler arquivo"))
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        uri?.let { onEvent(ProductListEvent.ExportToUri(it)) }
    }

    LaunchedEffect(state.message) { state.message?.let { snackbarHostState.showSnackbar(it); onEvent(ProductListEvent.ClearMessage) } }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it); onEvent(ProductListEvent.ClearError) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produtos") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } },
                actions = {
                    IconButton(onClick = onOpenLayoutSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configurar layout")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // Seção de importação (colapsável)
            ImportSection(state, onEvent, onPickFile = { filePicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/octet-stream")) }, onExportFile = {
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                exportLauncher.launch("produtos_$timestamp.xlsx")
            })

            // Barra de busca com scanner
            SearchSection(
                searchQuery = state.searchQuery,
                isScanning = state.isScanning,
                onSearchChanged = { onEvent(ProductListEvent.UpdateSearchQuery(it)) },
                onStartScanning = { onEvent(ProductListEvent.StartScanning) },
                onStopScanning = { onEvent(ProductListEvent.StopScanning) }
            )

            // Lista de produtos
            ProductsListWithLayout(
                products = state.filteredProducts,
                layout = state.layout,
                attributes = state.productAttributes
            )
        }
    }
}

@Composable
private fun ImportSection(
    state: ProductListState,
    onEvent: (ProductListEvent) -> Unit,
    onPickFile: () -> Unit,
    onExportFile: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Importar XLSX", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Recolher" else "Expandir"
                    )
                }
            }

            if (isExpanded) {
                Spacer(Modifier.height(8.dp))
                Text("Primeira linha = cabeçalhos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPickFile) { Text("Escolher arquivo") }
                    OutlinedButton(onClick = { onEvent(ProductListEvent.ClearFile) }, enabled = state.xlsxFileName != null && !state.importing) { Text("Limpar") }
                }

                if (state.xlsxFileName != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("${state.xlsxFileName} (${state.xlsxSize} bytes)", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onEvent(ProductListEvent.ImportAppend) }, enabled = !state.importing && state.xlsxFileName != null) {
                        if (state.importing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Importar +")
                    }
                    Button(onClick = { onEvent(ProductListEvent.ImportOverwrite) }, enabled = !state.importing && state.xlsxFileName != null) { Text("Sobrescrever") }
                    OutlinedButton(onClick = onExportFile, enabled = !state.exporting) {
                        if (state.exporting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Exportar XLSX")
                    }
                }

                if (state.importing) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    val pct = if (state.importTotal > 0) (state.importProgress * 100 / state.importTotal) else 0
                    Text("Importando: ${state.importProgress}/${state.importTotal} ($pct%)", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SearchSection(
    searchQuery: String,
    isScanning: Boolean,
    onSearchChanged: (String) -> Unit,
    onStartScanning: () -> Unit,
    onStopScanning: () -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChanged,
        placeholder = { Text("Buscar item") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
        trailingIcon = {
            IconButton(
                onClick = if (isScanning) onStopScanning else onStartScanning
            ) {
                Icon(
                    if (isScanning) Icons.Filled.Stop else Icons.Filled.QrCodeScanner,
                    contentDescription = if (isScanning) "Parar scanner" else "Scanner"
                )
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun ProductsListWithLayout(
    products: List<ProductEntity>,
    layout: ProductListLayoutEntity,
    attributes: Map<String, Map<String, String?>>,
) {
    if (products.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Inventory,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Nenhum produto encontrado",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Importe um arquivo XLSX para começar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(products, key = { it.code }) { product ->
            ProductItemWithCustomLayout(
                product = product,
                layout = layout,
                attributes = attributes[product.code] ?: emptyMap(),
                modifier = Modifier.animateItem()
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ProductItemWithCustomLayout(
    product: ProductEntity,
    layout: ProductListLayoutEntity,
    attributes: Map<String, String?>,
    modifier: Modifier = Modifier
) {
    // Extrair valores conforme layout
    val line1Value = attributes[layout.line1Key] ?: product.name

    val line2Keys = layout.line2Keys.split(",").filter { it.isNotBlank() }
    val line2Values = line2Keys.mapNotNull { key ->
        attributes[key]?.takeIf { it.isNotBlank() }
    }
    val line2Text = line2Values.joinToString(" • ")

    val line3Keys = layout.line3Keys.split(",").filter { it.isNotBlank() }
    val line3Values = line3Keys.mapNotNull { key ->
        val value = attributes[key]?.takeIf { it.isNotBlank() }
        when {
            value == null -> null
            key.contains("preco") || key.contains("price") || key.contains("valor") -> {
                value.toDoubleOrNull()?.let {
                    java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR")).format(it)
                } ?: value
            }
            else -> value
        }
    }
    val line3Text = line3Values.joinToString(" • ")

    val quantityValue = attributes[layout.qtyKey]?.toDoubleOrNull()?.toInt()?.toString() ?: "0"

    ListItem(
        headlineContent = {
            Text(
                text = line1Value,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (line2Text.isNotBlank()) {
                    Text(
                        text = line2Text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (line3Text.isNotBlank()) {
                    Text(
                        text = line3Text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        trailingContent = {
            Text(
                text = quantityValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = modifier
    )
}

@Preview(showBackground = true, showSystemUi = true, name = "Produtos com Layout")
@Composable
private fun ProductListScreenPreview() {
    val sampleProducts = listOf(
        ProductEntity(code = "P1", name = "Produto Um", description = "Desc", category = null, unit = "UN"),
        ProductEntity(code = "P2", name = "Produto Dois", description = null, category = null, unit = "CX")
    )
    val layout = ProductListLayoutEntity(
        line1Key = "nome",
        line2Keys = "ean,sku",
        line3Keys = "preco,categoria",
        qtyKey = "quantidade"
    )
    val attrs = mapOf(
        "P1" to mapOf("nome" to "Produto Um", "ean" to "1234567890123", "sku" to "P001", "preco" to "10.50", "categoria" to "Cat A", "quantidade" to "5"),
        "P2" to mapOf("nome" to "Produto Dois", "ean" to "9876543210987", "sku" to "P002", "preco" to "25.90", "categoria" to "Cat B", "quantidade" to "2")
    )

    InventarioTheme {
        ProductListScreen(
            state = ProductListState(
                products = sampleProducts,
                filteredProducts = sampleProducts,
                layout = layout,
                productAttributes = attrs,
                searchQuery = ""
            ),
            onEvent = {},
            onBack = {},
            onOpenLayoutSettings = {}
        )
    }
}
