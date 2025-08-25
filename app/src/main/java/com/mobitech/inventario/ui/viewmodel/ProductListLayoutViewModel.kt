package com.mobitech.inventario.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitech.inventario.domain.common.Result
import com.mobitech.inventario.domain.model.ProductListLayoutEntity
import com.mobitech.inventario.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductListLayoutState(
    val layout: ProductListLayoutEntity = ProductListLayoutEntity(),
    val availableKeys: List<String> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val previewItem: PreviewItem = PreviewItem()
)

data class PreviewItem(
    val line1: String = "Produto Exemplo",
    val line2: String = "1234567890123 • PRD001",
    val line3: String = "R$ 25,90 • Categoria A",
    val quantity: String = "10"
)

sealed interface ProductListLayoutEvent {
    object LoadData : ProductListLayoutEvent
    data class UpdateLine1Key(val key: String) : ProductListLayoutEvent
    data class UpdateLine2Keys(val keys: List<String>) : ProductListLayoutEvent
    data class UpdateLine3Keys(val keys: List<String>) : ProductListLayoutEvent
    // Novos eventos para seleção em cascata
    data class UpdateLine2Key1(val key: String) : ProductListLayoutEvent
    data class UpdateLine2Key2(val key: String) : ProductListLayoutEvent
    data class UpdateLine3Key1(val key: String) : ProductListLayoutEvent
    data class UpdateLine3Key2(val key: String) : ProductListLayoutEvent
    data class UpdateQtyKey(val key: String) : ProductListLayoutEvent
    object SaveLayout : ProductListLayoutEvent
    object SuggestDefaults : ProductListLayoutEvent
    object ClearError : ProductListLayoutEvent
    object ClearMessage : ProductListLayoutEvent
}

@HiltViewModel
class ProductListLayoutViewModel @Inject constructor(
    private val getLayoutUC: GetProductListLayoutUseCase,
    private val updateLayoutUC: UpdateProductListLayoutUseCase,
    private val getAvailableKeysUC: GetAvailableLayoutKeysUseCase,
    private val suggestDefaultsUC: SuggestLayoutDefaultsUseCase
) : ViewModel() {

    var state by mutableStateOf(ProductListLayoutState())
        private set

    init {
        loadData()
    }

    fun onEvent(event: ProductListLayoutEvent) {
        when (event) {
            ProductListLayoutEvent.LoadData -> loadData()
            is ProductListLayoutEvent.UpdateLine1Key -> updateLine1Key(event.key)
            is ProductListLayoutEvent.UpdateLine2Keys -> updateLine2Keys(event.keys)
            is ProductListLayoutEvent.UpdateLine3Keys -> updateLine3Keys(event.keys)
            is ProductListLayoutEvent.UpdateLine2Key1 -> updateLine2Key1(event.key)
            is ProductListLayoutEvent.UpdateLine2Key2 -> updateLine2Key2(event.key)
            is ProductListLayoutEvent.UpdateLine3Key1 -> updateLine3Key1(event.key)
            is ProductListLayoutEvent.UpdateLine3Key2 -> updateLine3Key2(event.key)
            is ProductListLayoutEvent.UpdateQtyKey -> updateQtyKey(event.key)
            ProductListLayoutEvent.SaveLayout -> saveLayout()
            ProductListLayoutEvent.SuggestDefaults -> suggestDefaults()
            ProductListLayoutEvent.ClearError -> state = state.copy(error = null)
            ProductListLayoutEvent.ClearMessage -> state = state.copy(message = null)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)

            try {
                val layout = getLayoutUC()
                val availableKeysResult = getAvailableKeysUC()

                when (availableKeysResult) {
                    is Result.Success -> {
                        state = state.copy(
                            layout = layout,
                            availableKeys = availableKeysResult.data,
                            loading = false
                        )
                        updatePreview()
                    }
                    is Result.Error -> {
                        state = state.copy(
                            loading = false,
                            error = availableKeysResult.message
                        )
                    }
                }
            } catch (e: Exception) {
                state = state.copy(
                    loading = false,
                    error = "Erro ao carregar dados: ${e.message}"
                )
            }
        }
    }

    private fun updateLine1Key(key: String) {
        state = state.copy(layout = state.layout.copy(line1Key = key))
        updatePreview()
    }

    private fun updateLine2Keys(keys: List<String>) {
        state = state.copy(layout = state.layout.copy(line2Keys = keys.joinToString(",")))
        updatePreview()
    }

    private fun updateLine3Keys(keys: List<String>) {
        state = state.copy(layout = state.layout.copy(line3Keys = keys.joinToString(",")))
        updatePreview()
    }

    private fun updateLine2Key1(key: String) {
        val currentKeys = state.layout.line2Keys.split(",").toMutableList()
        if (key in currentKeys) return // Evita duplicatas
        currentKeys.add(0, key) // Adiciona na frente
        state = state.copy(layout = state.layout.copy(line2Keys = currentKeys.joinToString(",")))
        updatePreview()
    }

    private fun updateLine2Key2(key: String) {
        val currentKeys = state.layout.line2Keys.split(",").toMutableList()
        if (key in currentKeys) return // Evita duplicatas
        currentKeys.add(1, key) // Adiciona na segunda posição
        state = state.copy(layout = state.layout.copy(line2Keys = currentKeys.joinToString(",")))
        updatePreview()
    }

    private fun updateLine3Key1(key: String) {
        val currentKeys = state.layout.line3Keys.split(",").toMutableList()
        if (key in currentKeys) return // Evita duplicatas
        currentKeys.add(0, key) // Adiciona na frente
        state = state.copy(layout = state.layout.copy(line3Keys = currentKeys.joinToString(",")))
        updatePreview()
    }

    private fun updateLine3Key2(key: String) {
        val currentKeys = state.layout.line3Keys.split(",").toMutableList()
        if (key in currentKeys) return // Evita duplicatas
        currentKeys.add(1, key) // Adiciona na segunda posição
        state = state.copy(layout = state.layout.copy(line3Keys = currentKeys.joinToString(",")))
        updatePreview()
    }

    private fun updateQtyKey(key: String) {
        state = state.copy(layout = state.layout.copy(qtyKey = key))
        updatePreview()
    }

    private fun saveLayout() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, message = null)

            when (val result = updateLayoutUC(state.layout)) {
                is Result.Success -> {
                    state = state.copy(
                        loading = false,
                        message = "Layout salvo com sucesso!"
                    )
                }
                is Result.Error -> {
                    state = state.copy(
                        loading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    private fun suggestDefaults() {
        viewModelScope.launch {
            val suggested = suggestDefaultsUC(state.availableKeys)
            state = state.copy(layout = suggested)
            updatePreview()
        }
    }

    private fun updatePreview() {
        val layout = state.layout
        val keys = state.availableKeys

        // Simula dados de exemplo para preview
        val sampleData = mapOf(
            "nome" to "Produto Exemplo",
            "sku" to "PRD001",
            "ean" to "1234567890123",
            "preco" to "25.90",
            "categoria" to "Categoria A",
            "quantidade" to "10",
            "descricao" to "Descrição do produto",
            "marca" to "Marca X"
        )

        // Linha 1
        val line1 = sampleData[layout.line1Key] ?: layout.line1Key

        // Linha 2
        val line2Keys = layout.line2Keys.split(",").filter { it.isNotBlank() }
        val line2 = line2Keys.mapNotNull { key ->
            sampleData[key] ?: if (keys.contains(key)) key else null
        }.joinToString(" • ")

        // Linha 3
        val line3Keys = layout.line3Keys.split(",").filter { it.isNotBlank() }
        val line3 = line3Keys.mapNotNull { key ->
            val value = sampleData[key] ?: if (keys.contains(key)) key else null
            when {
                value == null -> null
                key.contains("preco") || key.contains("price") || key.contains("valor") ->
                    value.toDoubleOrNull()?.let { "R$ %.2f".format(it).replace(".", ",") } ?: value
                else -> value
            }
        }.joinToString(" • ")

        // Quantidade
        val quantity = sampleData[layout.qtyKey] ?: "0"

        state = state.copy(
            previewItem = PreviewItem(
                line1 = line1,
                line2 = line2.ifBlank { "Linha 2 vazia" },
                line3 = line3.ifBlank { "Linha 3 vazia" },
                quantity = quantity
            )
        )
    }
}
