@file:OptIn(ExperimentalMaterial3Api::class)

package com.mobitech.inventario.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitech.inventario.domain.model.*
import com.mobitech.inventario.ui.viewmodel.*

@Composable
fun InventoryConfigScreen(
    onBack: () -> Unit,
    viewModel: InventoryConfigViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações de Inventário") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.onEvent(InventoryConfigEvent.SaveTemplate) },
                            enabled = !state.isSaving
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = "Salvar Configurações")
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header com nome do template e configurações gerais
            ConfigHeaderSection(
                state = state,
                onEvent = viewModel::onEvent
            )

            // Tabs das 4 abas
            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.onEvent(InventoryConfigEvent.OnTabSelected(0)) },
                    text = { Text("Mapeamento") }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.onEvent(InventoryConfigEvent.OnTabSelected(1)) },
                    text = { Text("Busca") }
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.onEvent(InventoryConfigEvent.OnTabSelected(2)) },
                    text = { Text("Layout") }
                )
                Tab(
                    selected = state.selectedTab == 3,
                    onClick = { viewModel.onEvent(InventoryConfigEvent.OnTabSelected(3)) },
                    text = { Text("Regras") }
                )
            }

            // Conteúdo das abas
            when (state.selectedTab) {
                0 -> FieldMappingTab(state = state, onEvent = viewModel::onEvent)
                1 -> SearchConfigTab(state = state, onEvent = viewModel::onEvent)
                2 -> CardLayoutTab(state = state, onEvent = viewModel::onEvent)
                3 -> CreationRulesTab(state = state, onEvent = viewModel::onEvent)
            }
        }
    }

    // Feedback visual para mensagens de sucesso e erro
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.onEvent(InventoryConfigEvent.ClearMessages)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.onEvent(InventoryConfigEvent.ClearMessages)
        }
    }

    // Mostrar loading ou erro
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ConfigHeaderSection(
    state: InventoryConfigState,
    onEvent: (InventoryConfigEvent) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.configName,
                onValueChange = { onEvent(InventoryConfigEvent.OnConfigNameChanged(it)) },
                label = { Text("Nome do Template") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.isDefault,
                    onCheckedChange = { onEvent(InventoryConfigEvent.OnIsDefaultChanged(it)) }
                )
                Text("Marcar como padrão")
            }

            if (state.savedTemplates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Templates Salvos:", fontWeight = FontWeight.Bold)
                state.savedTemplates.forEach { template ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { onEvent(InventoryConfigEvent.LoadTemplate(template.id)) }
                        ) {
                            Text(template.name + if (template.isDefault) " (Padrão)" else "")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { onEvent(InventoryConfigEvent.DeleteTemplate(template.id)) }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                        }
                    }
                }
            }
        }
    }
}

// Aba A: Mapeamento de Campos
@Composable
private fun FieldMappingTab(
    state: InventoryConfigState,
    onEvent: (InventoryConfigEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Mapeamento de Campos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Configure quais campos da planilha XLSX correspondem aos dados do inventário",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            FieldDropdown(
                label = "Campo ID (OBRIGATÓRIO) *",
                value = state.idKey,
                options = state.availableFields,
                onValueChange = { onEvent(InventoryConfigEvent.OnIdKeyChanged(it)) },
                isRequired = true
            )
        }

        item {
            FieldDropdown(
                label = "Campo Quantidade Esperada",
                value = state.expectedQtyField,
                options = state.availableFields,
                onValueChange = { onEvent(InventoryConfigEvent.OnExpectedQtyFieldChanged(it)) }
            )
        }

        item {
            FieldDropdown(
                label = "Campo a Atualizar",
                value = state.updateQtyField,
                options = state.availableFields,
                onValueChange = { onEvent(InventoryConfigEvent.OnUpdateQtyFieldChanged(it)) }
            )
        }

        item {
            Text("Modo de Atualização:", fontWeight = FontWeight.Medium)
            Column(modifier = Modifier.selectableGroup()) {
                UpdateMode.values().forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = state.updateMode == mode,
                            onClick = { onEvent(InventoryConfigEvent.OnUpdateModeChanged(mode)) }
                        )
                        Column {
                            Text(mode.name)
                            Text(
                                text = when (mode) {
                                    UpdateMode.OVERWRITE -> "Substitui pelo valor contado"
                                    UpdateMode.DELTA -> "Soma (Contado - Esperado) ao campo"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pré-visualização", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { onEvent(InventoryConfigEvent.LoadPreviewData) }
                        ) {
                            Text("Atualizar")
                        }
                    }

                    if (state.previewData.isNotEmpty()) {
                        state.previewData.take(5).forEach { row ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    "ID: ${row[state.idKey] ?: "N/A"}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Esperado: ${row[state.expectedQtyField] ?: "N/A"}")
                                if (row.size > 2) {
                                    Text(
                                        "Outros: " + row.entries.take(3).joinToString(" • ") { "${it.key}: ${it.value}" },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    } else {
                        Text(
                            "Nenhum dado para pré-visualização",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// Aba B: Busca & Leitura
@Composable
private fun SearchConfigTab(
    state: InventoryConfigState,
    onEvent: (InventoryConfigEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Busca & Leitura",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Configure como o scanner e a pesquisa funcionarão durante a contagem",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            FieldDropdown(
                label = "Campo do Scanner",
                value = state.scanKey,
                options = state.availableFields,
                onValueChange = { onEvent(InventoryConfigEvent.OnScanKeyChanged(it)) }
            )
        }

        item {
            Text("Campos Pesquisáveis:", fontWeight = FontWeight.Medium)
            state.availableFields.forEach { field ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = state.filterKeys.contains(field),
                        onCheckedChange = { onEvent(InventoryConfigEvent.OnFilterKeyToggled(field)) }
                    )
                    Text(field)
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Opções de Busca:", fontWeight = FontWeight.Medium)

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.normalizeSearch,
                            onCheckedChange = { onEvent(InventoryConfigEvent.OnNormalizeSearchChanged(it)) }
                        )
                        Column {
                            Text("Normalizar busca")
                            Text(
                                "Ignora maiúsculas/minúsculas, espaços e hífens",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.incrementOnScan,
                            onCheckedChange = { onEvent(InventoryConfigEvent.OnIncrementOnScanChanged(it)) }
                        )
                        Column {
                            Text("Incremento automático")
                            Text(
                                "Soma +1 automaticamente ao escanear, senão abre teclado numérico",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Aba C: Layout do Card
@Composable
private fun CardLayoutTab(
    state: InventoryConfigState,
    onEvent: (InventoryConfigEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Layout do Card",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Configure como os produtos serão exibidos durante a contagem",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            FieldDropdown(
                label = "Linha 1 (Título)",
                value = state.line1Key,
                options = state.availableFields,
                onValueChange = { onEvent(InventoryConfigEvent.OnLine1KeyChanged(it)) }
            )
        }

        item {
            Text("Linha 2 (máx. 2 campos):", fontWeight = FontWeight.Medium)
            state.availableFields.forEach { field ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = state.line2Keys.contains(field),
                        onCheckedChange = { onEvent(InventoryConfigEvent.OnLine2KeyToggled(field)) },
                        enabled = state.line2Keys.contains(field) || state.line2Keys.size < 2
                    )
                    Text(field)
                }
            }
        }

        item {
            Text("Linha 3 (máx. 2 campos):", fontWeight = FontWeight.Medium)
            state.availableFields.forEach { field ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = state.line3Keys.contains(field),
                        onCheckedChange = { onEvent(InventoryConfigEvent.OnLine3KeyToggled(field)) },
                        enabled = state.line3Keys.contains(field) || state.line3Keys.size < 2
                    )
                    Text(field)
                }
            }
        }

        item {
            FieldDropdown(
                label = "Campo Quantidade",
                value = state.qtyKey,
                options = state.availableFields,
                onValueChange = { onEvent(InventoryConfigEvent.OnQtyKeyChanged(it)) }
            )
        }

        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Opções de Exibição:", fontWeight = FontWeight.Medium)

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.showExpected,
                            onCheckedChange = { onEvent(InventoryConfigEvent.OnShowExpectedChanged(it)) }
                        )
                        Text("Mostrar quantidade esperada")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Botões Rápidos:", fontWeight = FontWeight.Medium)
                    listOf(-10, -5, -1, 1, 5, 10).forEach { step ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.quickSteps.contains(step),
                                onCheckedChange = { onEvent(InventoryConfigEvent.OnQuickStepToggled(step)) }
                            )
                            Text("${if (step > 0) "+" else ""}$step")
                        }
                    }
                }
            }
        }
    }
}

// Aba D: Regras de Criação
@Composable
private fun CreationRulesTab(
    state: InventoryConfigState,
    onEvent: (InventoryConfigEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Regras de Criação",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Configure o escopo e comportamento na criação do inventário",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            CategoryDropdown(
                label = "Filtrar por Categoria (opcional)",
                value = state.scopeCategory,
                options = state.availableCategories,
                onValueChange = { onEvent(InventoryConfigEvent.OnScopeCategoryChanged(it)) }
            )
        }

        item {
            Text("Modo de Entrada:", fontWeight = FontWeight.Medium)
            Column(modifier = Modifier.selectableGroup()) {
                InputMode.values().forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = state.inputMode == mode,
                            onClick = { onEvent(InventoryConfigEvent.OnInputModeChanged(mode)) }
                        )
                        Column {
                            Text(mode.name)
                            Text(
                                text = when (mode) {
                                    InputMode.ACCUMULATE -> "Soma ao valor existente"
                                    InputMode.OVERWRITE -> "Substitui o valor existente"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    isRequired: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            isError = isRequired && value.isBlank(),
            supportingText = if (isRequired && value.isBlank()) {
                { Text("Campo obrigatório") }
            } else null
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (!isRequired) {
                DropdownMenuItem(
                    text = { Text("Nenhum") },
                    onClick = {
                        onValueChange("")
                        expanded = false
                    }
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryDropdown(
    label: String,
    value: String?,
    options: List<String>,
    onValueChange: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value ?: "Todas as categorias",
            onValueChange = { },
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Todas as categorias") },
                onClick = {
                    onValueChange(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
