package com.mobitech.inventario.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitech.inventario.domain.model.*
import com.mobitech.inventario.domain.usecase.*
import com.mobitech.inventario.domain.common.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class InventoryConfigState(
    val selectedTab: Int = 0,
    val configName: String = "",
    val isDefault: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isSaving: Boolean = false,

    // Aba A: Mapeamento de Campos
    val idKey: String = "",
    val expectedQtyField: String = "",
    val updateQtyField: String = "",
    val updateMode: UpdateMode = UpdateMode.OVERWRITE,
    val availableFields: List<String> = emptyList(),
    val previewData: List<Map<String, String>> = emptyList(),

    // Aba B: Busca & Leitura
    val scanKey: String = "",
    val filterKeys: List<String> = emptyList(),
    val normalizeSearch: Boolean = true,
    val incrementOnScan: Boolean = true,

    // Aba C: Layout do Card
    val line1Key: String = "",
    val line2Keys: List<String> = emptyList(),
    val line3Keys: List<String> = emptyList(),
    val qtyKey: String = "",
    val showExpected: Boolean = true,
    val quickSteps: List<Int> = listOf(-1, 1, 5, 10),

    // Aba D: Regras de Criação
    val scopeCategory: String? = null,
    val inputMode: InputMode = InputMode.ACCUMULATE,
    val availableCategories: List<String> = emptyList(),

    // Templates salvos
    val savedTemplates: List<InventoryConfigEntity> = emptyList()
)

sealed class InventoryConfigEvent {
    data class OnTabSelected(val tab: Int) : InventoryConfigEvent()
    data class OnConfigNameChanged(val name: String) : InventoryConfigEvent()
    data class OnIsDefaultChanged(val isDefault: Boolean) : InventoryConfigEvent()

    // Aba A
    data class OnIdKeyChanged(val key: String) : InventoryConfigEvent()
    data class OnExpectedQtyFieldChanged(val field: String) : InventoryConfigEvent()
    data class OnUpdateQtyFieldChanged(val field: String) : InventoryConfigEvent()
    data class OnUpdateModeChanged(val mode: UpdateMode) : InventoryConfigEvent()
    object LoadPreviewData : InventoryConfigEvent()

    // Aba B
    data class OnScanKeyChanged(val key: String) : InventoryConfigEvent()
    data class OnFilterKeyToggled(val key: String) : InventoryConfigEvent()
    data class OnNormalizeSearchChanged(val normalize: Boolean) : InventoryConfigEvent()
    data class OnIncrementOnScanChanged(val increment: Boolean) : InventoryConfigEvent()

    // Aba C
    data class OnLine1KeyChanged(val key: String) : InventoryConfigEvent()
    data class OnLine2KeyToggled(val key: String) : InventoryConfigEvent()
    data class OnLine3KeyToggled(val key: String) : InventoryConfigEvent()
    data class OnQtyKeyChanged(val key: String) : InventoryConfigEvent()
    data class OnShowExpectedChanged(val show: Boolean) : InventoryConfigEvent()
    data class OnQuickStepToggled(val step: Int) : InventoryConfigEvent()

    // Aba D
    data class OnScopeCategoryChanged(val category: String?) : InventoryConfigEvent()
    data class OnInputModeChanged(val mode: InputMode) : InventoryConfigEvent()

    // Gerenciamento
    object SaveTemplate : InventoryConfigEvent()
    object LoadTemplates : InventoryConfigEvent()
    data class LoadTemplate(val templateId: Long) : InventoryConfigEvent()
    data class DeleteTemplate(val templateId: Long) : InventoryConfigEvent()
    object ClearMessages : InventoryConfigEvent()
}

@HiltViewModel
class InventoryConfigViewModel @Inject constructor(
    private val observeCategoriesUC: ObserveCategoriesUseCase,
    private val getAvailableKeysUC: GetAvailableLayoutKeysUseCase,
    private val getProductAttributesUC: GetProductAttributesUseCase,
    private val saveInventoryConfigUC: SaveInventoryConfigUseCase,
    private val loadInventoryConfigUC: LoadInventoryConfigUseCase,
    private val deleteInventoryConfigUC: DeleteInventoryConfigUseCase,
    private val observeInventoryConfigsUC: ObserveInventoryConfigsUseCase
) : ViewModel() {

    var state by mutableStateOf(InventoryConfigState())
        private set

    init {
        loadInitialData()
    }

    fun onEvent(event: InventoryConfigEvent) {
        when (event) {
            is InventoryConfigEvent.OnTabSelected -> {
                state = state.copy(selectedTab = event.tab)
            }

            is InventoryConfigEvent.OnConfigNameChanged -> {
                state = state.copy(configName = event.name)
            }

            is InventoryConfigEvent.OnIsDefaultChanged -> {
                state = state.copy(isDefault = event.isDefault)
            }

            // Aba A - Mapeamento de Campos
            is InventoryConfigEvent.OnIdKeyChanged -> {
                state = state.copy(idKey = event.key)
            }

            is InventoryConfigEvent.OnExpectedQtyFieldChanged -> {
                state = state.copy(expectedQtyField = event.field)
            }

            is InventoryConfigEvent.OnUpdateQtyFieldChanged -> {
                state = state.copy(updateQtyField = event.field)
            }

            is InventoryConfigEvent.OnUpdateModeChanged -> {
                state = state.copy(updateMode = event.mode)
            }

            is InventoryConfigEvent.LoadPreviewData -> {
                loadPreviewData()
            }

            // Aba B - Busca & Leitura
            is InventoryConfigEvent.OnScanKeyChanged -> {
                state = state.copy(scanKey = event.key)
            }

            is InventoryConfigEvent.OnFilterKeyToggled -> {
                val currentKeys = state.filterKeys.toMutableList()
                if (currentKeys.contains(event.key)) {
                    currentKeys.remove(event.key)
                } else {
                    currentKeys.add(event.key)
                }
                state = state.copy(filterKeys = currentKeys)
            }

            is InventoryConfigEvent.OnNormalizeSearchChanged -> {
                state = state.copy(normalizeSearch = event.normalize)
            }

            is InventoryConfigEvent.OnIncrementOnScanChanged -> {
                state = state.copy(incrementOnScan = event.increment)
            }

            // Aba C - Layout do Card
            is InventoryConfigEvent.OnLine1KeyChanged -> {
                state = state.copy(line1Key = event.key)
            }

            is InventoryConfigEvent.OnLine2KeyToggled -> {
                val currentKeys = state.line2Keys.toMutableList()
                if (currentKeys.contains(event.key)) {
                    currentKeys.remove(event.key)
                } else if (currentKeys.size < 2) {
                    currentKeys.add(event.key)
                }
                state = state.copy(line2Keys = currentKeys)
            }

            is InventoryConfigEvent.OnLine3KeyToggled -> {
                val currentKeys = state.line3Keys.toMutableList()
                if (currentKeys.contains(event.key)) {
                    currentKeys.remove(event.key)
                } else if (currentKeys.size < 2) {
                    currentKeys.add(event.key)
                }
                state = state.copy(line3Keys = currentKeys)
            }

            is InventoryConfigEvent.OnQtyKeyChanged -> {
                state = state.copy(qtyKey = event.key)
            }

            is InventoryConfigEvent.OnShowExpectedChanged -> {
                state = state.copy(showExpected = event.show)
            }

            is InventoryConfigEvent.OnQuickStepToggled -> {
                val currentSteps = state.quickSteps.toMutableList()
                if (currentSteps.contains(event.step)) {
                    currentSteps.remove(event.step)
                } else {
                    currentSteps.add(event.step)
                }
                state = state.copy(quickSteps = currentSteps.sorted())
            }

            // Aba D - Regras de Criação
            is InventoryConfigEvent.OnScopeCategoryChanged -> {
                state = state.copy(scopeCategory = event.category)
            }

            is InventoryConfigEvent.OnInputModeChanged -> {
                state = state.copy(inputMode = event.mode)
            }

            // Gerenciamento de templates
            is InventoryConfigEvent.SaveTemplate -> {
                saveTemplate()
            }

            is InventoryConfigEvent.LoadTemplates -> {
                loadTemplates()
            }

            is InventoryConfigEvent.LoadTemplate -> {
                loadTemplate(event.templateId)
            }

            is InventoryConfigEvent.DeleteTemplate -> {
                deleteTemplate(event.templateId)
            }

            is InventoryConfigEvent.ClearMessages -> {
                state = state.copy(error = null, successMessage = null)
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                // Carregar campos disponíveis dos produtos importados (headers do XLSX)
                loadAvailableFields()
                // Carregar categorias reais do banco de dados
                loadAvailableCategories()
                // Carregar templates salvos
                loadTemplates()
                // Definir valores padrão sensatos baseados nos dados reais
                setDefaultValues()
            } catch (e: Exception) {
                state = state.copy(error = e.message)
            } finally {
                state = state.copy(isLoading = false)
            }
        }
    }

    private suspend fun loadAvailableFields() {
        when (val result = getAvailableKeysUC()) {
            is Result.Success -> {
                state = state.copy(availableFields = result.data)
            }
            is Result.Error -> {
                state = state.copy(error = result.message)
            }
        }
    }

    private suspend fun loadAvailableCategories() {
        try {
            val categories = observeCategoriesUC().first()
            val categoryNames = categories.map { it.name }.distinct().sorted()
            state = state.copy(availableCategories = categoryNames)
        } catch (e: Exception) {
            state = state.copy(error = e.message)
        }
    }

    private fun setDefaultValues() {
        val fields = state.availableFields
        if (fields.isNotEmpty()) {
            // Configurar valores padrão inteligentes baseados nos campos disponíveis
            val defaultIdKey = when {
                fields.contains("sku") -> "sku"
                fields.contains("codigo") -> "codigo"
                fields.contains("ean") -> "ean"
                fields.contains("id") -> "id"
                else -> fields.first()
            }

            val defaultQtyField = when {
                fields.contains("quantidade") -> "quantidade"
                fields.contains("estoque") -> "estoque"
                fields.contains("qty") -> "qty"
                fields.contains("saldo") -> "saldo"
                else -> fields.firstOrNull { it.lowercase().contains("quant") || it.lowercase().contains("est") }
            }

            val defaultScanKey = when {
                fields.contains("ean") -> "ean"
                fields.contains("codigo_barras") -> "codigo_barras"
                fields.contains("barcode") -> "barcode"
                fields.contains("sku") -> "sku"
                else -> defaultIdKey
            }

            val defaultLine1Key = when {
                fields.contains("nome") -> "nome"
                fields.contains("descricao") -> "descricao"
                fields.contains("produto") -> "produto"
                else -> defaultIdKey
            }

            val defaultLine2Keys = mutableListOf<String>()
            if (fields.contains("ean") && defaultLine1Key != "ean") defaultLine2Keys.add("ean")
            if (fields.contains("sku") && defaultLine1Key != "sku" && !defaultLine2Keys.contains("sku")) defaultLine2Keys.add("sku")
            if (defaultLine2Keys.isEmpty() && fields.contains("codigo")) defaultLine2Keys.add("codigo")

            val defaultLine3Keys = mutableListOf<String>()
            if (fields.contains("categoria")) defaultLine3Keys.add("categoria")
            if (fields.contains("marca") && defaultLine3Keys.size < 2) defaultLine3Keys.add("marca")
            if (fields.any { it.lowercase().contains("preco") || it.lowercase().contains("valor") } && defaultLine3Keys.size < 2) {
                fields.firstOrNull { it.lowercase().contains("preco") || it.lowercase().contains("valor") }?.let {
                    defaultLine3Keys.add(it)
                }
            }

            val defaultFilterKeys = mutableListOf<String>()
            if (fields.contains("nome")) defaultFilterKeys.add("nome")
            if (fields.contains("ean")) defaultFilterKeys.add("ean")
            if (fields.contains("sku")) defaultFilterKeys.add("sku")
            if (fields.contains("codigo")) defaultFilterKeys.add("codigo")
            if (fields.contains("descricao")) defaultFilterKeys.add("descricao")

            state = state.copy(
                idKey = defaultIdKey,
                expectedQtyField = defaultQtyField ?: "",
                updateQtyField = defaultQtyField ?: "",
                scanKey = defaultScanKey,
                filterKeys = defaultFilterKeys,
                line1Key = defaultLine1Key,
                line2Keys = defaultLine2Keys.take(2),
                line3Keys = defaultLine3Keys.take(2),
                qtyKey = defaultQtyField ?: ""
            )
        }
    }

    private fun loadPreviewData() {
        viewModelScope.launch {
            if (state.idKey.isBlank()) {
                state = state.copy(error = "Selecione um campo ID primeiro")
                return@launch
            }

            try {
                // Buscar dados reais dos produtos para preview
                // Pegamos os primeiros 5 produtos como amostra
                val keysToFetch = listOfNotNull(
                    state.idKey,
                    state.expectedQtyField.takeIf { it.isNotBlank() },
                    state.line1Key.takeIf { it.isNotBlank() }
                ).plus(state.line2Keys).plus(state.line3Keys).distinct()

                // TODO: Implementar busca de códigos de produtos
                // Por enquanto, vamos simular com dados dos campos disponíveis
                val mockPreview = (1..5).map { index ->
                    val row = mutableMapOf<String, String>()
                    state.availableFields.forEach { field ->
                        row[field] = when {
                            field.lowercase().contains("sku") -> "SKU00$index"
                            field.lowercase().contains("ean") -> "789123456789$index"
                            field.lowercase().contains("nome") -> "Produto Teste $index"
                            field.lowercase().contains("quant") || field.lowercase().contains("est") -> "${(index * 5) + 10}"
                            field.lowercase().contains("preco") || field.lowercase().contains("valor") -> "${(index * 2.5 + 10.0)}"
                            field.lowercase().contains("categoria") -> "Categoria ${index % 3 + 1}"
                            field.lowercase().contains("marca") -> "Marca ${index % 2 + 1}"
                            else -> "Valor $index"
                        }
                    }
                    row.toMap()
                }

                state = state.copy(previewData = mockPreview)
            } catch (e: Exception) {
                state = state.copy(error = e.message)
            }
        }
    }

    private fun saveTemplate() {
        viewModelScope.launch {
            if (state.configName.isBlank()) {
                state = state.copy(error = "Nome do template é obrigatório")
                return@launch
            }

            if (state.idKey.isBlank()) {
                state = state.copy(error = "Campo ID é obrigatório")
                return@launch
            }

            state = state.copy(isSaving = true, error = null, successMessage = null)
            try {
                // SALVAMENTO REAL das configurações no banco de dados
                val result = saveInventoryConfigUC(
                    name = state.configName,
                    isDefault = state.isDefault,
                    idKey = state.idKey,
                    expectedQtyField = state.expectedQtyField,
                    updateQtyField = state.updateQtyField,
                    updateMode = state.updateMode,
                    scanKey = state.scanKey,
                    filterKeys = state.filterKeys,
                    normalizeSearch = state.normalizeSearch,
                    incrementOnScan = state.incrementOnScan,
                    line1Key = state.line1Key,
                    line2Keys = state.line2Keys,
                    line3Keys = state.line3Keys,
                    qtyKey = state.qtyKey,
                    showExpected = state.showExpected,
                    quickSteps = state.quickSteps,
                    scopeCategory = state.scopeCategory,
                    inputMode = state.inputMode
                )

                when (result) {
                    is Result.Success -> {
                        state = state.copy(
                            isSaving = false,
                            successMessage = "✅ Configurações salvas com sucesso! (ID: ${result.data})"
                        )

                        // Recarregar lista de templates
                        loadTemplates()

                        // Auto-limpar mensagem de sucesso após 3 segundos
                        kotlinx.coroutines.delay(3000)
                        if (state.successMessage != null) {
                            state = state.copy(successMessage = null)
                        }
                    }
                    is Result.Error -> {
                        state = state.copy(
                            isSaving = false,
                            error = "❌ Erro ao salvar: ${result.message}"
                        )
                    }
                }

            } catch (e: Exception) {
                state = state.copy(
                    isSaving = false,
                    error = "❌ Erro inesperado: ${e.message}"
                )
            }
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            try {
                // CARREGAMENTO REAL de templates do banco de dados
                observeInventoryConfigsUC().collect { templates ->
                    state = state.copy(savedTemplates = templates)
                }
            } catch (e: Exception) {
                state = state.copy(error = e.message)
            }
        }
    }

    private fun loadTemplate(templateId: Long) {
        viewModelScope.launch {
            try {
                // CARREGAMENTO REAL de template específico
                when (val result = loadInventoryConfigUC(templateId)) {
                    is Result.Success -> {
                        val config = result.data
                        state = state.copy(
                            configName = config.config.name,
                            isDefault = config.config.isDefault,
                            idKey = config.fieldMapping?.idKey ?: "",
                            expectedQtyField = config.fieldMapping?.expectedQtyField ?: "",
                            updateQtyField = config.fieldMapping?.updateQtyField ?: "",
                            updateMode = config.fieldMapping?.updateMode ?: UpdateMode.OVERWRITE,
                            scanKey = config.searchConfig?.scanKey ?: "",
                            filterKeys = config.searchConfig?.filterKeys?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                            normalizeSearch = config.searchConfig?.normalizeSearch ?: true,
                            incrementOnScan = config.searchConfig?.incrementOnScan ?: true,
                            line1Key = config.cardLayout?.line1Key ?: "",
                            line2Keys = config.cardLayout?.line2Keys?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                            line3Keys = config.cardLayout?.line3Keys?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                            qtyKey = config.cardLayout?.qtyKey ?: "",
                            showExpected = config.cardLayout?.showExpected ?: true,
                            quickSteps = config.cardLayout?.quickSteps?.split(",")?.mapNotNull { it.toIntOrNull() } ?: listOf(-1, 1, 5, 10),
                            scopeCategory = config.creationRules?.scopeCategory,
                            inputMode = config.creationRules?.inputMode ?: InputMode.ACCUMULATE,
                            successMessage = "✅ Template '${config.config.name}' carregado com sucesso!"
                        )
                    }
                    is Result.Error -> {
                        state = state.copy(error = "❌ Erro ao carregar template: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                state = state.copy(error = "❌ Erro inesperado: ${e.message}")
            }
        }
    }

    private fun deleteTemplate(templateId: Long) {
        viewModelScope.launch {
            try {
                // EXCLUSÃO REAL de template do banco de dados
                when (val result = deleteInventoryConfigUC(templateId)) {
                    is Result.Success -> {
                        state = state.copy(successMessage = "✅ Template excluído com sucesso!")
                        loadTemplates() // Recarregar lista
                    }
                    is Result.Error -> {
                        state = state.copy(error = "❌ Erro ao excluir template: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                state = state.copy(error = "❌ Erro inesperado: ${e.message}")
            }
        }
    }
}
