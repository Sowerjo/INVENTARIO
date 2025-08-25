package com.mobitech.inventario.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitech.inventario.domain.common.Result
import com.mobitech.inventario.domain.model.InventoryEntity
import com.mobitech.inventario.domain.model.InventoryItemEntity
import com.mobitech.inventario.domain.model.InventoryStatus
import com.mobitech.inventario.domain.repository.InventoryItemRepository
import com.mobitech.inventario.domain.repository.InventoryRepository
import com.mobitech.inventario.domain.usecase.AddInventoryItemUseCase
import com.mobitech.inventario.domain.usecase.FinishInventoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


data class InventoryDetailState(
    val inventory: InventoryEntity? = null,
    val items: List<InventoryItemEntity> = emptyList(),
    val productCode: String = "",
    val qty: String = "1",
    val adding: Boolean = false,
    val finishing: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

sealed interface InventoryDetailEvent {
    data class OnCode(val v: String): InventoryDetailEvent
    data class OnQty(val v: String): InventoryDetailEvent
    object AddItem: InventoryDetailEvent
    object Finish: InventoryDetailEvent
    object ClearError: InventoryDetailEvent
    object ClearMessage: InventoryDetailEvent
}

@HiltViewModel
class InventoryDetailViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val itemRepository: InventoryItemRepository,
    private val addItemUC: AddInventoryItemUseCase,
    private val finishUC: FinishInventoryUseCase
): ViewModel() {
    var state by mutableStateOf(InventoryDetailState())
        private set

    private var currentId: Long? = null

    fun load(id: Long) {
        if (currentId == id && state.inventory != null) return
        currentId = id
        viewModelScope.launch {
            state = state.copy(error = null, message = null)
            val inv = inventoryRepository.getById(id)
            state = state.copy(inventory = inv)
            observeItems(id)
        }
    }

    private fun observeItems(id: Long) = viewModelScope.launch {
        itemRepository.observeItems(id).collectLatest { list ->
            state = state.copy(items = list)
        }
    }

    fun onEvent(e: InventoryDetailEvent) {
        when(e) {
            is InventoryDetailEvent.OnCode -> state = state.copy(productCode = e.v)
            is InventoryDetailEvent.OnQty -> state = state.copy(qty = e.v)
            InventoryDetailEvent.AddItem -> addItem()
            InventoryDetailEvent.Finish -> finish()
            InventoryDetailEvent.ClearError -> state = state.copy(error = null)
            InventoryDetailEvent.ClearMessage -> state = state.copy(message = null)
        }
    }

    private fun addItem() {
        val inv = state.inventory ?: return
        if (inv.status != InventoryStatus.ONGOING) { state = state.copy(error = "Inventário finalizado"); return }
        val code = state.productCode.trim()
        val qtyD = state.qty.toDoubleOrNull()
        if (code.isBlank() || qtyD == null || qtyD <= 0) { state = state.copy(error = "Dados inválidos"); return }
        viewModelScope.launch {
            state = state.copy(adding = true, error = null)
            when(val res = addItemUC(inv.id, code, qtyD)) {
                is Result.Success -> state = state.copy(adding=false, productCode="", qty="1", message = "Item adicionado")
                is Result.Error -> state = state.copy(adding=false, error = res.message)
            }
        }
    }

    private fun finish() {
        val inv = state.inventory ?: return
        if (inv.status != InventoryStatus.ONGOING) { state = state.copy(error = "Já finalizado"); return }
        viewModelScope.launch {
            state = state.copy(finishing = true, error = null)
            when(val res = finishUC(inv.id)) {
                is Result.Success -> {
                    // recarregar inventário
                    val updated = inventoryRepository.getById(inv.id)
                    state = state.copy(finishing=false, inventory = updated, message = "Inventário finalizado")
                }
                is Result.Error -> state = state.copy(finishing=false, error = res.message)
            }
        }
    }
}

