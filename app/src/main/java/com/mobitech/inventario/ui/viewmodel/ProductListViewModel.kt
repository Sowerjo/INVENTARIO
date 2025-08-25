package com.mobitech.inventario.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitech.inventario.domain.common.Result
import com.mobitech.inventario.domain.model.ProductEntity
import com.mobitech.inventario.domain.model.ProductListLayoutEntity
import com.mobitech.inventario.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

data class ProductListState(
    val products: List<ProductEntity> = emptyList(),
    val filteredProducts: List<ProductEntity> = emptyList(),
    val searchQuery: String = "",
    val layout: ProductListLayoutEntity = ProductListLayoutEntity(),
    val productAttributes: Map<String, Map<String, String?>> = emptyMap(),
    val xlsxFileName: String? = null,
    val xlsxSize: Int = 0,
    val importing: Boolean = false,
    val importProgress: Int = 0,
    val importTotal: Int = 0,
    val exporting: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val activeCategoryCount: Int = 0,
    val totalCategoryCount: Int = 0,
    val isScanning: Boolean = false
)

sealed interface ProductListEvent {
    data class OnXlsxLoaded(val name: String, val bytes: ByteArray): ProductListEvent
    object ImportAppend: ProductListEvent
    object ImportOverwrite: ProductListEvent
    object Export: ProductListEvent
    data class ExportToUri(val uri: android.net.Uri): ProductListEvent
    object ClearMessage: ProductListEvent
    object ClearError: ProductListEvent
    object ClearFile: ProductListEvent
    data class SetError(val msg: String): ProductListEvent
    data class UpdateSearchQuery(val query: String): ProductListEvent
    object StartScanning: ProductListEvent
    object StopScanning: ProductListEvent
    data class OnBarcodeScanned(val barcode: String): ProductListEvent
}

@HiltViewModel
class ProductListViewModel @Inject constructor(
    observeUC: ObserveProductsUseCase,
    private val importXlsxUC: ImportProductsXlsxUseCase,
    private val exportXlsxUC: ExportProductsXlsxUseCase,
    private val exportXlsxToUriUC: ExportProductsXlsxToUriUseCase,
    private val prepareXmlUC: PrepareProductsXmlParserUseCase,
    observeCategoriesUC: ObserveCategoriesUseCase,
    private val getAttrsUC: GetProductAttributesUseCase,
    private val getLayoutUC: GetProductListLayoutUseCase
): ViewModel() {
    var state by mutableStateOf(ProductListState())
        private set

    private var fullProducts: List<ProductEntity> = emptyList()
    private var lastXlsxBytes: ByteArray? = null
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    init {
        viewModelScope.launch { prepareXmlUC() }

        // Observar layout
        viewModelScope.launch {
            val layout = getLayoutUC()
            state = state.copy(layout = layout)
            refreshVisibleAttributes()
        }

        // Observar categorias
        viewModelScope.launch {
            observeCategoriesUC().collectLatest { cats ->
                val enabled = cats.filter { it.enabled }.map { it.name }.toSet()
                state = state.copy(
                    activeCategoryCount = enabled.size,
                    totalCategoryCount = cats.size
                )
                refreshVisibleAttributes()
            }
        }

        // Observar produtos
        viewModelScope.launch {
            observeUC().collectLatest { list ->
                fullProducts = list
                state = state.copy(products = fullProducts)
                filterProducts()
                refreshVisibleAttributes()
            }
        }
    }

    fun onEvent(e: ProductListEvent) {
        when(e) {
            is ProductListEvent.OnXlsxLoaded -> {
                lastXlsxBytes = e.bytes
                state = state.copy(xlsxFileName = e.name, xlsxSize = e.bytes.size)
            }
            ProductListEvent.ImportAppend -> import(false)
            ProductListEvent.ImportOverwrite -> import(true)
            ProductListEvent.Export -> export()
            is ProductListEvent.ExportToUri -> exportToUri(e.uri)
            ProductListEvent.ClearMessage -> state = state.copy(message = null)
            ProductListEvent.ClearError -> state = state.copy(error = null)
            ProductListEvent.ClearFile -> {
                lastXlsxBytes = null
                state = state.copy(xlsxFileName = null, xlsxSize = 0)
            }
            is ProductListEvent.SetError -> state = state.copy(error = e.msg)
            is ProductListEvent.UpdateSearchQuery -> {
                state = state.copy(searchQuery = e.query)
                filterProducts()
            }
            ProductListEvent.StartScanning -> state = state.copy(isScanning = true)
            ProductListEvent.StopScanning -> state = state.copy(isScanning = false)
            is ProductListEvent.OnBarcodeScanned -> {
                state = state.copy(searchQuery = e.barcode, isScanning = false)
                filterProducts()
            }
        }
    }

    private fun filterProducts() {
        val query = state.searchQuery.lowercase().trim()
        if (query.isBlank()) {
            state = state.copy(filteredProducts = fullProducts)
            return
        }

        val layout = state.layout
        val line1Key = layout.line1Key
        val line2Keys = layout.line2Keys.split(",").filter { it.isNotBlank() }
        val line3Keys = layout.line3Keys.split(",").filter { it.isNotBlank() }
        val searchKeys = (listOf(line1Key) + line2Keys + line3Keys).distinct()

        val filtered = fullProducts.filter { product ->
            val attributes = state.productAttributes[product.code] ?: emptyMap()

            // Buscar nos campos básicos
            val basicMatch = product.name.lowercase().contains(query) ||
                    product.code.lowercase().contains(query) ||
                    product.description?.lowercase()?.contains(query) == true ||
                    product.category?.lowercase()?.contains(query) == true

            // Buscar nos atributos das chaves do layout
            val attributeMatch = searchKeys.any { key ->
                attributes[key]?.lowercase()?.contains(query) == true
            }

            basicMatch || attributeMatch
        }

        state = state.copy(filteredProducts = filtered)
    }

    private fun import(overwrite: Boolean) {
        val bytes = lastXlsxBytes
        if (bytes == null || bytes.isEmpty()) {
            state = state.copy(error = "XLSX não carregado")
            return
        }
        viewModelScope.launch {
            state = state.copy(importing = true, importProgress = 0, importTotal = 0, error = null, message = null)
            val res = importXlsxUC(bytes, overwrite) { cur, total ->
                state = state.copy(importProgress = cur.coerceAtMost(total), importTotal = total)
            }
            when(res) {
                is Result.Success -> {
                    state = state.copy(importing=false, message = "Importados ${res.data} produtos")
                    // Recarregar layout após importação
                    val newLayout = getLayoutUC()
                    state = state.copy(layout = newLayout)
                }
                is Result.Error -> state = state.copy(importing=false, error = res.message)
            }
        }
    }

    private fun export() {
        viewModelScope.launch {
            state = state.copy(exporting = true, error = null, message = null)
            when(val res = exportXlsxUC()) {
                is Result.Success -> state = state.copy(exporting=false, message = "Exportado em ${res.data}")
                is Result.Error -> state = state.copy(exporting=false, error = res.message)
            }
        }
    }

    private fun exportToUri(uri: android.net.Uri) {
        viewModelScope.launch {
            state = state.copy(exporting = true, error = null, message = null)
            when(val res = exportXlsxToUriUC(uri)) {
                is Result.Success -> state = state.copy(exporting = false, message = "Arquivo exportado com sucesso!")
                is Result.Error -> state = state.copy(exporting = false, error = res.message)
            }
        }
    }

    private var refreshJob: kotlinx.coroutines.Job? = null
    private fun refreshVisibleAttributes() {
        refreshJob?.cancel()
        val layout = state.layout
        val allKeys = listOf(layout.line1Key, layout.qtyKey) +
                     layout.line2Keys.split(",").filter { it.isNotBlank() } +
                     layout.line3Keys.split(",").filter { it.isNotBlank() }
        val keys = allKeys.distinct().filter { it.isNotBlank() }

        if (keys.isEmpty()) {
            state = state.copy(productAttributes = emptyMap())
            return
        }

        val codes = state.products.map { it.code }
        refreshJob = viewModelScope.launch {
            when(val res = getAttrsUC(codes, keys)) {
                is Result.Success -> {
                    state = state.copy(productAttributes = res.data)
                    filterProducts() // Re-filter com novos atributos
                }
                is Result.Error -> state = state.copy(error = res.message)
            }
        }
    }

    fun formatValue(key: String, value: String?): String? {
        if (value.isNullOrBlank()) return null

        return when {
            key.contains("preco") || key.contains("price") || key.contains("valor") -> {
                value.toDoubleOrNull()?.let { currencyFormatter.format(it) } ?: value
            }
            else -> value
        }
    }
}
